/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.infra.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import org.glassfish.jersey.message.internal.QualitySourceMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ContentNegotiation {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentNegotiation.class);
    private static final String CONTENT_TYPE_PARAMETER = "f";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String LANGUAGE_PARAMETER = "lang";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    private static final List<String> USER_AGENT_BOTS = ImmutableList.of("googlebot", "bingbot", "duckduckbot",
                                                                         "yandexbot", "baiduspider", "slurp",
                                                                         "exabot", "facebot", "ia_archiver");

    public ContentNegotiation() {
    }

    public Optional<ApiMediaType> negotiate(ContainerRequestContext requestContext,
                                            ImmutableSet<ApiMediaType> supportedMediaTypes) {

        evaluateFormatParameter(supportedMediaTypes, requestContext.getUriInfo()
                                                                   .getQueryParameters(), requestContext.getHeaders());

        LOGGER.debug("accept {}", requestContext.getHeaderString(ACCEPT_HEADER));

        Optional<ApiMediaType> ogcApiMediaType = negotiateMediaType(supportedMediaTypes, requestContext.getRequest());

        LOGGER.debug("content-type {}", ogcApiMediaType);

        return ogcApiMediaType;
    }

    public Optional<ApiMediaType> negotiate(Request request, HttpHeaders httpHeaders, UriInfo uriInfo,
                                            ImmutableSet<ApiMediaType> supportedMediaTypes) {

        evaluateFormatParameter(supportedMediaTypes, uriInfo.getQueryParameters(), httpHeaders.getRequestHeaders());

        LOGGER.debug("accept {}", httpHeaders.getHeaderString(ACCEPT_HEADER));

        Optional<ApiMediaType> ogcApiMediaType = negotiateMediaType(supportedMediaTypes, request);

        LOGGER.debug("content-type {}", ogcApiMediaType);

        return ogcApiMediaType;
    }

    public Optional<Locale> negotiate(ContainerRequestContext requestContext) {

        evaluateLanguageParameter(
                requestContext.getUriInfo()
                    .getQueryParameters(),
                requestContext.getHeaders());

        LOGGER.debug("accept-language {}", requestContext.getHeaderString(ACCEPT_LANGUAGE_HEADER));

        Optional<Locale> locale = negotiateLanguage(requestContext.getRequest());

        LOGGER.debug("content-language {}", locale);

        return locale;
    }

    public Optional<Locale> negotiate(Request request, HttpHeaders httpHeaders, UriInfo uriInfo) {

        evaluateLanguageParameter(uriInfo.getQueryParameters(), httpHeaders.getRequestHeaders());

        LOGGER.debug("accept-language {}", httpHeaders.getHeaderString(ACCEPT_LANGUAGE_HEADER));

        Optional<Locale> locale = negotiateLanguage(request);

        LOGGER.debug("content-language {}", locale);

        return locale;
    }

    private void evaluateFormatParameter(
            ImmutableSet<ApiMediaType> supportedMediaTypes,
            MultivaluedMap<String, String> queryParameters,
            MultivaluedMap<String, String> headers) {

        String format = null;
        if (queryParameters.containsKey(CONTENT_TYPE_PARAMETER)) {
            format = queryParameters.getFirst(CONTENT_TYPE_PARAMETER);
        } else {
            // use crawler user agent headers to trigger an implicit f=html
            String userAgent = headers.getFirst("user-agent");
            if (Objects.nonNull(userAgent)) {
                String finalUserAgent = userAgent.toLowerCase();
                if (USER_AGENT_BOTS.stream()
                                   .anyMatch(bot -> finalUserAgent.contains(bot))) {
                    format = "html";
                }
            }
        }

        if (Objects.nonNull(format)) {
            String finalFormat = format;
            Optional<ApiMediaType> ogcApiMediaType = supportedMediaTypes.stream()
                                                                        .filter(mediaType -> Objects.equals(mediaType.parameter(), finalFormat))
                                                                        .findFirst();
            if (ogcApiMediaType.isPresent()) {
                headers.putSingle(ACCEPT_HEADER, ogcApiMediaType.get()
                                                                .type()
                                                                .toString());
            }
        }
    }

    private Optional<ApiMediaType> negotiateMediaType(
            ImmutableSet<ApiMediaType> supportedMediaTypes,
            Request request) {
        MediaType[] supportedMediaTypesArray = supportedMediaTypes.stream()
                                                                  .flatMap(this::toTypes)
                                                                  .distinct()
                                                                  .toArray(MediaType[]::new);

        Variant variant = null;
        try {
            if (supportedMediaTypesArray.length > 0) {
                variant = request.selectVariant(Variant.mediaTypes(supportedMediaTypesArray)
                                                       .build());
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not parse request headers during content negotiation. Selecting any media type. Reason: {}", ex.getMessage());
            return supportedMediaTypes.stream().findAny();
        }

        return Optional.ofNullable(variant)
                       .map(Variant::getMediaType)
                       .flatMap(mediaType -> findMatchingOgcApiMediaType(mediaType, supportedMediaTypes));
    }

    private Optional<ApiMediaType> findMatchingOgcApiMediaType(MediaType mediaType,
                                                               ImmutableSet<ApiMediaType> supportedMediaTypes) {
        return supportedMediaTypes.stream()
                                  .filter(type -> type.matches(mediaType))
                                  .findFirst();
    }

    private Stream<MediaType> toTypes(ApiMediaType apiMediaType) {
        return Stream.of(apiMediaType.type())
                     .map(mediaType -> apiMediaType.qs() < 1000 ? new QualitySourceMediaType(mediaType.getType(), mediaType.getSubtype(), apiMediaType.qs(), mediaType.getParameters()) : mediaType);
    }

    private void evaluateLanguageParameter(
            MultivaluedMap<String, String> queryParameters,
            MultivaluedMap<String, String> headers) {

        if (queryParameters.containsKey(LANGUAGE_PARAMETER)) {
            String locale = queryParameters.getFirst(LANGUAGE_PARAMETER);

            Optional<Locale> ogcApiLocale = I18n.getLanguages().stream()
                    .filter(language -> Objects.equals(language.getLanguage(), locale))
                    .findFirst();
            if (ogcApiLocale.isPresent()) {
                headers.putSingle(ACCEPT_LANGUAGE_HEADER, ogcApiLocale.get()
                        .getLanguage());
            }
        }
    }

    private Optional<Locale> negotiateLanguage(
            Request request) {
        Locale[] supportedLanguagesArray = I18n.getLanguages().stream()
                .toArray(Locale[]::new);

        Variant variant = null;
        try {
            if (supportedLanguagesArray.length > 0) {
                variant = request.selectVariant(Variant.languages(supportedLanguagesArray)
                                                       .build());
            }
        } catch (Exception ex) {
            LOGGER.warn("Could not parse request headers during content negotiation. Selecting any language. Reason: {}", ex.getMessage());
            return Optional.ofNullable(supportedLanguagesArray[0]);
        }

        return Optional.ofNullable(variant)
                .map(Variant::getLanguage)
                .flatMap(locale -> findMatchingLanguage(locale, I18n.getLanguages()));
    }

    private Optional<Locale> findMatchingLanguage(Locale locale, Set<Locale> supportedLocales) {
        return supportedLocales.stream()
                .filter(language -> Objects.equals(language.getLanguage(),locale.getLanguage()))
                .findFirst();
    }

}
