/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.crs.domain.CrsSupport;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.routes.app.json.RouteDefinitionFormatJson;
import de.ii.ogcapi.routes.domain.FeatureTransformationContextRoutes;
import de.ii.ogcapi.routes.domain.ImmutableFeatureTransformationContextRoutes;
import de.ii.ogcapi.routes.domain.ImmutableRoutes;
import de.ii.ogcapi.routes.domain.QueryHandlerRoutes;
import de.ii.ogcapi.routes.domain.Route;
import de.ii.ogcapi.routes.domain.RouteDefinition;
import de.ii.ogcapi.routes.domain.RouteDefinitionFormatExtension;
import de.ii.ogcapi.routes.domain.RouteDefinitionInputs;
import de.ii.ogcapi.routes.domain.RouteFormatExtension;
import de.ii.ogcapi.routes.domain.RouteRepository;
import de.ii.ogcapi.routes.domain.Routes;
import de.ii.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ogcapi.routes.domain.RoutesLinksGenerator;
import de.ii.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.ogcapi.routes.domain.RoutingFlag;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.routes.sql.domain.ImmutableRouteQuery;
import de.ii.xtraplatform.routes.sql.domain.Preference;
import de.ii.xtraplatform.routes.sql.domain.RouteQuery;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

@Singleton
@AutoBind
public class QueryHandlerRoutesImpl implements QueryHandlerRoutes {

    private final I18n i18n;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
    private final CrsTransformerFactory crsTransformerFactory;
    private final EntityRegistry entityRegistry;
    private final CrsSupport crsSupport;
    private final RouteRepository routeRepository;

    @Inject
    public QueryHandlerRoutesImpl(I18n i18n,
                                  CrsTransformerFactory crsTransformerFactory,
                                  EntityRegistry entityRegistry,
                                  CrsSupport crsSupport,
                                  RouteRepository routeRepository) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.entityRegistry = entityRegistry;
        this.crsSupport = crsSupport;
        this.routeRepository = routeRepository;

        this.queryHandlers = ImmutableMap.of(
            Query.COMPUTE_ROUTE, QueryHandler.with(QueryInputComputeRoute.class, this::computeRoute),
            Query.GET_ROUTES, QueryHandler.with(QueryInputRoutes.class, this::getRoutes),
            Query.GET_ROUTE, QueryHandler.with(QueryInputRoute.class, this::getRoute),
            Query.GET_ROUTE_DEFINITION, QueryHandler.with(QueryInputRoute.class, this::getRouteDefinition),
            Query.DELETE_ROUTE, QueryHandler.with(QueryInputRoute.class, this::deleteRoute)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response computeRoute(QueryInputComputeRoute queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        RouteDefinition routeDefinition = queryInput.getDefinition();
        String routeId = queryInput.getRouteId();
        FeatureQuery query = queryInput.getQuery();
        FeatureProvider2 featureProvider = queryInput.getFeatureProvider();
        RoutingConfiguration config = apiData.getExtension(RoutingConfiguration.class)
            .orElseThrow(() -> new IllegalStateException("No routing configuration found for the API."));
        RoutesConfiguration providerConfig = featureProvider.getData().getExtension(RoutesConfiguration.class)
            .orElseThrow(() -> new IllegalStateException("No routing configuration found for the feature provider of this API."));

        RouteFormatExtension outputFormat = api.getOutputFormat(
                RouteFormatExtension.class,
                requestContext.getMediaType(),
                "/routes",
                Optional.empty())
            .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        RouteDefinitionInputs inputs = routeDefinition.getInputs();

        ImmutableRouteQuery.Builder routeQueryBuilder = ImmutableRouteQuery.builder()
            .start(routeDefinition.getStart())
            .end(routeDefinition.getEnd())
            .wayPoints(routeDefinition.getWaypoints());

        String preference = inputs.getPreference().orElse(config.getDefaultPreference());
        routeQueryBuilder = processPreference(preference, providerConfig.getPreferences(), routeQueryBuilder);

        String mode = inputs.getMode().orElse(config.getDefaultMode());
        routeQueryBuilder = processMode(mode, providerConfig.getModes(), routeQueryBuilder);

        ImmutableList.Builder<String> flagBuilder = new ImmutableList.Builder<>();
        for (String flag: inputs.getAdditionalFlags()) {
            flagBuilder = processFlag(flag, config.getAdditionalFlags(), flagBuilder);
        }
        routeQueryBuilder.flags(flagBuilder.build());

        if (!inputs.getWeight().isEmpty() && !config.supportsWeightRestrictions()) {
            throw new IllegalArgumentException("This API does not support weight restrictions as part of the definition of a route.");
        }
        routeQueryBuilder.weight(inputs.getWeight());

        if (!inputs.getHeight().isEmpty() && !config.supportsHeightRestrictions()) {
            throw new IllegalArgumentException("This API does not support weight restrictions as part of the definition of a route.");
        }
        routeQueryBuilder.height(inputs.getHeight());

        Optional<Geometry.MultiPolygon> obstacles = routeDefinition.getObstacles();
        if (!obstacles.isEmpty() && !config.supportsObstacles()) {
            throw new IllegalArgumentException("This API does not support obstacles as part of the definition of a route.");
        }
        routeQueryBuilder.obstacles(obstacles);

        EpsgCrs waypointCrs = routeDefinition.getWaypointsCrs();
        if (!crsSupport.isSupported(apiData, waypointCrs)) {
            throw new IllegalArgumentException(String.format("The parameter 'coordRefSys' in the route definition is invalid: the crs '%s' is not supported", waypointCrs.toUriString()));
        }

        if (!routeDefinition.getWaypoints().isEmpty() && !config.supportsIntermediateWaypoints()) {
            throw new IllegalArgumentException(String.format("This API does not support waypoints in addition to the start and end location. The following waypoints were provided: %s",
                                                             routeDefinition.getWaypoints()
                                                                 .stream()
                                                                 .map(p -> p.getCoordinates().get(0).toString())
                                                                 .collect(Collectors.joining(","))));
        }

        RouteQuery routeQuery = routeQueryBuilder
            .build();
        LOGGER.debug("Route Query: {}", routeQuery);

        query = ImmutableFeatureQuery.builder()
            .from(query)
            .addExtensions(routeQuery)
            .build();

        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs sourceCrs = null;
        EpsgCrs targetCrs = query.getCrs()
            .orElse(queryInput.getDefaultCrs());
        if (featureProvider.supportsCrs()) {
            sourceCrs = featureProvider.crs()
                .getNativeCrs();
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
        }

        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links = ImmutableList.of();
        if (config.isManageRoutesEnabled()) {
            links = new RoutesLinksGenerator().generateRouteLinks(routeId,
                                                                  inputs.getName(),
                                                                  requestContext.getUriCustomizer(),
                                                                  requestContext.getMediaType(),
                                                                  i18n,
                                                                  requestContext.getLanguage());
        }

        FeatureTransformationContextRoutes transformationContext = ImmutableFeatureTransformationContextRoutes.builder()
            .apiData(api.getData())
            .featureSchema(featureProvider.getData().getTypes().get(queryInput.getFeatureTypeId()))
            .collectionId("not_applicable")
            .ogcApiRequest(requestContext)
            .crsTransformer(crsTransformer)
            .codelists(entityRegistry.getEntitiesForType(Codelist.class)
                           .stream()
                           .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
            .defaultCrs(queryInput.getDefaultCrs())
            .sourceCrs(Optional.ofNullable(sourceCrs))
            .crs(targetCrs)
            .links(links)
            .isFeatureCollection(true)
            .isHitsOnly(query.hitsOnly())
            .isPropertyOnly(query.propertyOnly())
            .fields(query.getFields())
            .limit(query.getLimit())
            .offset(query.getOffset())
            .maxAllowableOffset(query.getMaxAllowableOffset())
            .geometryPrecision(query.getGeometryPrecision())
            .isHitsOnlyIfMore(false)
            .showsFeatureSelfLink(false)
            .name(inputs.getName())
            .format(outputFormat)
            .outputStream(new OutputStreamToByteConsumer())
            .startTimeNano(System.nanoTime())
            .speedLimitUnit(queryInput.getSpeedLimitUnit())
            .elevationProfileSimplificationTolerance(queryInput.getElevationProfileSimplificationTolerance())
            .build();

        Optional<FeatureTokenEncoder<?>> encoder = outputFormat.getFeatureEncoder(transformationContext, Optional.empty());

        if (encoder.isEmpty()) {
            throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated, because it does not support streaming.", requestContext.getMediaType().type()));
        }

        FeatureStream featureStream = featureProvider.queries().getFeatureStream(query);

        StreamingOutput streamingOutput = stream(featureStream, encoder.get());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            streamingOutput.write(baos);
        } catch (IOException e) {
            throw new IllegalStateException("Feature stream error.", e);
        }
        byte[] result = baos.toByteArray();

        Date lastModified = null;
        EntityTag etag = getEtag(result);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        // write routeDefinition and route if managing routes is enabled
        if (config.isManageRoutesEnabled()) {
            try {
                List<Link> definitionLinks = new RoutesLinksGenerator().generateRouteDefinitionLinks(routeId,
                                                                                                     inputs.getName(),
                                                                                                     requestContext.getUriCustomizer(),
                                                                                                     RouteDefinitionFormatJson.MEDIA_TYPE,
                                                                                                     i18n,
                                                                                                     requestContext.getLanguage());
                routeRepository.writeRouteAndDefinition(apiData, routeId, outputFormat, result, routeDefinition, definitionLinks);
            } catch (IOException e) {
                LOGGER.error("Could not store route in route repository.", e);
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Stacktrace: ", e);
            }
        }

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      targetCrs,
                                      true,
                                      String.format("%s.%s",
                                                    inputs.getName().orElse("Route"),
                                                    outputFormat.getMediaType().fileExtension()))
            .entity(result)
            .build();
    }

    private Response getRoutes(QueryInputRoutes queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();

        List<Link> links = ImmutableList.of();

        RoutesFormatExtension outputFormatExtension = api.getOutputFormat(RoutesFormatExtension.class,
                                                                          requestContext.getMediaType(),
                                                                          "/routes",
                                                                          Optional.empty())
            .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));

        Routes routes = new ImmutableRoutes.Builder()
            .from(routeRepository.getRoutes(apiData, requestContext))
            .templateInfo(queryInput.getTemplateInfo())
            .build();

        Object entity = outputFormatExtension.getRoutesEntity(routes,
                                                              requestContext.getApi(),
                                                              requestContext);

        Date lastModified = getLastModified(queryInput, api);
        EntityTag etag = getEtag(routes, Routes.FUNNEL, outputFormatExtension);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("route-definition-template.%s", outputFormatExtension.getMediaType().fileExtension()))
            .entity(entity)
            .build();
    }

    private Response getRoute(QueryInputRoute queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String routeId = queryInput.getRouteId();

        RouteFormatExtension format = api.getOutputFormat(RouteFormatExtension.class,
                                                                requestContext.getMediaType(),
                                                                "/routes/"+routeId,
                                                                Optional.empty())
            .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));

        Route route = routeRepository.getRoute(apiData, routeId, format);
        byte[] content = format.getRouteAsByteArray(route, apiData, requestContext);

        Date lastModified = routeRepository.getLastModified(apiData, routeId);
        EntityTag etag = getEtag(content);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? route.getLinks() : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("%s.%s", routeId, format.getFileExtension()))
            .entity(content)
            .build();
    }

    private Response getRouteDefinition(QueryInputRoute queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String routeId = queryInput.getRouteId();
        RouteDefinition routeDefinition = routeRepository.getRouteDefinition(apiData, queryInput.getRouteId());

        RouteDefinitionFormatExtension outputFormat = api.getOutputFormat(RouteDefinitionFormatExtension.class,
                                                                          requestContext.getMediaType(),
                                                                          "/routes/"+routeId+"/definition",
                                                                          Optional.empty())
            .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));

        byte[] content = outputFormat.getRouteDefinitionAsByteArray(routeDefinition, apiData, requestContext);

        Date lastModified = routeRepository.getLastModified(apiData, routeId);
        EntityTag etag = getEtag(content);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? routeDefinition.getLinks() : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null,
                                      true,
                                      String.format("%s.definition.%s", queryInput.getRouteId(), outputFormat.getMediaType().fileExtension()))
            .entity(content)
            .build();
    }

    private Response deleteRoute(QueryInputRoute queryInput, ApiRequestContext requestContext) {

        try {
            routeRepository.deleteRoute(requestContext.getApi().getData(),
                                        queryInput.getRouteId());
        } catch (IOException e) {
            throw new WebApplicationException("Could not delete the route from the store.", e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent()
            .build();
    }

    private StreamingOutput stream(FeatureStream featureTransformStream,
                                   final FeatureTokenEncoder<?> encoder) {
        return outputStream -> {
            Reactive.SinkTransformed<Object, byte[]> featureSink = encoder.to(Reactive.Sink.outputStream(outputStream));

            try {
                FeatureStream.Result result = featureTransformStream.runWith(featureSink, Optional.empty())
                    .toCompletableFuture()
                    .join();

                result.getError()
                    .ifPresent(QueriesHandler::processStreamError);

                if (result.isEmpty()) {
                    throw new NotFoundException("The requested route could not be computed.");
                }

            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error.", e.getCause());
            }
        };
    }

    private ImmutableRouteQuery.Builder processPreference(@NotNull String preference, Map<String, Preference> preferencesConfig,
                                   ImmutableRouteQuery.Builder routeQueryBuilder) {
        if (!preferencesConfig.containsKey(preference))
            throw new IllegalArgumentException(String.format("Unknown preference '%s'. Known preferences: %s", preference, String.join(", ", preferencesConfig.keySet())));
        Preference pref = preferencesConfig.get(preference);
        routeQueryBuilder
            .costColumn(pref.getCostColumn())
            .reverseCostColumn(pref.getReverseCostColumn());
        return routeQueryBuilder;
    }

    private ImmutableRouteQuery.Builder processMode(@NotNull String mode, Map<String, String> modesConfig,
                                                    ImmutableRouteQuery.Builder routeQueryBuilder) {
        if (!modesConfig.containsKey(mode))
            throw new IllegalArgumentException(String.format("Unknown mode '%s'. Known modes: %s", mode, String.join(", ", modesConfig.keySet())));
        routeQueryBuilder.mode(mode);
        return routeQueryBuilder;
    }

    private ImmutableList.Builder<String> processFlag(String flag, Map<String, RoutingFlag> flagConfig,
                             ImmutableList.Builder<String> flagBuilder) {
        if (Objects.nonNull(flag)) {
            if (!flagConfig.containsKey(flag))
                throw new IllegalArgumentException(String.format("Unknown routing flag '%s'. Known flags: %s", flag, String.join(", ", flagConfig.keySet())));
            flagBuilder.add(flagConfig.get(flag).getProviderFlag().orElse(flag));
        }
        return flagBuilder;
    }
}
