/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.ImmutableOgcApiQueryInputApiDefinition;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon.Query;
import de.ii.ldproxy.ogcapi.application.OgcApiQueriesHandlerCommon.OgcApiQueryInputApiDefinition;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.auth.api.User;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class OgcApiEndpointApiDefinition implements OgcApiEndpointExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointApiDefinition.class);

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("api")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^(?:/[^/]*)?$")
            .build();

    private final OgcApiExtensionRegistry extensionRegistry;

    @Requires
    private OgcApiQueriesHandlerCommon queryHandler;

    public OgcApiEndpointApiDefinition(@Requires OgcApiExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        return extensionRegistry.getExtensionsForType(ApiDefinitionFormatExtension.class)
                                .stream()
                                .filter(formatExtension -> ("/api"+subPath).matches(formatExtension.getPathPattern()))
                                .map(ApiDefinitionFormatExtension::getMediaType)
                                .collect(ImmutableSet.toImmutableSet());
    }

    @GET
    @Path("/")
    public Response getApiDefinition(@Auth Optional<User> optionalUser, @Context OgcApiApi api,
                                     @Context OgcApiRequestContext ogcApiContext) {

        OgcApiQueryInputApiDefinition queryInput = new ImmutableOgcApiQueryInputApiDefinition.Builder()
                .build();

        return queryHandler.handle(Query.API_DEFINITION, queryInput, ogcApiContext);
    }

    @GET
    @Path("/{file}")
    public Response getApiDefinition(@Auth Optional<User> optionalUser, @Context OgcApiApi api,
                                          @Context OgcApiRequestContext ogcApiContext, @PathParam("file") Optional<String> file) {

        OgcApiQueryInputApiDefinition queryInputApiDefinition = new ImmutableOgcApiQueryInputApiDefinition.Builder()
                .subPath(file)
                .build();

        return queryHandler.handle(Query.API_DEFINITION, queryInputApiDefinition, ogcApiContext);
    }
}
