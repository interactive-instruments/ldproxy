/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OutputFormatExtension;
import de.ii.ldproxy.ogcapi.infra.rest.ImmutableOgcApiRequestContext;
import de.ii.ldproxy.ogcapi.infra.rest.Wfs3RequestInjectableContext;
import de.ii.xtraplatform.server.CoreServerConfig;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Component
@Provides
//@Instantiate
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
            .put("mvt", new MediaType("application", "vnd.mapbox-vector-tile"))
            .put("jsonp", new MediaType("application", "javascript"))
            .build();

    @Requires
    private OgcApiExtensionRegistry extensionRegistry;

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

    private Set<OgcApiMediaType> getSupportedMediaTypes() {
        return extensionRegistry.getExtensionsForType(OutputFormatExtension.class)
                                .stream()
                                .map(OutputFormatExtension::getMediaType)
                                .collect(Collectors.toSet());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        evaluateFormatParameter(requestContext);

        String path = requestContext.getUriInfo()
                                    .getPath();
        if (!path.startsWith("admin")) {
            OgcApiMediaType ogcApiMediaType = negotiateMediaType(requestContext.getRequest())
                    .orElseThrow(NotAcceptableException::new);

            wfs3RequestContext.inject(requestContext, new ImmutableOgcApiRequestContext.Builder()
                    .requestUri(requestContext.getUriInfo()
                                              .getRequestUri())
                    .externalUri(externalUri)
                    .mediaType(ogcApiMediaType)
                    .build());
        }
    }

    private Optional<OgcApiMediaType> negotiateMediaType(Request request) {
        //TODO: mvt is added explicitely, adjust Wfs3ExtensionRegistry to support additional types
        Stream<MediaType> mediaTypeStream = Stream.concat(getSupportedMediaTypes().stream()
                                                                                  .flatMap(this::toTypes)
                                                                                  .distinct(), Stream.of(MIME_TYPES.get("mvt"), MIME_TYPES.get("jsonp")));

        MediaType[] supportedMediaTypes = mediaTypeStream.toArray(MediaType[]::new);

        Variant variant = request.selectVariant(Variant.mediaTypes(supportedMediaTypes)
                                                       .build());


        return Optional.ofNullable(variant)
                       .map(this::findMatchingWfs3MediaType);
    }

    private OgcApiMediaType findMatchingWfs3MediaType(Variant variant) {
        //TODO: mvt is added explicitely, adjust Wfs3ExtensionRegistry to support additional types
        Stream<OgcApiMediaType> wfs3MediaTypeStream = Stream.concat(getSupportedMediaTypes().stream(), Stream.of(new ImmutableOgcApiMediaType.Builder()
                        .main(MIME_TYPES.get("mvt"))
                        .build(),
                new ImmutableOgcApiMediaType.Builder()
                        .main(MIME_TYPES.get("jsonp"))
                        .build()));

        return wfs3MediaTypeStream.filter(wfs3MediaType -> wfs3MediaType.matches(variant.getMediaType()))
                                  .findFirst()
                                  .orElse(null);
    }

    private Stream<MediaType> toTypes(OgcApiMediaType ogcApiMediaType) {
        return Stream.of(ogcApiMediaType.main(), ogcApiMediaType.metadata())
                     .map(mediaType -> ogcApiMediaType.qs() < 1000 ? new QualitySourceMediaType(mediaType.getType(), mediaType.getSubtype(), ogcApiMediaType.qs(), mediaType.getParameters()) : mediaType);
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

            if (format.equals("json") && requestContext.getUriInfo()
                                                       .getQueryParameters()
                                                       .containsKey("callback")) {
                requestContext.getHeaders()
                              .putSingle(ACCEPT_HEADER, MIME_TYPES.get("jsonp")
                                                                  .toString());
            } else {

                final MediaType accept = MIME_TYPES.get(format);
                if (accept != null) {
                    requestContext.getHeaders()
                                  .putSingle(ACCEPT_HEADER, accept.toString());
                }
            }
        }
    }
}