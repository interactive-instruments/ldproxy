/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.infra.rest;

import com.google.common.collect.ImmutableList;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Provides
@Instantiate
public class OgcApiEndpointApiDefinition extends OgcApiEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiEndpointApiDefinition.class);

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("api")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^(?:/[^/]*)?$")
            .build();

    @Requires
    private OgcApiQueriesHandlerCommon queryHandler;

    public OgcApiEndpointApiDefinition(@Requires OgcApiExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiCommonConfiguration.class);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ApiDefinitionFormatExtension.class);
        return formats;
    }

    /*
    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        return extensionRegistry.getExtensionsForType(ApiDefinitionFormatExtension.class)
                                .stream()
                                .filter(formatExtension -> ("/api"+subPath).matches(formatExtension.getPathPattern()))
                                .map(ApiDefinitionFormatExtension::getMediaType)
                                .collect(ImmutableSet.toImmutableSet());
    }
     */

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("api")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_API_DEFINITION);
            Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/api");
            String operationSummary = "API definition";
            String path = "/api";
            ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                    .path(path);
            OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, Optional.empty(), ImmutableList.of());
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());
            operationSummary = "support files for the API definition in HTML";
            Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, "/api/{resource}");
            path = "/api/{resource}";
            resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                    .path(path)
                    .pathParameters(pathParameters);
            operation = addOperation(apiData, queryParameters, path, operationSummary, Optional.empty(), ImmutableList.of());
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
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
