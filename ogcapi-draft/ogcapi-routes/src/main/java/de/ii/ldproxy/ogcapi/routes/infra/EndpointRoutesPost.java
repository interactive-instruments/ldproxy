/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlForm;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlFormDefaults;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableQueryInputComputeRoute;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableQueryInputRoute;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableRouteDefinitionInputs;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableWaypoints;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableWaypointsValue;
import de.ii.ldproxy.ogcapi.routes.domain.QueryHandlerRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.RouteFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RouteRepository;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesLinksGenerator;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.ldproxy.ogcapi.routes.domain.WaypointsValue;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.CORE;
import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.MODE;

/**
 * computes routes
 */
@Component
@Provides
@Instantiate
public class EndpointRoutesPost extends Endpoint implements ConformanceClass {

    public static final ApiMediaType REQUEST_MEDIA_TYPE = new ImmutableApiMediaType.Builder()
        .type(MediaType.APPLICATION_JSON_TYPE)
        .label("JSON")
        .parameter("json")
        .build();

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointRoutesPost.class);
    private static final List<String> TAGS = ImmutableList.of("Routing");

    private final QueryHandlerRoutes queryHandler;
    private final Schema schemaRouteDefinition;
    private final FeaturesQuery ogcApiFeaturesQuery;
    private final FeaturesCoreProviders providers;
    private ObjectMapper mapper;
    private final RouteRepository routeRepository;

    public EndpointRoutesPost(@Requires ExtensionRegistry extensionRegistry,
                              @Requires QueryHandlerRoutes queryHandler,
                              @Requires SchemaGenerator schemaGenerator,
                              @Requires FeaturesQuery ogcApiFeaturesQuery,
                              @Requires FeaturesCoreProviders providers,
                              @Requires RouteRepository routeRepository) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.schemaRouteDefinition = schemaGenerator.getSchema(RouteDefinition.class);
        this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
        this.providers = providers;
        this.routeRepository = routeRepository;
        this.mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of(CORE, MODE);
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
            formats = extensionRegistry.getExtensionsForType(RouteFormatExtension.class);
        return formats;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, ValidationResult.MODE apiValidation) {
        ValidationResult result = super.onStartup(apiData, apiValidation);

        if (apiValidation== ValidationResult.MODE.NONE)
            return result;

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
            .from(result)
            .mode(apiValidation);

        builder = routeRepository.validate(builder, apiData);

        return builder.build();
    }

    private Map<MediaType, ApiMediaTypeContent> getRequestContent(Optional<RoutingConfiguration> config) {
        List<Example> examples = ImmutableList.of();;
        Optional<HtmlFormDefaults> defaults = config.map(RoutingConfiguration::getHtml)
            .flatMap(HtmlForm::getDefaults);
        if (defaults.isPresent()) {
            WaypointsValue waypoints = new ImmutableWaypointsValue.Builder().addCoordinates(defaults.get().getStart(), defaults.get().getEnd()).build();
            ImmutableRouteDefinitionInputs.Builder builder = new ImmutableRouteDefinitionInputs.Builder();
            config.map(RoutingConfiguration::getDefaultPreference).ifPresent(builder::preference);
            builder.waypoints(new ImmutableWaypoints.Builder().value(waypoints).build());
            defaults.get().getName().ifPresent(builder::name);
            config.map(RoutingConfiguration::getAdditionalFlags)
                .orElse(ImmutableMap.of())
                .entrySet()
                .stream()
                .filter(f -> f.getValue().getDefault())
                .map(Map.Entry::getKey)
                .forEach(builder::addAdditionalFlags);
            examples = ImmutableList.of(new ImmutableExample.Builder()
                                            .value(mapper.valueToTree(new ImmutableRouteDefinition.Builder()
                                                                          .inputs(builder.build())
                                                                          .build()))
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
        Optional<RoutesConfiguration> routesConfig = providers.getFeatureProviderOrThrow(apiData).getData().getExtension(RoutesConfiguration.class);
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("routes")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_ROUTES_POST);
        String path = "/routes";
        HttpMethods method = HttpMethods.POST;
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, method);
        List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, method);
        String operationSummary = "compute a route";
        String description = String.format("This creates a new route. The payload of the request specifies the definition of the new route.\n\n" +
            (config.map(RoutingConfiguration::getIntermediateWaypoints).orElse(false)
                ? "A route is defined by two or more `waypoints`, which will be visited in the order in which they are provided. "
                : "A route is defined by two `waypoints`, the start and end location of the route. ") +
            "If the coordinates are in a coordinate reference system that is not WGS 84 longitude/latitude, " +
            "the URI of the coordinate reference system must be provided in a member `coordRefSys` in the waypoints object.\n\n" +
            "A preference how the route should be optimized can be specified (`preference`). The default value is `%s`. Valid values are:\n\n" +
            String.join("", routesConfig
                .map(RoutesConfiguration::getPreferences)
                .map(Map::entrySet)
                .orElseThrow()
                .stream()
                .map(entry -> String.format("* `%s`: %s\n", entry.getKey(), entry.getValue().getLabel()))
                .collect(Collectors.toUnmodifiableList())) +
            "\n" +
            "The mode of transportation can be specified (`mode`). The default value is `%s`. Valid values are:\n\n" +
            String.join("", routesConfig
                .map(RoutesConfiguration::getModes)
                .map(Map::entrySet)
                .orElseThrow()
                .stream()
                .map(entry -> String.format("* `%s`: %s\n", entry.getKey(), entry.getValue()))
                .collect(Collectors.toUnmodifiableList())) +
            "\n" +
            "In addition, some flags can be provided in `additionalFlags` to alter the computation of the route:\n\n" +
            String.join("", config
                .map(RoutingConfiguration::getAdditionalFlags)
                .map(Map::entrySet)
                .orElseThrow()
                .stream()
                .map(entry -> String.format("* `%s`: %s\n", entry.getKey(), entry.getValue().getLabel()))
                .collect(Collectors.toUnmodifiableList())) +
            "\n" +
            "An optional `name` for the route may be provided.\n\n" +
            (config.map(RoutingConfiguration::getHeightRestrictions).orElse(false) ? "An optional vehicle height in meter may be provided (`height_m`).\n\n" : "") +
            (config.map(RoutingConfiguration::getWeightRestrictions).orElse(false) ? "An optional vehicle weight in metric tons may be provided (`weight_t`).\n\n" : "") +
            (config.map(RoutingConfiguration::getObstacles).orElse(false) ? "An optional multi-polygon geometry of areas that should be avoided may be provided (`obstacles`).\n\n" : ""),
                                           config.map(RoutingConfiguration::getDefaultPreference).orElseThrow(),
                                           config.map(RoutingConfiguration::getDefaultMode).orElseThrow());
        Optional<String> operationDescription = Optional.of(description);
        ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
            .path(path);
        Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(config);
        Map<MediaType, ApiMediaTypeContent> responseContent = getContent(apiData, "/routes", method);
        ApiOperation operation = addOperation(apiData, method, OPERATION_TYPE.PROCESS, requestContent, responseContent, queryParameters, headers, path, operationSummary, operationDescription, Optional.empty(), TAGS);
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
    @POST
    @SuppressWarnings("UnstableApiUsage")
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

        RouteDefinition definition;
        try {
            // parse input
            definition = mapper.readValue(requestBody, RouteDefinition.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("The content of the route definition is invalid: %s", e.getMessage()), e);
        }

        String routeId = Hashing.murmur3_128()
            .newHasher()
            .putObject(definition, RouteDefinition.FUNNEL)
            .putString(Optional.ofNullable(request.getHeader("crs")).orElse(defaultCrs.toUriString()), StandardCharsets.UTF_8)
            .hash()
            .toString();

        if (apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getManageRoutes)
            .orElse(false)) {
            if (routeRepository.routeExists(apiData, routeId)) {
                // If the same route is already stored, just return the stored route
                QueryHandlerRoutes.QueryInputRoute queryInput = new ImmutableQueryInputRoute.Builder()
                    .routeId(routeId)
                    .build();
                return queryHandler.handle(QueryHandlerRoutes.Query.GET_ROUTE, queryInput, requestContext);
            }
        }

        QueryHandlerRoutes.QueryInputComputeRoute queryInput = new ImmutableQueryInputComputeRoute.Builder()
            .definition(definition)
            .routeId(routeId)
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
