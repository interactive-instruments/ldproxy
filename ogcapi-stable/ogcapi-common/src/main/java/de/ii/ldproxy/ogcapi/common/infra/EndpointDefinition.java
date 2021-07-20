/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.common.app.ImmutableDefinition;
import de.ii.ldproxy.ogcapi.common.domain.QueriesHandlerCommon;
import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ldproxy.ogcapi.common.app.QueriesHandlerCommonImpl.Query;
import de.ii.ldproxy.ogcapi.common.domain.ApiDefinitionFormatExtension;
import de.ii.ldproxy.ogcapi.common.domain.CommonConfiguration;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.auth.domain.User;
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

@Component
@Provides
@Instantiate
public class EndpointDefinition extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointDefinition.class);

    private final QueriesHandlerCommon queryHandler;

    public EndpointDefinition(@Requires ExtensionRegistry extensionRegistry,
                              @Requires QueriesHandlerCommon queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CommonConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(ApiDefinitionFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("api")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_API_DEFINITION);
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, "/api");
        String operationSummary = "API definition";
        String path = "/api";
        ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                .path(path);
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, Optional.empty(), ImmutableList.of());
        if (operation!=null)
            resourceBuilder.putOperations("GET", operation);
        definitionBuilder.putResources(path, resourceBuilder.build());
        operationSummary = "support files for the API definition in HTML";
        List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, "/api/{resource}");
        path = "/api/{resource}";
        resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                .path(path)
                .pathParameters(pathParameters);
        operation = addOperation(apiData, queryParameters, path, operationSummary, Optional.empty(), ImmutableList.of());
        if (operation!=null)
            resourceBuilder.putOperations("GET", operation);
        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    @GET
    @Path("/")
    public Response getApiDefinition(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                     @Context ApiRequestContext ogcApiContext) {

        QueriesHandlerCommonImpl.Definition queryInput = new ImmutableDefinition.Builder()
                .build();

        return queryHandler.handle(Query.API_DEFINITION, queryInput, ogcApiContext);
    }

    @GET
    @Path("/{file}")
    public Response getApiDefinition(@Auth Optional<User> optionalUser, @Context OgcApi api,
                                     @Context ApiRequestContext ogcApiContext, @PathParam("file") Optional<String> file) {

        QueriesHandlerCommonImpl.Definition queryInputApiDefinition = new ImmutableDefinition.Builder()
                .from(getGenericQueryInput(api.getData()))
                .subPath(file)
                .build();

        return queryHandler.handle(Query.API_DEFINITION, queryInputApiDefinition, ogcApiContext);
    }
}
