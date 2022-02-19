/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.Files;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SimpleTimeZone;
import java.util.stream.Collectors;

@AutoMultiBind
public interface QueriesHandler<T extends QueryIdentifier> {

    Logger LOGGER = LoggerFactory.getLogger(QueriesHandler.class);

    Locale[] LANGUAGES = I18n.getLanguages()
                             .stream()
                             .collect(Collectors.toUnmodifiableList())
                             .toArray(Locale[]::new);
    String[] ENCODINGS = {"gzip", "identity"};

    Map<T, QueryHandler<? extends QueryInput>> getQueryHandlers();

    default Response handle(T queryIdentifier, QueryInput queryInput,
                            ApiRequestContext requestContext) {

        QueryHandler<? extends QueryInput> queryHandler = getQueryHandlers().get(queryIdentifier);

       if (Objects.isNull(queryHandler)) {
           throw new IllegalStateException("No query handler found for " + queryIdentifier +".");
       }

        if (!queryHandler.isValidInput(queryInput)) {
            throw new RuntimeException(MessageFormat.format("Invalid query handler {0} for query input of class {1}.", queryHandler.getClass().getSimpleName(), queryInput.getClass().getSimpleName()));
        }

        return queryHandler.handle(queryInput, requestContext);
    }

    default Response.ResponseBuilder prepareSuccessResponse(OgcApi api,
                                                            ApiRequestContext requestContext) {
        return prepareSuccessResponse(api, requestContext, null);
    }

    default Response.ResponseBuilder prepareSuccessResponse(OgcApi api,
                                                            ApiRequestContext requestContext,
                                                            List<Link> links) {
        return prepareSuccessResponse(api, requestContext, links, null);
    }

    default Response.ResponseBuilder prepareSuccessResponse(OgcApi api,
                                                            ApiRequestContext requestContext,
                                                            List<Link> links,
                                                            EpsgCrs crs) {
        Response.ResponseBuilder response = Response.ok()
                                                    .type(requestContext
                                                        .getMediaType()
                                                        .type());

        Optional<Locale> language = requestContext.getLanguage();
        if (language.isPresent())
            response.language(language.get());

        if (links != null)
            // skip URI templates in the header as these are not RFC 8288 links
            links.stream()
                    .filter(link -> link.getTemplated()==null || !link.getTemplated())
                    .forEach(link -> response.links(link.getLink()));

        if (crs != null)
            response.header("Content-Crs", "<" + crs.toUriString() + ">");

        return response;
    }

    default Response.ResponseBuilder evaluatePreconditions(ApiRequestContext requestContext,
                                                           Date lastModified,
                                                           EntityTag etag) {
        if (requestContext.getRequest().isPresent()) {
            Request request = requestContext.getRequest().get();
            try {
                if (Objects.nonNull(lastModified) && Objects.nonNull(etag))
                    return request.evaluatePreconditions(lastModified, etag);
                else if (Objects.nonNull(etag))
                    return request.evaluatePreconditions(etag);
                else if (Objects.nonNull(lastModified))
                    return request.evaluatePreconditions(lastModified);
                else
                    return request.evaluatePreconditions();
            } catch (Exception e) {
                // could not parse headers, so silently ignore them and return the regular response
                LOGGER.debug("Ignoring invalid condition request headers: {}", e.getMessage());
            }
        }

        return null;
    }

    default Response.ResponseBuilder prepareSuccessResponse(ApiRequestContext requestContext,
                                                            List<Link> links,
                                                            Date lastModified,
                                                            EntityTag etag,
                                                            String cacheControl,
                                                            Date expires,
                                                            EpsgCrs crs,
                                                            boolean inline,
                                                            String filename) {
        Response.ResponseBuilder response = Response.ok()
                                                    .type(requestContext
                                                                  .getMediaType()
                                                                  .type());

        if (Objects.nonNull(lastModified))
            response.lastModified(lastModified);

        if (Objects.nonNull(etag))
            response.tag(etag);

        if (Objects.nonNull(cacheControl))
            response.cacheControl(CacheControl.valueOf(cacheControl));

        if (Objects.nonNull(expires))
            response.expires(expires);

        response.variants(Variant.mediaTypes(new ImmutableList.Builder<ApiMediaType>().add(requestContext.getMediaType())
                                                                                      .addAll(requestContext.getAlternateMediaTypes())
                                                                                      .build()
                                                                                      .stream()
                                                                                      .map(ApiMediaType::type)
                                                                                      .toArray(MediaType[]::new))
                                 .languages(LANGUAGES)
                                 .encodings(ENCODINGS)
                                 .add()
                                 .build());

        requestContext.getLanguage()
                      .ifPresent(response::language);

        if (links != null)
            // skip URI templates in the header as these are not RFC 8288 links
            links.stream()
                 .filter(link -> link.getTemplated()==null || !link.getTemplated())
                 .forEach(link -> response.links(link.getLink()));

        if (crs != null)
            response.header("Content-Crs", "<" + crs.toUriString() + ">");

        if (!inline || Objects.nonNull(filename)) {
            response.header("Content-Disposition", (inline ? "inline" : "attachment") + (Objects.nonNull(filename) ? "; filename=\""+filename+"\"" : ""));

        }

        return response;
    }

    default Date getLastModified(QueryInput queryInput, PageRepresentation resource) {
        return queryInput.getLastModified()
                         .orElse(resource.getLastModified()
                                         .orElse(null));
    }

    default Date getLastModified(QueryInput queryInput, OgcApi api) {
        return queryInput.getLastModified()
                         .orElse(null);
                         /* TODO since the information in the service configuration is not updated,
                             if the file is updated outside of the manager, this is currently not a reliable mechanism
                         .orElse(Date.from(Instant.ofEpochMilli(api.getData()
                                                                   .getLastModified())));
                          */
    }

    default Date getLastModified(QueryInput queryInput, OgcApi api, FeatureProvider2 provider) {
        return queryInput.getLastModified()
                         .orElse(null);
                         /* TODO since the information in the provider or service configuration is not updated,
                             if the file is updated outside of the manager, this is currently not a reliable mechanism
                         .orElse(Date.from(Instant.ofEpochMilli(Math.max(api.getData()
                                                                            .getLastModified(),
                                                                         provider.getData()
                                                                                 .getLastModified()))));
                          */
    }

    default Date getLastModified(File file) {
        return Date.from(Instant.ofEpochMilli(file.lastModified()));
    }

    @SuppressWarnings("UnstableApiUsage")
    default EntityTag getEtag(Date date) {
        if (Objects.isNull(date))
            return null;

        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        sdf.applyPattern("dd MMM yyyy HH:mm:ss z");
        String etag = Hashing.murmur3_128()
                             .hashString(sdf.format(date), StandardCharsets.UTF_8)
                             .toString();
        return new EntityTag(etag, true);
    }

    @SuppressWarnings("UnstableApiUsage")
    default EntityTag getEtag(byte[] byteArray) {
        String etag = Hashing.murmur3_128()
                             .hashBytes(byteArray)
                             .toString();
        return new EntityTag(etag, false);
    }

    @SuppressWarnings("UnstableApiUsage")
    default EntityTag getEtag(File file) {
        String etag;
        try {
            etag = Files.asByteSource(file).hash(Hashing.murmur3_128()).toString();
        } catch (IOException e) {
            return null;
        }
        return new EntityTag(etag, false);
    }

    @SuppressWarnings("UnstableApiUsage")
    default EntityTag getEtag(InputStream inputStream) {
        String etag = new HashingInputStream(Hashing.murmur3_128(), inputStream).hash().toString();
        return new EntityTag(etag, false);
    }

    @SuppressWarnings("UnstableApiUsage")
    default <S> EntityTag getEtag(S entity, Funnel<S> funnel, FormatExtension outputFormat) {
        String etag = Hashing.murmur3_128()
                             .newHasher()
                             .putObject(entity, funnel)
                             .putString(Objects.nonNull(outputFormat) ? outputFormat.getMediaType().label() : "", StandardCharsets.UTF_8)
                             .hash()
                             .toString();
        return new EntityTag(etag, true);
    }

    /**
     * Analyse the error reported by a feature stream. If it looks like a server-side error, re-throw
     * the exception, otherwise continue
     * @param error the exception reported by xtraplatform
     */
    default void processStreamError(Throwable error) {
        String errorMessage = error.getMessage();
        if (Objects.isNull(errorMessage))
            errorMessage = error.getClass().getSimpleName() + " at " + error.getStackTrace()[0].toString();
        while (Objects.nonNull(error) && !Objects.equals(error,error.getCause())) {
            if (error instanceof org.eclipse.jetty.io.EofException) {
                // the connection has been lost, typically the client has cancelled the request, log on debug level
                return;
            } else if (error instanceof UnprocessableEntity) {
                // Cannot handle request
                throw new WebApplicationException(error.getMessage(), 422);
            } else if (error instanceof RuntimeException) {
                // Runtime exception is generated by XtraPlatform, look at the cause
                error = error.getCause();
            } else {
                // some other exception occurred, log as an error
                break;
            }
        }

        throw new InternalServerErrorException(errorMessage, error);
    }

}
