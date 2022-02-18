/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.infra.rest;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.EndpointExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class OptionsEndpoint implements EndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionsEndpoint.class);

    private final ExtensionRegistry extensionRegistry;

    @Inject
    public OptionsEndpoint(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public ImmutableSet<ApiMediaType> getMediaTypes(OgcApiDataV2 dataset, String subPath, String method) {
        return ImmutableSet.<ApiMediaType>builder().add(new ImmutableApiMediaType.Builder()
                .type(MediaType.TEXT_PLAIN_TYPE)
                .build()).build();
    }

    @OPTIONS
    @Path("")
    public Response getOptions(@Auth Optional<User> optionalUser,
                               @Context OgcApi api,
                               @Context ContainerRequestContext requestContext) {

        String path = requestContext.getUriInfo().getPath();
        int index = path.indexOf("/"+api.getId())+api.getId().length()+2;
        String entrypoint = index>path.length() ? "" : path.substring(index);

        return getOptions(api.getData(),entrypoint,"");
    }

    @OPTIONS
    @Path("/{subPath:.*}")
    public Response getOptions(@Auth Optional<User> optionalUser,
                               @Context OgcApi api,
                               @Context ContainerRequestContext requestContext,
                               @PathParam("subPath") String subPath) {

        String path = requestContext.getUriInfo().getPath();
        int index = path.indexOf("/"+api.getId())+api.getId().length()+2;
        String[] pathElements = path.substring(index).split("/",2);
        String entrypoint = pathElements[0];

        return getOptions(api.getData(),entrypoint,"/"+subPath);
    }

    private Response getOptions(OgcApiDataV2 apiData, String entrypoint, String subPath) {
        // special treatment for OPTIONS requests. We loop over the endpoints and determine
        // which methods are supported.
        Set<String> supportedMethods = Arrays.stream(HttpMethods.values())
                .filter(method -> !method.equals(HttpMethods.OPTIONS))
                .filter(otherMethod -> findEndpoint(
                        apiData,
                        entrypoint,
                        subPath,
                        otherMethod.toString())
                        .isPresent())
                .map(method -> method.toString())
                .collect(Collectors.toSet());

        if (supportedMethods.isEmpty())
            throw new NotFoundException("The requested path is not a resource in this API.");

        // add OPTIONS since this is supported for all paths
        supportedMethods.add("OPTIONS");

        return Response
                .ok(String.join(", ", supportedMethods))
                .allow(supportedMethods)
                // TODO add variants
                .header("Access-Control-Allow-Origin","*") // TODO * not allowed with credentials
                .header("Access-Control-Allow-Credentials","true")
                .header("Access-Control-Allow-Methods", String.join(", ", supportedMethods))
                .header("Access-Control-Allow-Headers", "Accept, Accept-Language, Origin, Content-Type, Content-Language, Content-Crs, Authorization")
                .build();
    }

    private Optional<EndpointExtension> findEndpoint(OgcApiDataV2 apiData,
                                                     String entrypoint,
                                                     String subPath,
                                                     String method) {
        return getEndpoints().stream()
                .filter(endpoint -> endpoint.getDefinition(apiData).matches("/"+entrypoint+subPath, method))
                .filter(endpoint -> endpoint.isEnabledForApi(apiData))
                .findFirst();
    }

    private List<EndpointExtension> getEndpoints() {
        return extensionRegistry.getExtensionsForType(EndpointExtension.class);
    }
}
