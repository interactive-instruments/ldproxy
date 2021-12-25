/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.routes.domain.FeatureTransformationContextRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.ImmutableFeatureTransformationContextRoutes;
import de.ii.xtraplatform.routes.sql.domain.Preference;
import de.ii.ldproxy.ogcapi.routes.domain.QueryHandlerRoutes;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinition;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionInputs;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionInfo;
import de.ii.ldproxy.ogcapi.routes.domain.RouteFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutesFormatExtension;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingFlag;
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
import de.ii.xtraplatform.routes.sql.domain.RouteQuery;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import de.ii.xtraplatform.streams.domain.Reactive;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.validation.constraints.NotNull;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
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

@Component
@Instantiate
@Provides
public class QueryHandlerRoutesImpl implements QueryHandlerRoutes {

    private final I18n i18n;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
    private final CrsTransformerFactory crsTransformerFactory;
    private final EntityRegistry entityRegistry;
    private final CrsSupport crsSupport;

    public QueryHandlerRoutesImpl(@Requires I18n i18n,
                                  @Requires CrsTransformerFactory crsTransformerFactory,
                                  @Requires EntityRegistry entityRegistry,
                                  @Requires CrsSupport crsSupport) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.entityRegistry = entityRegistry;
        this.crsSupport = crsSupport;
        this.queryHandlers = ImmutableMap.of(
            Query.ROUTE_DEFINITION_FORM, QueryHandler.with(QueryInputRouteDefinitionForm.class, this::getForm),
            Query.COMPUTE_ROUTE, QueryHandler.with(QueryInputComputeRoute.class, this::computeRoute)
        );
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getForm(QueryInputRouteDefinitionForm queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();

        List<Link> links = ImmutableList.of();

        RoutesFormatExtension outputFormatExtension = api.getOutputFormat(RoutesFormatExtension.class,
                                                                          requestContext.getMediaType(), "/routes",
                                                                          Optional.empty())
            .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated.", requestContext.getMediaType().type())));

        Object entity = outputFormatExtension.getFormEntity(queryInput.getTemplateInfo(),
                                                            requestContext.getApi(),
                                                            requestContext);

        Date lastModified = getLastModified(queryInput, api);
        EntityTag etag = getEtag(queryInput.getTemplateInfo(), RouteDefinitionInfo.FUNNEL, outputFormatExtension);
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

    private Response computeRoute(QueryInputComputeRoute queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        MediaType contentType = queryInput.getContentType();
        RouteDefinition routeDefinition = parse(queryInput.getRequestBody(), false);
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

        if (!inputs.getWeight().isEmpty() && !Objects.requireNonNullElse(config.getWeightRestrictions(), false)) {
            throw new IllegalArgumentException("This API does not support weight restrictions as part of the definition of a route.");
        }
        routeQueryBuilder.weight(inputs.getWeight());

        if (!inputs.getHeight().isEmpty() && !Objects.requireNonNullElse(config.getHeightRestrictions(), false)) {
            throw new IllegalArgumentException("This API does not support weight restrictions as part of the definition of a route.");
        }
        routeQueryBuilder.height(inputs.getHeight());

        Optional<Geometry.MultiPolygon> obstacles = routeDefinition.getObstacles();
        if (!obstacles.isEmpty() && !Objects.requireNonNullElse(config.getObstacles(), false)) {
            throw new IllegalArgumentException("This API does not support obstacles as part of the definition of a route.");
        }
        routeQueryBuilder.obstacles(obstacles);

        EpsgCrs waypointCrs = routeDefinition.getWaypointsCrs();
        if (!crsSupport.isSupported(apiData, waypointCrs)) {
            throw new IllegalArgumentException(String.format("The parameter 'coordRefSys' in the route definition is invalid: the crs '%s' is not supported", waypointCrs.toUriString()));
        }

        if (!routeDefinition.getWaypoints().isEmpty() && !Objects.requireNonNullElse(config.getIntermediateWaypoints(), false)) {
            throw new IllegalArgumentException(String.format("This API does not support waypoints in addition to the start and end location. The following waypoints were provided: %s",
                                                             routeDefinition.getWaypoints()
                                                                 .stream()
                                                                 .map(p -> p.getCoordinates().get(0).toString())
                                                                 .collect(Collectors.joining(","))));
        }

        RouteQuery routeQuery = routeQueryBuilder.build();
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
            swapCoordinates = crsTransformer.isPresent() && crsTransformer.get()
                .needsCoordinateSwap();
        }

        List<ApiMediaType> alternateMediaTypes = requestContext.getAlternateMediaTypes();

        List<Link> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(), requestContext.getMediaType(), alternateMediaTypes, i18n, requestContext.getLanguage());

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
            .shouldSwapCoordinates(swapCoordinates)
            .isHitsOnlyIfMore(false)
            .showsFeatureSelfLink(false)
            .name(inputs.getName().orElse("Route"))
            .format(outputFormat)
            .outputStream(new OutputStreamToByteConsumer())
            .startTimeNano(System.nanoTime())
            .speedLimitUnit(queryInput.getSpeedLimitUnit())
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

        return prepareSuccessResponse(requestContext, queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      targetCrs,
                                      true,
                                      String.format("%s.%s",
                                                    Objects.requireNonNullElse(inputs.getName(),"Route"),
                                                    outputFormat.getMediaType().fileExtension()))
            .entity(result)
            .build();
    }

    static RouteDefinition parse(byte[] content, boolean strict) {
        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, strict);
        mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        try {
            // parse input
            return mapper.readValue(content, RouteDefinition.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("The content of the route definition is invalid.", e);
        }
    }

    private StreamingOutput stream(FeatureStream featureTransformStream,
                                   final FeatureTokenEncoder<?> encoder) {
        return outputStream -> {
            Reactive.SinkTransformed<Object, byte[]> featureSink = encoder.to(Reactive.Sink.outputStream(outputStream));

            try {
                FeatureStream.Result result = featureTransformStream.runWith(featureSink, Optional.empty())
                    .toCompletableFuture()
                    .join();

                if (result.getError()
                    .isPresent()) {
                    processStreamError(result.getError().get());
                    // the connection has been lost, typically the client has cancelled the request, log on debug level
                    LOGGER.debug("Request cancelled due to lost connection.");
                }

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
