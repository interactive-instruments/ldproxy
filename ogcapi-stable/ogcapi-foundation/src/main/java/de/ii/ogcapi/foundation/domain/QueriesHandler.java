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
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
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

  static void ensureCollectionIdExists(OgcApiDataV2 apiData, String collectionId) {
    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
    }
  }

  static void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
    if (!featureProvider.supportsQueries()) {
      throw new IllegalStateException("Feature provider does not support queries.");
    }
  }

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
      // skip URI templates in the Link header as these are not RFC 8288 links
      List<javax.ws.rs.core.Link> headerLinks =
          links.stream()
              .filter(link -> link.getTemplated() == null || !link.getTemplated())
              .sorted(Link.COMPARATOR_LINKS)
              .map(Link::getLink)
              .collect(Collectors.toUnmodifiableList());

      // Instead use a Link-Template header for templaes
      List<String> headerLinkTemplates = getLinkTemplates(links);

      // only add links and link templates, if the strings are not larger than the limit
      if (headerLinks.stream().map(l -> l.toString().length()).mapToInt(Integer::intValue).sum()
              + headerLinkTemplates.stream().map(String::length).mapToInt(Integer::intValue).sum()
          <= requestContext.getMaxResponseLinkHeaderSize()) {
        headerLinks.forEach(response::links);
        headerLinkTemplates.forEach(template -> response.header("Link-Template", template));
      }
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

  default List<String> getLinkTemplates(List<Link> links) {
    return links.stream()
        .filter(link -> link.getTemplated() != null && link.getTemplated())
        .sorted(Link.COMPARATOR_LINKS)
        .map(
            template -> {
              StringBuilder builder =
                  new StringBuilder(
                      String.format("<%s>; rel=\"%s\"", template.getHref(), template.getRel()));
              if (template.getTitle() != null) {
                builder.append(String.format("; title=\"%s\"", template.getTitle()));
              }
              if (template.getType() != null) {
                builder.append(String.format("; type=\"%s\"", template.getType()));
              }
              return builder.toString();
            })
        .collect(Collectors.toUnmodifiableList());
  }

  default Date getLastModified(QueryInput queryInput, PageRepresentation resource) {
    return queryInput.getLastModified().or(resource::getLastModified).orElse(null);
  }

  default Date getLastModified(QueryInput queryInput) {
    return queryInput.getLastModified().orElse(null);
  }

  default List<Link> getLinks(ApiRequestContext requestContext, I18n i18n) {
    return new DefaultLinksGenerator()
        .generateLinks(
            requestContext.getUriCustomizer(),
            requestContext.getMediaType(),
            requestContext.getAlternateMediaTypes(),
            i18n,
            requestContext.getLanguage());
  }
}
