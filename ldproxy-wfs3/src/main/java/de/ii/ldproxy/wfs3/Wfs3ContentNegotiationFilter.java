/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.xsf.core.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.glassfish.jersey.message.internal.QualitySourceMediaType;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Variant;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@PreMatching
public class Wfs3ContentNegotiationFilter implements ContainerRequestFilter {
    private static final String CONTENT_TYPE_PARAMETER = "f";
    private static final String ACCEPT_HEADER = "Accept";
    private static final Map<String, MediaType> MIME_TYPES = new ImmutableMap.Builder<String, MediaType>()
            .put("json", MediaType.APPLICATION_JSON_TYPE)
            .put("jsonld", new MediaType("application", "ld+json"))
            .put("geojson", new MediaType("application", "geo+json"))
            .put("yaml", new MediaType("application", "yaml"))
            .put("html", MediaType.TEXT_HTML_TYPE)
            .put("xml", MediaType.APPLICATION_XML_TYPE)
            .build();

    @Requires
    private Wfs3ExtensionRegistry wfs3ConformanceClassRegistry;

    @Requires
    private Wfs3RequestInjectableContext wfs3RequestContext;

    private Optional<URI> externalUri = Optional.empty();

    @Bind
    void setCore(CoreServerConfig coreServerConfig) {
        URI externalUri = null;
        try {
            externalUri = new URI(coreServerConfig.getExternalUrl());
        } catch (URISyntaxException e) {
            // ignore
        }

        this.externalUri = Optional.ofNullable(externalUri);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        evaluateFormatParameter(requestContext);

        Wfs3MediaType wfs3MediaType = negotiateMediaType(requestContext.getRequest())
                .orElseThrow(NotAcceptableException::new);

        wfs3RequestContext.inject(requestContext, ImmutableWfs3RequestContextImpl.builder()
                                                                                 .requestUri(requestContext.getUriInfo()
                                                                                                           .getRequestUri())
                                                                                 .externalUri(externalUri)
                                                                                 .mediaType(wfs3MediaType)
                                                                                 .build());
    }

    private Optional<Wfs3MediaType> negotiateMediaType(Request request) {
        MediaType[] supportedMediaTypes = wfs3ConformanceClassRegistry.getOutputFormats()
                                                                      .keySet()
                                                                      .stream()
                                                                      .flatMap(this::toTypes)
                                                                      .distinct()
                                                                      .toArray(MediaType[]::new);

        Variant variant = request.selectVariant(Variant.mediaTypes(supportedMediaTypes)
                                                       .build());


        return Optional.ofNullable(variant)
                       .map(this::findMatchingWfs3MediaType);
    }

    private Wfs3MediaType findMatchingWfs3MediaType(Variant variant) {
        return wfs3ConformanceClassRegistry.getOutputFormats()
                                           .keySet()
                                           .stream()
                                           .filter(wfs3MediaType -> wfs3MediaType.matches(variant.getMediaType()))
                                           .findFirst()
                                           .orElse(null);
    }

    private Stream<MediaType> toTypes(Wfs3MediaType wfs3MediaType) {
        return Stream.of(wfs3MediaType.main(), wfs3MediaType.metadata())
                     .map(mediaType -> wfs3MediaType.qs() < 1000 ? new QualitySourceMediaType(mediaType.getType(), mediaType.getSubtype(), wfs3MediaType.qs(), mediaType.getParameters()) : mediaType);
    }

    //TODO: parameter values from outputformats
    private void evaluateFormatParameter(ContainerRequestContext requestContext) throws IOException {
        // Quick check for a 'f' parameter
        if (!requestContext.getUriInfo()
                           .getQueryParameters()
                           .containsKey(CONTENT_TYPE_PARAMETER)) {
            // overwrite wildcard
            if (requestContext.getHeaderString(ACCEPT_HEADER) == null || requestContext.getHeaderString(ACCEPT_HEADER)
                                                                                       .length() == 0 || requestContext.getHeaders()
                                                                                                                       .getFirst(ACCEPT_HEADER)
                                                                                                                       .trim()
                                                                                                                       .equals("*/*")) {
                requestContext.getHeaders()
                              .putSingle(ACCEPT_HEADER, MediaType.APPLICATION_JSON);
            }
        } else {
            String format = requestContext.getUriInfo()
                                          .getQueryParameters()
                                          .getFirst(CONTENT_TYPE_PARAMETER);

            final MediaType accept = MIME_TYPES.get(format);
            if (accept != null) {
                requestContext.getHeaders()
                              .putSingle(ACCEPT_HEADER, accept.toString());
            }
        }
    }
}