/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.infra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlForm;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlFormDefaults;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableQueryInputComputeRoute;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableQueryInputRouteDefinitionForm;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinitionInfo;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinitionWrapper;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableWaypointsWrapper;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableWaypoints;
import de.ii.ldproxy.ogcapi.routes.domain.QueryHandlerRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionWrapper;
import de.ii.ldproxy.ogcapi.routes.domain.RouteFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.ldproxy.ogcapi.routes.domain.Waypoints;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.CORE;

/**
 * computes routes
 */
@Component
@Provides
@Instantiate
public class EndpointRoutes extends Endpoint implements ConformanceClass {

    public static final ApiMediaType REQUEST_MEDIA_TYPE = new ImmutableApiMediaType.Builder()
        .type(MediaType.APPLICATION_JSON_TYPE)
        .label("JSON")
        .parameter("json")
        .build();

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointRoutes.class);
    private static final List<String> TAGS = ImmutableList.of("Compute routes");

    private final QueryHandlerRoutes queryHandler;
    private final Schema schemaRouteDefinition;
    private final FeaturesQuery ogcApiFeaturesQuery;
    private final FeaturesCoreProviders providers;

    public EndpointRoutes(@Requires ExtensionRegistry extensionRegistry,
                          @Requires QueryHandlerRoutes queryHandler,
                          @Requires SchemaGenerator schemaGenerator,
                          @Requires FeaturesQuery ogcApiFeaturesQuery,
                          @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.schemaRouteDefinition = schemaGenerator.getSchema(RouteDefinitionWrapper.class);
        this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
        this.providers = providers;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of(CORE);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<RoutingConfiguration> extension = apiData.getExtension(RoutingConfiguration.class);

        return extension.filter(RoutingConfiguration::isEnabled)
            .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = ImmutableList.<FormatExtension>builder()
                .addAll(extensionRegistry.getExtensionsForType(RouteFormatExtension.class))
                .addAll(extensionRegistry.getExtensionsForType(RoutesFormatExtension.class)).build();
        return formats;
    }

    private Map<MediaType, ApiMediaTypeContent> getRequestContent(Optional<RoutingConfiguration> config) {
        List<Example> examples = ImmutableList.of();;
        Optional<HtmlFormDefaults> defaults = config.map(RoutingConfiguration::getHtml)
            .flatMap(HtmlForm::getDefaults);
        if (defaults.isPresent()) {
            Waypoints waypoints = new ImmutableWaypoints.Builder().addCoordinates(defaults.get().getStart(), defaults.get().getEnd()).build();
            ImmutableRouteDefinition.Builder builder = new ImmutableRouteDefinition.Builder();
            config.map(RoutingConfiguration::getDefaultPreference).ifPresent(pref -> builder.preference(pref));
            builder.waypoints(new ImmutableWaypointsWrapper.Builder().value(waypoints).build());
            defaults.get().getName().ifPresent(name -> builder.name(name));
            examples = ImmutableList.of(new ImmutableExample.Builder()
                             .value(new ImmutableRouteDefinitionWrapper.Builder()
                                        .inputs(builder.build())
                                        .build())
                             .build());
        }
        return ImmutableMap.of(REQUEST_MEDIA_TYPE.type(), new ImmutableApiMediaTypeContent.Builder()
            .ogcApiMediaType(REQUEST_MEDIA_TYPE)
            .schema(schemaRouteDefinition)
            .schemaRef("#/components/schemas/RouteDefinition")
            .examples(examples)
            .build());
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        Optional<RoutingConfiguration> config = apiData.getExtension(RoutingConfiguration.class);
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("routes")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_ROUTES);
        String path = "/routes";
        HttpMethods method = HttpMethods.POST;
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
        List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, method);
        String operationSummary = "compute a route";
        String description = "This creates a new route. The payload of the request specifies the definition of the new route. \n" +
            "\n" +
            "At a minimum, a route is defined by two `waypoints`, the start and end point of the route.\n" +
            "\n" +
            "A preference for the route... TODO.\n" +
            "\n" +
            "An optional `name` for the route may be provided.\n" +
            "\n" +
            "More parameters and routing constraints can optionally be provided with the routing request:\n" +
            "* TODO."; // TODO generate from config, e.g.
        Optional<String> operationDescription = Optional.of(description);
        ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
            .path(path);
        Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(config);
        Map<MediaType, ApiMediaTypeContent> responseContent = getContent(apiData, "/routes", method);
        ApiOperation operation = addOperation(apiData, method, OPERATION_TYPE.PROCESS, requestContent, responseContent, queryParameters, headers, path, operationSummary, operationDescription, Optional.empty(), TAGS);
        if (operation!=null)
            resourceBuilder.putOperations(method.toString(), operation);

        // add HTML form
        method = HttpMethods.GET;
        queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
        headers = getHeaders(extensionRegistry, apiData, path, method);
        operationSummary = "provide a template for a route definition";
        description = "TODO"; // TODO
        operationDescription = Optional.of(description);
        responseContent = getContent(apiData, "/routes", method);
        operation = addOperation(apiData, method, responseContent, queryParameters, headers, path, operationSummary, operationDescription, Optional.empty(), TAGS);
        if (operation!=null)
            resourceBuilder.putOperations(method.toString(), operation);

        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    /**
     * creates a new route
     *
     * @return a route according to the RouteExchangeModel
     */
    @Path("/")
    @GET
    public Response getForm(@Auth Optional<User> optionalUser,
                            @Context OgcApi api,
                            @Context ApiRequestContext requestContext,
                            @Context UriInfo uriInfo,
                            @Context HttpServletRequest request) {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);

        Map<String, String> preferences = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getPreferences)
            .map(map -> map.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<String, String>(entry.getKey(), entry.getValue().getLabel()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(ImmutableMap.of());

        Map<String, String> additionalFlags = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getAdditionalFlags)
            .map(map -> map.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<String, String>(entry.getKey(), entry.getValue().getLabel()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .orElse(ImmutableMap.of());

        ImmutableMap<String, String> crsList = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getHtml)
            .map(HtmlForm::getCrs)
            .orElse(ImmutableMap.of())
            .entrySet()
            .stream()
            .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getValue().toUriString(), entry.getKey()))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        QueryHandlerRoutes.QueryInputRouteDefinitionForm queryInput = new ImmutableQueryInputRouteDefinitionForm.Builder()
            .templateInfo(new ImmutableRouteDefinitionInfo.Builder()
                              .preferences(preferences)
                              .defaultPreference(apiData.getExtension(RoutingConfiguration.class)
                                                     .map(RoutingConfiguration::getDefaultPreference)
                                                     .orElse(preferences.keySet()
                                                                 .stream()
                                                                 .findFirst()
                                                                 .orElseThrow()))
                              .additionalFlags(additionalFlags)
                              .crs(crsList)
                              .build())
            .build();
        return queryHandler.handle(QueryHandlerRoutes.Query.ROUTE_DEFINITION_FORM, queryInput, requestContext);
    }

    /**
     * creates a new route
     *
     * @return a route according to the RouteExchangeModel
     */
    @Path("/")
    @POST
    public Response computeRoute(@Auth Optional<User> optionalUser,
                                 @Context OgcApi api,
                                 @Context ApiRequestContext requestContext,
                                 @Context UriInfo uriInfo,
                                 @Context HttpServletRequest request,
                                 byte[] requestBody) {

        OgcApiDataV2 apiData = api.getData();
        checkAuthorization(apiData, optionalUser);

        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(api.getData());
        ensureFeatureProviderSupportsRouting(featureProvider);

        String featureTypeId = api.getData()
            .getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getFeatureType)
            .orElseThrow(() -> new IllegalStateException("No feature type has been configured for routing."));

        EpsgCrs defaultCrs = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getDefaultEpsgCrs)
            .orElse(OgcCrs.CRS84);
        Map<String, Integer> coordinatePrecision = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getCoordinatePrecision)
            .orElse(ImmutableMap.of());
        String speedLimitUnit = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getSpeedLimitUnit)
            .orElse("kmph");
        List<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), "/routes", HttpMethods.POST);
        FeatureQuery query = ogcApiFeaturesQuery.requestToBareFeatureQuery(api.getData(), featureTypeId, defaultCrs, coordinatePrecision, 1, Integer.MAX_VALUE, Integer.MAX_VALUE, toFlatMap(uriInfo.getQueryParameters()), allowedParameters);
        QueryHandlerRoutes.QueryInputComputeRoute queryInput = new ImmutableQueryInputComputeRoute.Builder()
            .contentType(mediaTypeFromString(request.getContentType()))
            .requestBody(requestBody)
            .featureProvider(featureProvider)
            .featureTypeId(featureTypeId)
            .query(query)
            .crs(Optional.ofNullable(request.getHeader("crs")))
            .defaultCrs(defaultCrs)
            .speedLimitUnit(speedLimitUnit)
            .build();
        return queryHandler.handle(QueryHandlerRoutes.Query.COMPUTE_ROUTE, queryInput, requestContext);
    }

    private static void ensureFeatureProviderSupportsRouting(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("Feature provider does not support queries.");
        }
        featureProvider.getData().getExtension(RoutesConfiguration.class)
            .orElseThrow(() -> new IllegalStateException("Feature provider does not support routing."));
    }
}
