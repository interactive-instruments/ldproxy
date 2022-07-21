/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoMultiBind
public interface QueriesHandler<T extends QueryIdentifier> {

  Logger LOGGER = LoggerFactory.getLogger(QueriesHandler.class);

  Locale[] LANGUAGES =
      I18n.getLanguages().stream().collect(Collectors.toUnmodifiableList()).toArray(Locale[]::new);
  String[] ENCODINGS = {"gzip", "identity"};

  Map<T, QueryHandler<? extends QueryInput>> getQueryHandlers();

  default boolean canHandle(T queryIdentifier, QueryInput queryInput) {
    return true;
  }

  default Response handle(
      T queryIdentifier, QueryInput queryInput, ApiRequestContext requestContext) {

    QueryHandler<? extends QueryInput> queryHandler = getQueryHandlers().get(queryIdentifier);

    if (Objects.isNull(queryHandler)) {
      throw new IllegalStateException("No query handler found for " + queryIdentifier + ".");
    }

    if (!queryHandler.isValidInput(queryInput)) {
      throw new IllegalStateException(
          MessageFormat.format(
              "Invalid query handler {0} for query input of class {1}.",
              queryHandler.getClass().getSimpleName(), queryInput.getClass().getSimpleName()));
    }

    return queryHandler.handle(queryInput, requestContext);
  }

  default Response.ResponseBuilder evaluatePreconditions(
      ApiRequestContext requestContext, Date lastModified, EntityTag etag) {
    if (requestContext.getRequest().isPresent()) {
      Request request = requestContext.getRequest().get();
      try {
        if (Objects.nonNull(lastModified) && Objects.nonNull(etag)) {
          return request.evaluatePreconditions(lastModified, etag);
        } else if (Objects.nonNull(etag)) {
          return request.evaluatePreconditions(etag);
        } else if (Objects.nonNull(lastModified)) {
          return request.evaluatePreconditions(lastModified);
        } else {
          return request.evaluatePreconditions();
        }
      } catch (Exception e) {
        // could not parse headers, so silently ignore them and return the regular response
        LOGGER.debug("Ignoring invalid conditional request headers: {}", e.getMessage());
      }
    }

    return null;
  }

  default Response.ResponseBuilder prepareSuccessResponse(ApiRequestContext requestContext) {
    Response.ResponseBuilder response = Response.ok().type(requestContext.getMediaType().type());

    requestContext.getLanguage().ifPresent(response::language);

    return response;
  }

  default Response.ResponseBuilder prepareSuccessResponse(
      ApiRequestContext requestContext,
      List<Link> links,
      HeaderCaching cacheInfo,
      EpsgCrs crs,
      HeaderContentDisposition dispositionInfo) {
    Response.ResponseBuilder response = Response.ok().type(requestContext.getMediaType().type());

    cacheInfo.getLastModified().ifPresent(response::lastModified);
    cacheInfo.getEtag().ifPresent(response::tag);
    cacheInfo
        .cacheControl()
        .ifPresent(cacheControl -> response.cacheControl(CacheControl.valueOf(cacheControl)));
    cacheInfo.expires().ifPresent(response::expires);

    response.variants(
        Variant.mediaTypes(
                new ImmutableList.Builder<ApiMediaType>()
                        .add(requestContext.getMediaType())
                        .addAll(requestContext.getAlternateMediaTypes())
                        .build()
                        .stream()
                        .map(ApiMediaType::type)
                        .toArray(MediaType[]::new))
            .languages(LANGUAGES)
            .encodings(ENCODINGS)
            .add()
            .build());

    requestContext.getLanguage().ifPresent(response::language);

    if (Objects.nonNull(links)) {

      // skip URI templates in the header as these are not RFC 8288 links
      links.stream()
          .filter(link -> link.getTemplated() == null || !link.getTemplated())
          .sorted(Link.COMPARATOR_LINKS)
          .forEachOrdered(link -> response.links(link.getLink()));
    }

    if (Objects.nonNull(crs)) {
      response.header("Content-Crs", "<" + crs.toUriString() + ">");
    }

    if (Objects.nonNull(dispositionInfo)) {
      response.header(
          "Content-Disposition",
          (dispositionInfo.getAttachment() ? "attachment" : "inline")
              + dispositionInfo
                  .getFilename()
                  .map(filename -> "; filename=\"" + filename + "\"")
                  .orElse(""));
    }

    return response;
  }

  default Date getLastModified(QueryInput queryInput, PageRepresentation resource) {
    return queryInput.getLastModified().or(resource::getLastModified).orElse(null);
  }

  default Date getLastModified(QueryInput queryInput) {
    return queryInput.getLastModified().orElse(null);
  }

  /**
   * Analyse the error reported by a feature stream. If it looks like a server-side error, re-throw
   * the exception, otherwise continue
   *
   * @param error the exception reported by xtraplatform
   */
  static void processStreamError(Throwable error) {
    String errorMessage = error.getMessage();
    if (Objects.isNull(errorMessage)) {
      errorMessage =
          error.getClass().getSimpleName() + " at " + error.getStackTrace()[0].toString();
    }
    Throwable rootError = error;
    while (Objects.nonNull(rootError) && !Objects.equals(rootError, rootError.getCause())) {
      if (rootError instanceof org.eclipse.jetty.io.EofException) {
        // the connection has been lost, typically the client has cancelled the request, log on
        // debug level
        LOGGER.debug("Request cancelled due to lost connection.");
        return;
      } else if (rootError instanceof UnprocessableEntity) {
        // Cannot handle request
        throw new WebApplicationException(rootError.getMessage(), 422);
      } else if (rootError instanceof IllegalArgumentException) {
        // Bad request
        LogContext.errorAsDebug(LOGGER, rootError, "Invalid request");
        throw new BadRequestException(rootError.getMessage());
      } else if (rootError instanceof RuntimeException) {
        // Runtime exception is generated by XtraPlatform, look at the cause, if there is one
        if (Objects.nonNull(rootError.getCause())) {
          rootError = rootError.getCause();
        } else {
          // otherwise log the error
          break;
        }
      } else {
        // some other exception occurred, log as an error
        break;
      }
    }

    LogContext.error(LOGGER, rootError, "");

    throw new InternalServerErrorException(errorMessage, error);
  }
}
