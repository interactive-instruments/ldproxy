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
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.domain.OgcApiEndpointDefinition.SORT_PRIORITY_DUMMY;


@Component
@Provides(properties = {
        @StaticServiceProperty(name = ServiceResource.SERVICE_TYPE_KEY, type = "java.lang.String", value = "OGC_API")
})
@Instantiate

@PermitAll
public class OgcApiRequestDispatcher implements ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiRequestDispatcher.class);

    private static final Set<String> NOCONTENT_METHODS = ImmutableSet.of("POST", "PUT", "DELETE");
    private static final OgcApiMediaType DEFAULT_MEDIA_TYPE = new ImmutableOgcApiMediaType.Builder()
            .type(new MediaType("application", "json"))
            .label("JSON")
            .parameter("json")
            .build();

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
    public OgcApiEndpointExtension dispatchLandingPageWithoutSlash(@PathParam("entrypoint") String entrypoint, @Context OgcApiApi service,
                                                                   @Context ContainerRequestContext requestContext) {
        return dispatch("", service, requestContext);

    }

    @Path("/{entrypoint: [^/]*}")
    public OgcApiEndpointExtension dispatch(@PathParam("entrypoint") String entrypoint, @Context OgcApiApi service,
                                            @Context ContainerRequestContext requestContext) {

        String subPath = ((UriRoutingContext) requestContext.getUriInfo()).getFinalMatchingGroup();
        String method = requestContext.getMethod();

        OgcApiEndpointExtension ogcApiEndpoint = findEndpoint(service.getData(), entrypoint, subPath, method).orElse(null);

        if (ogcApiEndpoint==null) {
            throwNotAllowedOrNotFound(getMethods(service.getData(), entrypoint, subPath));
            /* TODO should this belong here or should this be done by the resources?
            // check, if this may be an issue of special characters in the path, replace all non-Word characters with an underscore and test the sub path again
            String subPathReduced = subPath.replaceAll("\\W","_");
            if (findEndpoint(service.getData(), entrypoint, subPathReduced, null).isPresent())
                throw new BadRequestException("The sub path '"+subPath+"' includes characters that are not supported for a resource. Resource ids typically only support word characters (ASCII letters, digits, underscore) for the resource names.");
            throw new NotFoundException();
             */
        }

        Set<String> parameters = requestContext.getUriInfo().getQueryParameters().keySet();
        List<OgcApiQueryParameter> knownParameters = ogcApiEndpoint.getParameters(service.getData(), subPath);
        Set<String> unknownParameters = parameters.stream()
                .filter(parameter -> !knownParameters.stream().filter(param -> param.getName().equalsIgnoreCase(parameter)).findAny().isPresent())
                .collect(Collectors.toSet());
        if (!unknownParameters.isEmpty()) {
            throw new BadRequestException("The following query parameters are rejected: " +
                    String.join(", ", unknownParameters) +
                    ". Valid parameters for this request are: " +
                    String.join(", ", knownParameters.stream().map(OgcApiParameter::getName).collect(Collectors.toList())));
        }

        ImmutableSet<OgcApiMediaType> supportedMediaTypes = method.equals("GET") || method.equals("HEAD") ?
                ogcApiEndpoint.getMediaTypes(service.getData(), subPath):
                ogcApiEndpoint.getMediaTypes(service.getData(), subPath, method);

        OgcApiMediaType selectedMediaType;
        Set<OgcApiMediaType> alternateMediaTypes;
        if (supportedMediaTypes.isEmpty() && NOCONTENT_METHODS.contains(method)) {
            selectedMediaType = DEFAULT_MEDIA_TYPE;
            alternateMediaTypes = ImmutableSet.of();

        } else {
            selectedMediaType = ogcApiContentNegotiation.negotiate(requestContext, supportedMediaTypes)
                    .orElseThrow(NotAcceptableException::new);
            alternateMediaTypes = getAlternateMediaTypes(selectedMediaType, supportedMediaTypes);

        }

        Locale selectedLanguage = ogcApiContentNegotiation.negotiate(requestContext)
                                                          .orElse(Locale.ENGLISH);

        OgcApiRequestContext ogcApiRequestContext = new ImmutableOgcApiRequestContext.Builder()
                .requestUri(requestContext.getUriInfo()
                                          .getRequestUri())
                .externalUri(getExternalUri())
                .mediaType(selectedMediaType)
                .alternateMediaTypes(alternateMediaTypes)
                .language(selectedLanguage)
                .api(service)
                .build();

        // validate request
        OgcApiEndpointDefinition apiDef = ogcApiEndpoint.getDefinition(service.getData());
        if (!apiDef.getResources().isEmpty()) {
            // check that the subPath is valid
            OgcApiResource resource = apiDef.getResource("/" + entrypoint + subPath).orElse(null);
            if (resource==null)
                throw new NotFoundException();

            // no need to check the path parameters here, only the parent path parameters (service, endpoint) are available;
            // path parameters in the sub-path have to be checked later
            OgcApiOperation operation = apiDef.getOperation(resource, method).orElse(null);
            if (operation==null) {
                throwNotAllowedOrNotFound(getMethods(service.getData(),entrypoint,subPath));
            }

            Optional<String> collectionId = resource.getCollectionId();

            // validate query parameters
            requestContext.getUriInfo()
                    .getQueryParameters()
                    .entrySet()
                    .stream()
                    .forEach(p -> {
                        String name = p.getKey();
                        List<String> values = p.getValue();
                        operation.getQueryParameters()
                                .stream()
                                .filter(param -> param.getName().equalsIgnoreCase(name))
                                .forEach(param -> {
                                    Optional<String> result = param.validate(service.getData(), collectionId, values);
                                    if (result.isPresent())
                                        throw new BadRequestException(result.get());
                                });
                    });
        }

        // TODO check lang, too

        ogcApiInjectableContext.inject(requestContext, ogcApiRequestContext);

        return ogcApiEndpoint;
    }

    private void throwNotAllowedOrNotFound(Set<String> methods) {
        if (!methods.isEmpty()) {
            String first = methods.stream().findFirst().get();
            String[] more = methods.stream()
                    .filter(method -> !method.equals(first))
                    .toArray(String[]::new);
            throw new NotAllowedException(first,more);
        } else
            throw new NotFoundException();
    }

    private Set<OgcApiMediaType> getAlternateMediaTypes(OgcApiMediaType selectedMediaType,
                                                          Set<OgcApiMediaType> mediaTypes) {
        return mediaTypes.stream()
                         .filter(mediaType -> !Objects.equals(mediaType, selectedMediaType))
                         .collect(ImmutableSet.toImmutableSet());
    }

    private Optional<OgcApiEndpointExtension> findEndpoint(OgcApiApiDataV2 dataset,
                                                           @PathParam("entrypoint") String entrypoint,
                                                           String subPath, String method) {
        if (method=="OPTIONS") {
            // special treatment for OPTIONS
            // check that the resource exists and in that case use the general endpoint for all OPTIONS requests
            boolean resourceExists = getEndpoints().stream()
                    .filter(endpoint -> endpoint.isEnabledForApi(dataset))
                    .anyMatch(endpoint -> {
                        OgcApiEndpointDefinition apiDef = endpoint.getDefinition(dataset);
                        if (apiDef.getSortPriority()!=SORT_PRIORITY_DUMMY)
                            return apiDef.matches("/"+entrypoint+subPath, null);
                        return false;
                    });
            if (!resourceExists)
                throw new NotFoundException();

            return getEndpoints().stream()
                    .filter(endpoint -> endpoint.getClass()==OgcApiOptionsEndpoint.class)
                    .findAny();
        }

        return getEndpoints().stream()
                             .filter(endpoint -> {
                                 OgcApiEndpointDefinition apiDef = endpoint.getDefinition(dataset);
                                 if (apiDef!=null && apiDef.getSortPriority()!=SORT_PRIORITY_DUMMY)
                                     return apiDef.matches("/"+entrypoint+subPath, method);
                                 return false;
                             })
                             .filter(endpoint -> endpoint.isEnabledForApi(dataset))
                             .findFirst();
    }

    private Set<String> getMethods(OgcApiApiDataV2 dataset,
                                     @PathParam("entrypoint") String entrypoint,
                                     String subPath) {
        return getEndpoints().stream()
                .map(endpoint -> {
                    OgcApiEndpointDefinition apiDef = endpoint.getDefinition(dataset);
                    if (!apiDef.getResources().isEmpty()) {
                        Optional<OgcApiResource> resource = apiDef.getResource("/" + entrypoint + subPath);
                        if (resource.isPresent())
                            return resource.get().getOperations().keySet();
                        return null;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
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
