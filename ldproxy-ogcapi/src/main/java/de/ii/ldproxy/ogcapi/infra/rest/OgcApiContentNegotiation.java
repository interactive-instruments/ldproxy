/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import org.glassfish.jersey.message.internal.QualitySourceMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OgcApiContentNegotiation {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiContentNegotiation.class);
    private static final String CONTENT_TYPE_PARAMETER = "f";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String DEFAULT_MEDIA_TYPE = MediaType.APPLICATION_JSON;

    public OgcApiContentNegotiation() {
    }

    public Optional<OgcApiMediaType> negotiate(ContainerRequestContext requestContext,
                                               ImmutableSet<OgcApiMediaType> supportedMediaTypes) {

        evaluateFormatParameter(supportedMediaTypes, requestContext.getUriInfo()
                                                                   .getQueryParameters(), requestContext.getHeaders());

        LOGGER.debug("accept {}", requestContext.getHeaderString(ACCEPT_HEADER));

        Optional<OgcApiMediaType> ogcApiMediaType = negotiateMediaType(supportedMediaTypes, requestContext.getRequest());

        LOGGER.debug("accepted {}", ogcApiMediaType);

        return ogcApiMediaType;
    }

    private void evaluateFormatParameter(
            ImmutableSet<OgcApiMediaType> supportedMediaTypes,
            MultivaluedMap<String, String> queryParameters,
            MultivaluedMap<String, String> headers) {

        if (queryParameters.containsKey(CONTENT_TYPE_PARAMETER)) {
            String format = queryParameters.getFirst(CONTENT_TYPE_PARAMETER);

            /*if (format.equals("json") && queryParameters.containsKey("callback")) {
                headers.putSingle(ACCEPT_HEADER, MIME_TYPES.get("jsonp")
                                                           .toString());
            } else {*/

            Optional<OgcApiMediaType> ogcApiMediaType = supportedMediaTypes.stream()
                                                                           .filter(mediaType -> Objects.equals(mediaType.parameter(), format))
                                                                           .findFirst();
            if (ogcApiMediaType.isPresent()) {
                headers.putSingle(ACCEPT_HEADER, ogcApiMediaType.get()
                                                                .main()
                                                                .toString());
            }
            //}
        } else {
            // if no accept header or wildcard
            if (Strings.isNullOrEmpty(headers.getFirst(ACCEPT_HEADER)) || headers.getFirst(ACCEPT_HEADER)
                                                                                 .trim()
                                                                                 .equals("*/*")) {
                headers.putSingle(ACCEPT_HEADER, DEFAULT_MEDIA_TYPE);
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
                                  .filter(wfs3MediaType -> wfs3MediaType.matches(mediaType))
                                  .findFirst();
    }

    private Stream<MediaType> toTypes(OgcApiMediaType ogcApiMediaType) {
        return Stream.of(ogcApiMediaType.main(), ogcApiMediaType.metadata())
                     .map(mediaType -> ogcApiMediaType.qs() < 1000 ? new QualitySourceMediaType(mediaType.getType(), mediaType.getSubtype(), ogcApiMediaType.qs(), mediaType.getParameters()) : mediaType);
    }
}
