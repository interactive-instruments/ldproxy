/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.common.app.ImmutableDefinition;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl;
import de.ii.ogcapi.common.app.QueriesHandlerCommonImpl.Query;
import de.ii.ogcapi.common.domain.ApiDefinitionFormatExtension;
import de.ii.ogcapi.common.domain.CommonConfiguration;
import de.ii.ogcapi.common.domain.QueriesHandlerCommon;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import io.dropwizard.auth.Auth;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API Definition
 * @path /{apiId}/api
 * @formats {@link de.ii.ogcapi.common.domain.ApiDefinitionFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointDefinition extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointDefinition.class);

    private final QueriesHandlerCommon queryHandler;

    @Inject
    public EndpointDefinition(ExtensionRegistry extensionRegistry,
                              QueriesHandlerCommon queryHandler) {
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
