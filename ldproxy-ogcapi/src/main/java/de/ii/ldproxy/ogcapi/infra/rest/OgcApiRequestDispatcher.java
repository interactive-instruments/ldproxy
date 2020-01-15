/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.server.CoreServerConfig;
import de.ii.xtraplatform.service.api.ServiceResource;
import org.apache.felix.ipojo.annotations.*;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;


@Component
@Provides(properties = {
        @StaticServiceProperty(name = ServiceResource.SERVICE_TYPE_KEY, type = "java.lang.String", value = "WFS3")
})
@Instantiate

@PermitAll
public class OgcApiRequestDispatcher implements ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiRequestDispatcher.class);

    private final OgcApiExtensionRegistry extensionRegistry;
    private final OgcApiRequestInjectableContext ogcApiInjectableContext;
    private final CoreServerConfig coreServerConfig;
    private final OgcApiContentNegotiation ogcApiContentNegotiation;

    OgcApiRequestDispatcher(@Requires OgcApiExtensionRegistry extensionRegistry,
                            @Requires OgcApiRequestInjectableContext ogcApiInjectableContext,
                            @Requires CoreServerConfig coreServerConfig) {
        this.extensionRegistry = extensionRegistry;
        this.ogcApiInjectableContext = ogcApiInjectableContext;
        this.coreServerConfig = coreServerConfig;
        this.ogcApiContentNegotiation = new OgcApiContentNegotiation();
    }

    @Path("")
    public OgcApiEndpointExtension dispatchLandingPageWithoutSlash(@PathParam("entrypoint") String entrypoint, @Context OgcApiDataset service,
                                                                   @Context ContainerRequestContext requestContext) {
        return dispatch("", service, requestContext);

    }

    @Path("/{entrypoint: [^/]*}")
    public OgcApiEndpointExtension dispatch(@PathParam("entrypoint") String entrypoint, @Context OgcApiDataset service,
                                            @Context ContainerRequestContext requestContext) {

        String subPath = ((UriRoutingContext) requestContext.getUriInfo()).getFinalMatchingGroup();
        String method = requestContext.getMethod();

        OgcApiEndpointExtension ogcApiEndpoint = findEndpoint(service.getData(), entrypoint, subPath, method).orElse(null);

        if (ogcApiEndpoint==null) {
            if (findEndpoint(service.getData(), entrypoint, subPath, null).isPresent())
                throw new NotAllowedException("Method "+method+" is not supported for this resource.");
            // TODO does this belong here or should this be done by the resources?
            // check, if this may be an issue of special characters in the path, replace all non-Word characters with an underscore and test the sub path again
            String subPathReduced = subPath.replaceAll("\\W","_");
            if (findEndpoint(service.getData(), entrypoint, subPathReduced, null).isPresent())
                throw new BadRequestException("The sub path '"+subPath+"' includes characters that are not supported for a resource. Resource ids typically only support word characters (ASCII letters, digits, underscore) for the resource names.");
            throw new NotFoundException();
        }

        Set<String> parameters = requestContext.getUriInfo().getQueryParameters().keySet();
        Set<String> knownParameters = ogcApiEndpoint.getParameters(service.getData(), subPath);
        Set<String> unknownParameters = parameters.stream()
                .filter(parameter -> !knownParameters.contains(parameter))
                .collect(Collectors.toSet());
        if (!unknownParameters.isEmpty()) {
            throw new BadRequestException("The following query parameters are rejected: " +
                    String.join(", ", unknownParameters) +
                    ". Valid parameters for this request are: " +
                    String.join(", ", knownParameters));
        }

        ImmutableSet<OgcApiMediaType> supportedMediaTypes = ogcApiEndpoint.getMediaTypes(service.getData(), subPath);

        OgcApiMediaType selectedMediaType = ogcApiContentNegotiation.negotiate(requestContext, supportedMediaTypes)
                                                                    .orElseThrow(NotAcceptableException::new);

        Locale selectedLanguage = ogcApiContentNegotiation.negotiate(requestContext)
                                                          .orElse(Locale.ENGLISH);

        Set<OgcApiMediaType> alternateMediaTypes = getAlternateMediaTypes(selectedMediaType, supportedMediaTypes);

        OgcApiRequestContext ogcApiRequestContext = new ImmutableOgcApiRequestContext.Builder()
                .requestUri(requestContext.getUriInfo()
                                          .getRequestUri())
                .externalUri(getExternalUri())
                .mediaType(selectedMediaType)
                .alternateMediaTypes(alternateMediaTypes)
                .language(selectedLanguage)
                .api(service)
                .build();

        // validate generic parameters
        String f = requestContext.getUriInfo().getQueryParameters().getFirst("f");
        if (f!=null) {
            boolean found = supportedMediaTypes.stream()
                    .map(mediaType -> mediaType.parameter())
                    .anyMatch(value -> value != null && value.equalsIgnoreCase(f));
            if (!found) {
                Set<String> validValues = supportedMediaTypes.stream()
                        .map(mediaType -> mediaType.parameter())
                        .filter(value -> value != null && !value.isEmpty())
                        .collect(Collectors.toSet());
                throw new BadRequestException("Invalid value for parameter 'f': " + f + ". Valid values: " + String.join(", ",validValues)+".");
            }
        }

        // TODO check lang, too

        ogcApiInjectableContext.inject(requestContext, ogcApiRequestContext);

        return ogcApiEndpoint;
    }

    private Set<OgcApiMediaType> getAlternateMediaTypes(OgcApiMediaType selectedMediaType,
                                                          Set<OgcApiMediaType> mediaTypes) {
        return mediaTypes.stream()
                         .filter(mediaType -> !Objects.equals(mediaType, selectedMediaType))
                         .collect(ImmutableSet.toImmutableSet());
    }

    private Optional<OgcApiEndpointExtension> findEndpoint(OgcApiDatasetData dataset,
                                                           @PathParam("entrypoint") String entrypoint,
                                                           String subPath, String method) {
        return getEndpoints().stream()
                             .filter(wfs3Endpoint -> wfs3Endpoint.getApiContext()
                                                                 .matches(entrypoint, subPath, method))
                             .filter(wfs3Endpoint -> wfs3Endpoint.isEnabledForApi(dataset))
                             .findFirst();
    }

    private List<OgcApiEndpointExtension> getEndpoints() {
        return extensionRegistry.getExtensionsForType(OgcApiEndpointExtension.class);
    }

    private Optional<URI> getExternalUri() {
        URI externalUri = null;
        try {
            externalUri = new URI(coreServerConfig.getExternalUrl());
        } catch (URISyntaxException e) {
            // return null
        }

        return Optional.ofNullable(externalUri);
    }
}
