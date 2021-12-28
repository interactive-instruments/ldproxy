/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableQueryInputRoute;
import de.ii.ldproxy.ogcapi.routes.domain.QueryHandlerRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
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

/**
 * fetch the definition of a route
 */
@Component
@Provides
@Instantiate
public class EndpointRouteDefinition extends Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointRouteDefinition.class);

    protected static final List<String> TAGS = ImmutableList.of("Routing");

    private final QueryHandlerRoutes queryHandler;

    public EndpointRouteDefinition(@Requires ExtensionRegistry extensionRegistry,
                                   @Requires QueryHandlerRoutes queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(RoutingConfiguration.class)
            .filter(RoutingConfiguration::getEnabled)
            .filter(RoutingConfiguration::getManageRoutes)
            .isPresent();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(RouteDefinitionFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("routes")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_ROUTE_DEFINITION);
        String path = "/routes/{routeId}/definition";
        ImmutableList<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
        List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        if (pathParameters.stream().noneMatch(param -> param.getName().equals("routeId"))) {
            LOGGER.error("Path parameter 'routeId' missing for resource at path '" + path + "'. The GET method will not be available.");
        } else {
            String operationSummary = "fetch the definition of route `routeId`";
            Optional<String> operationDescription = Optional.of("This operation returns the parameters used to create the route with id `routeId`.");
            ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                    .path(path)
                    .pathParameters(pathParameters);
            ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilder.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilder.build());
        }

        return definitionBuilder.build();
    }

    /**
     * Fetch the definition of a route
     *
     * @param routeId the local identifier of route
     * @return the style in a json file
     */
    @Path("/{routeId}/definition")
    @GET
    public Response getRouteDefinition(@Auth Optional<User> optionalUser,
                                       @PathParam("routeId") String routeId,
                                       @Context OgcApi api,
                                       @Context ApiRequestContext requestContext) {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);
        checkPathParameter(extensionRegistry, apiData, "/routes/{routeId}/definition", "routeId", routeId);

        QueryHandlerRoutes.QueryInputRoute queryInput = new ImmutableQueryInputRoute.Builder()
                .from(getGenericQueryInput(api.getData()))
                .routeId(routeId)
                .build();

        return queryHandler.handle(QueryHandlerRoutes.Query.GET_ROUTE_DEFINITION, queryInput, requestContext);
    }
}
