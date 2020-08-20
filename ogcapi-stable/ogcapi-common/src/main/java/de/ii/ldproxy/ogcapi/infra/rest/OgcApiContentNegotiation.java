/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import org.glassfish.jersey.message.internal.QualitySourceMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class OgcApiContentNegotiation {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiContentNegotiation.class);
    private static final String CONTENT_TYPE_PARAMETER = "f";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String LANGUAGE_PARAMETER = "lang";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    public OgcApiContentNegotiation() {
    }

    public Optional<OgcApiMediaType> negotiate(ContainerRequestContext requestContext,
                                               ImmutableSet<OgcApiMediaType> supportedMediaTypes) {

        evaluateFormatParameter(supportedMediaTypes, requestContext.getUriInfo()
                                                                   .getQueryParameters(), requestContext.getHeaders());

        LOGGER.debug("accept {}", requestContext.getHeaderString(ACCEPT_HEADER));

        Optional<OgcApiMediaType> ogcApiMediaType = negotiateMediaType(supportedMediaTypes, requestContext.getRequest());

        LOGGER.debug("content-type {}", ogcApiMediaType);

        return ogcApiMediaType;
    }

    public Optional<OgcApiMediaType> negotiate(Request request, HttpHeaders httpHeaders, UriInfo uriInfo,
                                               ImmutableSet<OgcApiMediaType> supportedMediaTypes) {

        evaluateFormatParameter(supportedMediaTypes, uriInfo.getQueryParameters(), httpHeaders.getRequestHeaders());

        LOGGER.debug("accept {}", httpHeaders.getHeaderString(ACCEPT_HEADER));

        Optional<OgcApiMediaType> ogcApiMediaType = negotiateMediaType(supportedMediaTypes, request);

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
            ImmutableSet<OgcApiMediaType> supportedMediaTypes,
            MultivaluedMap<String, String> queryParameters,
            MultivaluedMap<String, String> headers) {

        if (queryParameters.containsKey(CONTENT_TYPE_PARAMETER)) {
            String format = queryParameters.getFirst(CONTENT_TYPE_PARAMETER);

            Optional<OgcApiMediaType> ogcApiMediaType = supportedMediaTypes.stream()
                                                                           .filter(mediaType -> Objects.equals(mediaType.parameter(), format))
                                                                           .findFirst();
            if (ogcApiMediaType.isPresent()) {
                headers.putSingle(ACCEPT_HEADER, ogcApiMediaType.get()
                                                                .type()
                                                                .toString());
            }
        }
    }

    private Optional<OgcApiMediaType> negotiateMediaType(
            ImmutableSet<OgcApiMediaType> supportedMediaTypes,
            Request request) {
        MediaType[] supportedMediaTypesArray = supportedMediaTypes.stream()
                                                                  .flatMap(this::toTypes)
                                                                  .distinct()
                                                                  .toArray(MediaType[]::new);

        Variant variant = null;
        if (supportedMediaTypesArray.length > 0) {
            variant = request.selectVariant(Variant.mediaTypes(supportedMediaTypesArray)
                                         .build());
        }

        return Optional.ofNullable(variant)
                       .map(Variant::getMediaType)
                       .flatMap(mediaType -> findMatchingOgcApiMediaType(mediaType, supportedMediaTypes));
    }

    private Optional<OgcApiMediaType> findMatchingOgcApiMediaType(MediaType mediaType,
                                                                  ImmutableSet<OgcApiMediaType> supportedMediaTypes) {
        return supportedMediaTypes.stream()
                                  .filter(type -> type.matches(mediaType))
                                  .findFirst();
    }

    private Stream<MediaType> toTypes(OgcApiMediaType ogcApiMediaType) {
        return Stream.of(ogcApiMediaType.type())
                     .map(mediaType -> ogcApiMediaType.qs() < 1000 ? new QualitySourceMediaType(mediaType.getType(), mediaType.getSubtype(), ogcApiMediaType.qs(), mediaType.getParameters()) : mediaType);
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
        if (supportedLanguagesArray.length > 0) {
            variant = request.selectVariant(Variant.languages(supportedLanguagesArray)
                    .build());
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
