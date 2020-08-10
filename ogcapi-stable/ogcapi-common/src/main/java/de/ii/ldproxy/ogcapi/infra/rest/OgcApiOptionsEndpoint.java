/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class OgcApiOptionsEndpoint implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiOptionsEndpoint.class);

    private final OgcApiExtensionRegistry extensionRegistry;

    @Requires
    private OgcApiQueriesHandlerCommon queryHandler;

    public OgcApiOptionsEndpoint(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath, String method) {
        return ImmutableSet.<OgcApiMediaType>builder().add(new ImmutableOgcApiMediaType.Builder()
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build()).build();
    }

    @OPTIONS
    @Path("")
    public Response getOptions(@Auth Optional<User> optionalUser,
                               @Context OgcApiApi api,
                               @Context ContainerRequestContext requestContext) {

        String path = requestContext.getUriInfo().getPath();
        int index = path.indexOf("/"+api.getId())+api.getId().length()+2;
        String entrypoint = index>path.length() ? "" : path.substring(index);

        return getOptions(api.getData(),entrypoint,"");
    }

    @OPTIONS
    @Path("/{subPath:.*}")
    public Response getOptions(@Auth Optional<User> optionalUser,
                               @Context OgcApiApi api,
                               @Context ContainerRequestContext requestContext,
                               @PathParam("subPath") String subPath) {

        String path = requestContext.getUriInfo().getPath();
        int index = path.indexOf("/"+api.getId())+api.getId().length()+2;
        String[] pathElements = path.substring(index).split("/",2);
        String entrypoint = pathElements[0];

        return getOptions(api.getData(),entrypoint,"/"+subPath);
    }

    private Response getOptions(OgcApiApiDataV2 apiData, String entrypoint, String subPath) {
        // special treatment for OPTIONS requests. We loop over the endpoints and determine
        // which methods are supported.
        Set<String> supportedMethods = Arrays.stream(OgcApiContext.HttpMethods.values())
                .filter(method -> !method.equals(OgcApiContext.HttpMethods.OPTIONS))
                .filter(otherMethod -> findEndpoint(
                        apiData,
                        entrypoint,
                        subPath,
                        otherMethod.toString())
                        .isPresent())
                .map(method -> method.toString())
                .collect(Collectors.toSet());

        if (supportedMethods.isEmpty())
            throw(new NotFoundException());

        // add OPTIONS since this is supported for all paths
        supportedMethods.add("OPTIONS");

        return Response
                .ok(String.join(", ", supportedMethods))
                .allow(supportedMethods)
                .header("Access-Control-Allow-Origin","*") // TODO * not allowed with credentials
                .header("Access-Control-Allow-Credentials","true")
                .header("Access-Control-Allow-Methods", String.join(", ", supportedMethods))
                .header("Access-Control-Allow-Headers", "Accept, Accept-Language, Origin, Content-Type, Content-Language, Content-Crs, Authorization")
                .build();
    }

    private Optional<OgcApiEndpointExtension> findEndpoint(OgcApiApiDataV2 apiData,
                                                           String entrypoint,
                                                           String subPath,
                                                           String method) {
        return getEndpoints().stream()
                .filter(endpoint -> endpoint.getDefinition(apiData).matches("/"+entrypoint+subPath, method))
                .filter(endpoint -> endpoint.isEnabledForApi(apiData))
                .findFirst();
    }

    private List<OgcApiEndpointExtension> getEndpoints() {
        return extensionRegistry.getExtensionsForType(OgcApiEndpointExtension.class);
    }


}
