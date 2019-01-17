/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.server.CoreServerConfig;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.entity.api.EntityRepository;
import de.ii.xtraplatform.service.api.Service;
import de.ii.xtraplatform.service.api.ServiceData;
import de.ii.xtraplatform.service.api.ServiceDataWithStatus;
import de.ii.xtraplatform.service.api.ServiceResource;
import io.dropwizard.auth.Auth;
import io.dropwizard.views.ViewRenderer;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = ServiceResource.SERVICE_TYPE_KEY, type = "java.lang.String", value = "WFS3")
})
@Instantiate

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@PermitAll //TODO ???
@Produces(MediaType.WILDCARD)
public class Wfs3ServiceResource implements ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3ServiceResource.class);

    private Set<Wfs3MediaType> supportedMediaTypes;

    private Optional<URI> externalUri = Optional.empty();
    private List<Wfs3EndpointExtension> wfs3Endpoints;

    @Requires
    private Wfs3RequestInjectableContext wfs3RequestContext;


    Wfs3ServiceResource(@Requires Wfs3ExtensionRegistry wfs3ConformanceClassRegistry) {
        this.supportedMediaTypes = wfs3ConformanceClassRegistry.getOutputFormats()
                                                               .keySet();
        this.wfs3Endpoints = wfs3ConformanceClassRegistry.getEndpoints();
    }

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
    public ServiceData getService() {
        return null;
    }

    @Override
    public void setService(ServiceData service) {

    }

    @Override
    public void init(ObjectMapper defaultObjectMapper, EntityRepository entityRepository, AuthorizationProvider permProvider) {

    }

    @Override
    public void setMustacheRenderer(ViewRenderer mustacheRenderer) {

    }

    @GET
    public Response getDataset(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        checkAuthorization((Wfs3Service) service, optionalUser);

        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getDatasetResponse(wfs3Request, false);
    }

    @Path("/conformance")
    @GET
    public Response getConformanceClasses(@Auth Optional<User> optionalUser, @Context Service service, @Context Wfs3RequestContext wfs3Request) {
        //Wfs3Request wfs3Request = new Wfs3Request(uriInfo.getRequestUri(), externalUri, httpHeaders);

        return ((Wfs3Service) service).getConformanceResponse(wfs3Request);
    }

    @Path("/{path}")
    public Wfs3EndpointExtension dispatch(@PathParam("path") String path, @Context Service service, @Context HttpHeaders httpHeaders, @Context ContainerRequestContext request) {

        Wfs3Service wfs3Service = (Wfs3Service) service;
        String subPath = ((UriRoutingContext) request.getUriInfo()).getFinalMatchingGroup();

        return wfs3Endpoints.stream()
                            .filter(wfs3Endpoint -> wfs3Endpoint.matches(path, request.getMethod(), subPath))
                            .filter(wfs3Endpoint -> wfs3Endpoint.isEnabledForService(wfs3Service.getData()))
                            .findFirst()
                            .orElseThrow(() -> new NotFoundException());
    }


    private void checkAuthorization(Wfs3Service service, Optional<User> optionalUser) {
        if (service.getData()
                   .isSecured() && !optionalUser.isPresent()) {
            throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
            //throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
    }
}
