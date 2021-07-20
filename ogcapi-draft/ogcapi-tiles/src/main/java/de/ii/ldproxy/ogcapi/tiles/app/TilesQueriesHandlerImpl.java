/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.DefaultLinksGenerator;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchema;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorFeatureGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.SchemaGeneratorGeoJson;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableFeatureTransformationContextTiles;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTileLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTilePoint;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesCache;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureStream2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Component
@Instantiate
@Provides
public class TilesQueriesHandlerImpl implements TilesQueriesHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TilesQueriesHandlerImpl.class);

    private final I18n i18n;
    private final CrsTransformerFactory crsTransformerFactory;
    private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
    private final MetricRegistry metricRegistry;
    private final EntityRegistry entityRegistry;
    private final ExtensionRegistry extensionRegistry;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final TilesCache tilesCache;
    private final StaticTileProviderStore staticTileProviderStore;
    private final SchemaGeneratorFeatureGeoJson schemaGeneratorFeature;

    public TilesQueriesHandlerImpl(@Requires I18n i18n,
                                   @Requires CrsTransformerFactory crsTransformerFactory,
                                   @Requires Dropwizard dropwizard,
                                   @Requires EntityRegistry entityRegistry,
                                   @Requires ExtensionRegistry extensionRegistry,
                                   @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                                   @Requires TilesCache tilesCache,
                                   @Requires StaticTileProviderStore staticTileProviderStore,
                                   @Requires SchemaGeneratorFeatureGeoJson schemaGeneratorFeature) {
        this.i18n = i18n;
        this.crsTransformerFactory = crsTransformerFactory;
        this.entityRegistry = entityRegistry;

        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();
        this.extensionRegistry = extensionRegistry;
        this.limitsGenerator = limitsGenerator;
        this.tilesCache = tilesCache;
        this.staticTileProviderStore = staticTileProviderStore;
        this.schemaGeneratorFeature = schemaGeneratorFeature;

        this.queryHandlers = new ImmutableMap.Builder()
                .put(Query.TILE_SETS, QueryHandler.with(QueryInputTileSets.class, this::getTileSetsResponse))
                .put(Query.TILE_SET, QueryHandler.with(QueryInputTileSet.class, this::getTileSetResponse))
                .put(Query.SINGLE_LAYER_TILE, QueryHandler.with(QueryInputTileSingleLayer.class, this::getSingleLayerTileResponse))
                .put(Query.MULTI_LAYER_TILE, QueryHandler.with(QueryInputTileMultiLayer.class, this::getMultiLayerTileResponse))
                .put(Query.EMPTY_TILE, QueryHandler.with(QueryInputTileEmpty.class, this::getEmptyTileResponse))
                .put(Query.TILE_FILE, QueryHandler.with(QueryInputTileFile.class, this::getTileFileResponse))
                .put(Query.MBTILES_TILE, QueryHandler.with(QueryInputTileMbtilesTile.class, this::getMbtilesTileResponse))
                .build();
    }

    @Override
    public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
        return queryHandlers;
    }

    private Response getTileSetsResponse(QueryInputTileSets queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Optional<String> collectionId = queryInput.getCollectionId();
        String path = collectionId.map(s -> "/collections/" + s + "/tiles").orElse("/tiles");

        TileSetsFormatExtension outputFormat = api.getOutputFormat(TileSetsFormatExtension.class, requestContext.getMediaType(), path, collectionId)
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        Optional<FeatureTypeConfigurationOgcApi> featureType = collectionId.map(s -> requestContext.getApi().getData().getCollections().get(s));
        Map<String, MinMax> tileMatrixSetZoomLevels = queryInput.getTileMatrixSetZoomLevels();
        double[] center = queryInput.getCenter();

        List<ApiMediaType> tileSetFormats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class)
                                                             .stream()
                                                             .filter(format -> collectionId.map(s -> format.isEnabledForApi(apiData, s)).orElseGet(() -> format.isEnabledForApi(apiData)))
                                                             .filter(format -> {
                    Optional<TilesConfiguration> config = collectionId.isPresent() ?
                            apiData.getCollections().get(collectionId.get()).getExtension(TilesConfiguration.class) :
                            apiData.getExtension(TilesConfiguration.class);
                    return config.isPresent() && (config.get().getTileSetEncodings()==null || (config.get().getTileSetEncodings().isEmpty() || config.get().getTileSetEncodings().contains(format.getMediaType().label())));
                })
                                                             .map(FormatExtension::getMediaType)
                                                             .collect(Collectors.toList());

        List<ApiMediaType> tileFormats = extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                                                          .stream()
                                                          .filter(format -> collectionId.map(s -> format.isEnabledForApi(apiData, s)).orElseGet(() -> format.isEnabledForApi(apiData)))
                                                          .filter(format -> {
                                                              Optional<TilesConfiguration> config = collectionId.isPresent() ?
                                                                      apiData.getCollections().get(collectionId.get()).getExtension(TilesConfiguration.class) :
                                                                      apiData.getExtension(TilesConfiguration.class);
                                                              return config.isPresent() && (config.get().getTileEncodingsDerived()==null || (config.get().getTileEncodingsDerived().isEmpty() || config.get().getTileEncodingsDerived().contains(format.getMediaType().label())));
                                                          })
                                                          .map(FormatExtension::getMediaType)
                                                          .collect(Collectors.toList());

        List<Link> links = vectorTilesLinkGenerator.generateTileSetsLinks(requestContext.getUriCustomizer(),
                                                                          requestContext.getMediaType(),
                                                                          requestContext.getAlternateMediaTypes(),
                                                                          tileFormats,
                                                                          i18n,
                                                                          requestContext.getLanguage());

        ImmutableTileSets.Builder builder = ImmutableTileSets.builder()
                                                             .title(featureType.isPresent()
                                                                            ? featureType.get().getLabel()
                                                                            : apiData.getLabel())
                                                             .description(featureType.map(ft -> ft.getDescription().orElse(""))
                                                                                     .orElseGet(() -> apiData.getDescription().orElse("")))
                                                             .links(links);

        List<TileMatrixSet> tileMatrixSets = tileMatrixSetZoomLevels.keySet()
                                                                    .stream()
                                                                    .map(this::getTileMatrixSetById)
                                                                    .collect(Collectors.toUnmodifiableList());

        builder.tilesets(tileMatrixSets.stream()
                                       .map(tileMatrixSet -> getTileSet(apiData,
                                                                    tileMatrixSet,
                                                                    tileMatrixSetZoomLevels.get(tileMatrixSet.getId()),
                                                                    center,
                                                                    collectionId,
                                                                    vectorTilesLinkGenerator.generateTileSetEmbeddedLinks(requestContext.getUriCustomizer(),
                                                                                                               tileMatrixSet.getId(),
                                                                                                               tileFormats,
                                                                                                               i18n,
                                                                                                               requestContext.getLanguage()),
                                                                    requestContext.getUriCustomizer().copy()))
                                       .collect(Collectors.toUnmodifiableList()));

        TileSets tileSets = builder.build();

        Date lastModified = getLastModified(queryInput, requestContext.getApi());
        EntityTag etag = getEtag(tileSets, TileSets.FUNNEL, outputFormat);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext.getApi(), requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null), null)
                .entity(outputFormat.getTileSetsEntity(tileSets, collectionId, api, requestContext))
                .build();
    }

    private Response getTileSetResponse(QueryInputTileSet queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        String tileMatrixSetId = queryInput.getTileMatrixSetId();
        Optional<String> collectionId = queryInput.getCollectionId();
        String path = collectionId.map(s -> "/collections/" + s + "/tiles/" + tileMatrixSetId).orElseGet(() -> "/tiles/" + tileMatrixSetId);

        TileSetFormatExtension outputFormat = api.getOutputFormat(TileSetFormatExtension.class, requestContext.getMediaType(), path, collectionId)
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        List<ApiMediaType> tileFormats = extensionRegistry.getExtensionsForType(TileFormatExtension.class)
                                                          .stream()
                                                          .filter(format -> collectionId.map(s -> format.isEnabledForApi(apiData, s)).orElseGet(() -> format.isEnabledForApi(apiData)))
                                                          .filter(format -> {
                                                              Optional<TilesConfiguration> config = collectionId.map(cid -> apiData.getCollections().get(cid).getExtension(TilesConfiguration.class))
                                                                                                                .orElse(apiData.getExtension(TilesConfiguration.class));
                                                              return config.isPresent() && (config.get().getTileEncodingsDerived()==null || (config.get().getTileEncodingsDerived().isEmpty() || config.get().getTileEncodingsDerived().contains(format.getMediaType().label())));
                                                          })
                                                          .map(FormatExtension::getMediaType)
                                                          .collect(Collectors.toList());

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<Link> links = vectorTilesLinkGenerator.generateTileSetLinks(requestContext.getUriCustomizer(),
                                                                         requestContext.getMediaType(),
                                                                         requestContext.getAlternateMediaTypes(),
                                                                         tileFormats,
                                                                         i18n,
                                                                         requestContext.getLanguage());

        MinMax zoomLevels = queryInput.getZoomLevels();
        double[] center = queryInput.getCenter();
        TileSet tileset = getTileSet(apiData, getTileMatrixSetById(tileMatrixSetId),
                                     zoomLevels, center,
                                     collectionId, links,
                                     requestContext.getUriCustomizer().copy());

        Date lastModified = getLastModified(queryInput, requestContext.getApi());
        EntityTag etag = getEtag(tileset, TileSet.FUNNEL, outputFormat);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext.getApi(), requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null)
                .entity(outputFormat.getTileSetEntity(tileset, apiData, collectionId, requestContext))
                .build();
    }

    private Response getSingleLayerTileResponse(QueryInputTileSingleLayer queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Tile tile = queryInput.getTile();
        String collectionId = tile.getCollectionId();
        FeatureProvider2 featureProvider = tile.getFeatureProvider().get();
        FeatureQuery query = queryInput.getQuery();
        TileFormatExtension outputFormat = tile.getOutputFormat();

        // process parameters and generate query
        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs targetCrs = query.getCrs()
                                 .orElse(queryInput.getDefaultCrs());
        if (featureProvider.supportsCrs()) {
            EpsgCrs sourceCrs = featureProvider.crs()
                                               .getNativeCrs();
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() && crsTransformer.get()
                                                                          .needsCoordinateSwap();
        }

        List<Link> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                                                                     requestContext.getMediaType(),
                                                                     requestContext.getAlternateMediaTypes(),
                                                                     i18n,
                                                                     requestContext.getLanguage());

        String featureTypeId = apiData.getCollections()
                                            .get(collectionId)
                                            .getExtension(FeaturesCoreConfiguration.class)
                                            .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                            .orElse(collectionId);

        ImmutableFeatureTransformationContextTiles.Builder transformationContext;
        try {
            transformationContext = new ImmutableFeatureTransformationContextTiles.Builder()
                    .apiData(apiData)
                    .featureSchema(featureProvider.getData().getTypes().get(featureTypeId))
                    .tile(tile)
                    .tileFile(tilesCache.getFile(tile))
                    .collectionId(collectionId)
                    .ogcApiRequest(requestContext)
                    .crsTransformer(crsTransformer)
                    .crsTransformerFactory(crsTransformerFactory)
                    .shouldSwapCoordinates(swapCoordinates)
                    .codelists(entityRegistry.getEntitiesForType(Codelist.class)
                                             .stream()
                                             .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
                    .defaultCrs(queryInput.getDefaultCrs())
                    .links(links)
                    .isFeatureCollection(true)
                    .fields(query.getFields())
                    .limit(query.getLimit())
                    .offset(0)
                    .i18n(i18n);
        } catch (Exception e) {
            throw new RuntimeException("Error building the tile transformation context.", e);
        }

        if (queryInput.getOutputStream().isPresent()) {

            // do not stream as a response
            if (outputFormat.canTransformFeatures()) {
                FeatureStream2 featureStream = featureProvider.queries()
                        .getFeatureStream2(query);

                try {
                    Optional<FeatureTransformer2> featureTransformer = outputFormat.getFeatureTransformer(
                            transformationContext.outputStream(queryInput.getOutputStream().get()).build(),
                            requestContext.getLanguage());

                    if (featureTransformer.isPresent()) {
                        FeatureStream2.Result result = featureStream.runWith(featureTransformer.get())
                                                                    .toCompletableFuture()
                                                                    .join();
                        if (result.getError()
                                  .isPresent()) {
                            processStreamError(result.getError().get());
                            // the connection has been lost, typically the client has cancelled the request, log on debug level
                            LOGGER.debug("Request cancelled due to lost connection.");
                        }
                    } else {
                        throw new IllegalStateException("Could not acquire FeatureTransformer.");
                    }
                } catch (CompletionException e) {
                    if (e.getCause() instanceof WebApplicationException) {
                        throw (WebApplicationException) e.getCause();
                    }
                    throw new IllegalStateException("Feature stream error.", e.getCause());
                }
            } else {
                throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated, because it does not support streaming.", requestContext.getMediaType().type()));
            }

            // internal processing, no need to process headers
            return prepareSuccessResponse(requestContext.getApi(), requestContext, null)
                    .entity(queryInput.getOutputStream().get())
                    .build();
        }

        // streaming output as response
        StreamingOutput streamingOutput;
        if (outputFormat.canTransformFeatures()) {
            FeatureStream2 featureStream = featureProvider.queries()
                    .getFeatureStream2(query);

            streamingOutput = stream(featureStream,
                    outputStream -> outputFormat.getFeatureTransformer(
                            transformationContext.outputStream(outputStream).build(),
                            requestContext.getLanguage())
                            .get());
        } else {
            throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated, because it does not support streaming.", requestContext.getMediaType().type()));
        }

        Date lastModified = Date.from(Instant.now());
        EntityTag etag = null;
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext.getApi(), requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null)
                .entity(streamingOutput)
                .build();
    }

    private Response getMultiLayerTileResponse(QueryInputTileMultiLayer queryInput, ApiRequestContext requestContext) {
        OgcApi api = requestContext.getApi();
        OgcApiDataV2 apiData = api.getData();
        Tile multiLayerTile = queryInput.getTile();
        List<String> collectionIds = multiLayerTile.getCollectionIds();
        Map<String, FeatureQuery> queryMap = queryInput.getQueryMap();
        Map<String, Tile> singleLayerTileMap = queryInput.getSingleLayerTileMap();
        FeatureProvider2 featureProvider = multiLayerTile.getFeatureProvider().get();
        TileMatrixSet tileMatrixSet = multiLayerTile.getTileMatrixSet();
        int tileLevel = multiLayerTile.getTileLevel();
        int tileRow = multiLayerTile.getTileRow();
        int tileCol = multiLayerTile.getTileCol();
        TileFormatExtension outputFormat = multiLayerTile.getOutputFormat();

        // process parameters and generate query
        Optional<CrsTransformer> crsTransformer = Optional.empty();
        boolean swapCoordinates = false;

        EpsgCrs targetCrs = tileMatrixSet.getCrs();
        if (featureProvider.supportsCrs()) {
            EpsgCrs sourceCrs = featureProvider.crs()
                    .getNativeCrs();
            crsTransformer = crsTransformerFactory.getTransformer(sourceCrs, targetCrs);
            swapCoordinates = crsTransformer.isPresent() && crsTransformer.get()
                                                                          .needsCoordinateSwap();
        }

        List<Link> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                                                                     requestContext.getMediaType(),
                                                                     requestContext.getAlternateMediaTypes(),
                                                                     i18n,
                                                                     requestContext.getLanguage());

        Map<String, ByteArrayOutputStream> byteArrayMap = collectionIds.stream()
                .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> new ByteArrayOutputStream()));

        for (String collectionId : collectionIds) {
            // TODO limitation of the current model: all layers have to come from the same feature provider and use the same CRS

            Tile tile = singleLayerTileMap.get(collectionId);
            Path tileFile;
            try {
                tileFile = tilesCache.getFile(tile);
            } catch (IOException e) {
                throw new RuntimeException("Error accessing the tile cache.", e);
            }
            ByteArrayOutputStream singleLayerOutputStream = byteArrayMap.get(collectionId);
            if (!multiLayerTile.getTemporary() && Files.exists(tileFile)) {
                // use cached tile
                try {
                    if (Files.size(tileFile)>0) {
                        singleLayerOutputStream.write(Files.readAllBytes(tileFile));
                        singleLayerOutputStream.flush();
                    }
                    continue;
                } catch (IOException e) {
                    // could not read the cache, generate the tile
                }
            }

            String featureTypeId = apiData.getCollections()
                                      .get(collectionId)
                                      .getExtension(FeaturesCoreConfiguration.class)
                                      .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                      .orElse(collectionId);

            FeatureQuery query = queryMap.get(collectionId);
            ImmutableFeatureTransformationContextTiles transformationContext;
            try {
                transformationContext = new ImmutableFeatureTransformationContextTiles.Builder()
                        .apiData(apiData)
                        .featureSchema(featureProvider.getData().getTypes().get(featureTypeId))
                        .tile(tile)
                        .tileFile(tilesCache.getFile(tile))
                        .collectionId(collectionId)
                        .ogcApiRequest(requestContext)
                        .crsTransformer(crsTransformer)
                        .crsTransformerFactory(crsTransformerFactory)
                        .shouldSwapCoordinates(swapCoordinates)
                        .codelists(entityRegistry.getEntitiesForType(Codelist.class)
                                                 .stream()
                                                 .collect(Collectors.toMap(PersistentEntity::getId, c -> c)))
                        .defaultCrs(queryInput.getDefaultCrs())
                        .links(links)
                        .isFeatureCollection(true)
                        .fields(query.getFields())
                        .limit(query.getLimit())
                        .offset(0)
                        .i18n(i18n)
                        .outputStream(singleLayerOutputStream)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Error building the tile transformation context.", e);
            }

            if (outputFormat.canTransformFeatures()) {
                FeatureStream2 featureStream = featureProvider.queries()
                        .getFeatureStream2(query);

                try {
                    Optional<FeatureTransformer2> featureTransformer = outputFormat.getFeatureTransformer(transformationContext, requestContext.getLanguage());

                    if (featureTransformer.isPresent()) {
                        FeatureStream2.Result result = featureStream.runWith(featureTransformer.get())
                                                                    .toCompletableFuture()
                                                                    .join();
                        if (result.getError()
                                  .isPresent()) {
                            processStreamError(result.getError().get());
                            // the connection has been lost, typically the client has cancelled the request, log on debug level
                            LOGGER.debug("Request cancelled due to lost connection.");
                        }
                    } else {
                        throw new IllegalStateException("Could not acquire FeatureTransformer.");
                    }
                } catch (CompletionException e) {
                    if (e.getCause() instanceof WebApplicationException) {
                        throw (WebApplicationException) e.getCause();
                    }
                    throw new IllegalStateException("Feature stream error.", e.getCause());
                }
            } else {
                throw new NotAcceptableException(MessageFormat.format("The requested media type {0} cannot be generated, because it does not support streaming.", requestContext.getMediaType().type()));
            }
        }

        TileFormatExtension.MultiLayerTileContent result;
        try {
            result = outputFormat.combineSingleLayerTilesToMultiLayerTile(tileMatrixSet, singleLayerTileMap, byteArrayMap);
        } catch (IOException e) {
            throw new RuntimeException("Error accessing the tile cache.", e);
        }

        // try to write/update tile in cache, if all collections have been processed
        if (result.isComplete) {
            Path tileFile;
            try {
                tileFile = tilesCache.getFile(multiLayerTile);
            } catch (IOException e) {
                throw new RuntimeException("Error accessing the tile cache.", e);
            }
            if (Files.notExists(tileFile) || Files.isWritable(tileFile)) {
                try {
                    Files.write(tileFile, result.byteArray);
                } catch (IOException e) {
                    String msg = "Failure to write the multi-layer file of tile {}/{}/{}/{} in dataset '{}', format '{}' to the cache.";
                    LOGGER.info(msg, tileMatrixSet.getId(), tileLevel, tileRow, tileCol, api.getId(), outputFormat.getExtension());
                }
            }
        }

        Date lastModified = Date.from(Instant.now());
        EntityTag etag = getEtag(result.byteArray);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext.getApi(), requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null)
                .entity(result.byteArray)
                .build();
    }

    private Response getTileFileResponse(QueryInputTileFile queryInput, ApiRequestContext requestContext) {

        File tileFile = queryInput.getTileFile().toFile();
        StreamingOutput streamingOutput = outputStream -> ByteStreams.copy(new FileInputStream(tileFile), outputStream);

        List<Link> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                                                                     requestContext.getMediaType(),
                                                                     requestContext.getAlternateMediaTypes(),
                                                                     i18n,
                                                                     requestContext.getLanguage());

        Date lastModified = getLastModified(tileFile);
        EntityTag etag = getEtag(tileFile);
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext.getApi(), requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified, etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null), null)
                .entity(streamingOutput)
                .build();
    }

    private Response getMbtilesTileResponse(QueryInputTileMbtilesTile queryInput, ApiRequestContext requestContext) {

        StreamingOutput streamingOutput = outputStream -> ByteStreams.copy(staticTileProviderStore.getTile(queryInput.getTileProvider(), queryInput.getTile()), outputStream);

        List<Link> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                                                                     requestContext.getMediaType(),
                                                                     ImmutableList.of(),
                                                                     i18n,
                                                                     requestContext.getLanguage());

        Date lastModified = getLastModified(queryInput.getTileProvider().toFile());
        EntityTag etag = getEtag(staticTileProviderStore.getTile(queryInput.getTileProvider(), queryInput.getTile()));
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext.getApi(), requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null)
                .entity(streamingOutput)
                .build();
    }

    private Response getEmptyTileResponse(QueryInputTileEmpty queryInput, ApiRequestContext requestContext) {

        List<Link> links = new DefaultLinksGenerator().generateLinks(requestContext.getUriCustomizer(),
                                                                     requestContext.getMediaType(),
                                                                     requestContext.getAlternateMediaTypes(),
                                                                     i18n,
                                                                     requestContext.getLanguage());

        Tile tile = queryInput.getTile();

        Date lastModified = Date.from(Instant.now());
        EntityTag etag = getEtag(tile.getOutputFormat().getEmptyTile(tile));
        Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
        if (Objects.nonNull(response))
            return response.build();

        return prepareSuccessResponse(requestContext.getApi(), requestContext,
                                      queryInput.getIncludeLinkHeader() ? links : null,
                                      lastModified,
                                      etag,
                                      queryInput.getCacheControl().orElse(null),
                                      queryInput.getExpires().orElse(null),
                                      null)
                .entity(tile.getOutputFormat().getEmptyTile(tile))
                .build();
    }

    private StreamingOutput stream(FeatureStream2 featureTransformStream,
                                   final Function<OutputStream, FeatureTransformer2> featureTransformer) {
        Timer.Context timer = metricRegistry.timer(name(TilesQueriesHandlerImpl.class, "stream"))
                                            .time();

        return outputStream -> {
            try {
                FeatureStream2.Result result = featureTransformStream.runWith(featureTransformer.apply(outputStream))
                                                                     .toCompletableFuture()
                                                                     .join();
                timer.stop();

                if (result.getError()
                          .isPresent()) {
                    processStreamError(result.getError().get());
                    // the connection has been lost, typically the client has cancelled the request, log on debug level
                    LOGGER.debug("Request cancelled due to lost connection.");
                }

            } catch (CompletionException e) {
                if (e.getCause() instanceof WebApplicationException) {
                    throw (WebApplicationException) e.getCause();
                }
                throw new IllegalStateException("Feature stream error.", e.getCause());
            }
        };
    }

    private TileSet getTileSet(OgcApiDataV2 apiData, TileMatrixSet tileMatrixSet, MinMax zoomLevels, double[] center,
                               Optional<String> collectionId, List<Link> links, URICustomizer uriCustomizer) {
        ImmutableTileSet.Builder builder = ImmutableTileSet.builder()
                                                           .dataType(TileSet.DataType.vector);

        builder.tileMatrixSetId(tileMatrixSet.getId());

        if (tileMatrixSet.getURI().isPresent())
            builder.tileMatrixSetURI(tileMatrixSet.getURI().get().toString());
        else
            builder.tileMatrixSet(tileMatrixSet.getTileMatrixSetData());

        builder.tileMatrixSetDefinition(uriCustomizer.removeLastPathSegments(collectionId.isPresent() ? 3 : 1)
                                                     .clearParameters()
                                                     .ensureLastPathSegments("tileMatrixSets", tileMatrixSet.getId())
                                                     .toString())
               .tileMatrixSetLimits(collectionId.isPresent()
                                            ? limitsGenerator.getCollectionTileMatrixSetLimits(apiData, collectionId.get(), tileMatrixSet, zoomLevels, crsTransformerFactory)
                                            : limitsGenerator.getTileMatrixSetLimits(apiData, tileMatrixSet, zoomLevels, crsTransformerFactory));

        if (zoomLevels.getDefault().isPresent() || Objects.nonNull(center)) {
            ImmutableTilePoint.Builder builder2 = new ImmutableTilePoint.Builder();
            zoomLevels.getDefault().ifPresent(def -> builder2.tileMatrix(String.valueOf(def)));
            if (Objects.nonNull(center))
                builder2.addCoordinates(center);
            builder.centerPoint(builder2.build());
        }

        // prepare a map with the JSON schemas of the feature collections used in the style
        SchemaGeneratorFeature.SCHEMA_TYPE schemaType = SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT;
        Map<String, JsonSchemaObject> schemaMap = collectionId.isPresent()
                ? apiData.getCollections()
                         .get(collectionId.get())
                         .getExtension(TilesConfiguration.class)
                         .filter(ExtensionConfiguration::isEnabled)
                         .map(config -> ImmutableMap.of(collectionId.get(), schemaGeneratorFeature.getSchemaJson(apiData, collectionId.get(), Optional.empty(), schemaType)))
                         .orElse(ImmutableMap.of())
                : apiData.getCollections()
                         .entrySet()
                         .stream()
                         .filter(entry -> {
                             Optional<TilesConfiguration> config = entry.getValue().getExtension(TilesConfiguration.class);
                             return entry.getValue().getEnabled() &&
                                     config.isPresent() &&
                                     config.get().getMultiCollectionEnabledDerived();
                         })
                         .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), schemaGeneratorFeature.getSchemaJson(apiData, entry.getKey(), Optional.empty(), schemaType)))
                         .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        schemaMap.entrySet()
                 .stream()
                 .forEach(entry -> {
                     String collectionId2 = entry.getKey();
                     FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
                                                                            .get(collectionId2);
                     JsonSchemaObject schema = entry.getValue();
                     ImmutableTileLayer.Builder builder2 = ImmutableTileLayer.builder()
                                                                             .id(collectionId2)
                                                                             .title(collectionData.getLabel())
                                                                             .description(collectionData.getDescription())
                                                                             .dataType(TileSet.DataType.vector);

                     final JsonSchema geometry = schema.getProperties().get("geometry");
                     if (Objects.nonNull(geometry)) {
                         String geomAsString = geometry.toString();
                         boolean point = geomAsString.contains("GeoJSON Point") || geomAsString.contains("GeoJSON MultiPoint");
                         boolean line = geomAsString.contains("GeoJSON LineString") || geomAsString.contains("GeoJSON MultiLineString");
                         boolean polygon = geomAsString.contains("GeoJSON Polygon") || geomAsString.contains("GeoJSON MultiPolygon");
                         if (point && !line && !polygon)
                             builder2.geometryType(TileLayer.GeometryType.points);
                         else if (!point && line && !polygon)
                             builder2.geometryType(TileLayer.GeometryType.lines);
                         else if (!point && !line && polygon)
                             builder2.geometryType(TileLayer.GeometryType.polygons);
                     }

                     final JsonSchemaObject properties = (JsonSchemaObject) schema.getProperties().get("properties");
                     builder2.propertiesSchema(ImmutableJsonSchemaObject.builder()
                                                                        .required(properties.getRequired())
                                                                        .properties(properties.getProperties())
                                                                        .patternProperties(properties.getPatternProperties())
                                                                        .build());
                     builder.addLayers(builder2.build());
                 });

        builder.links(links);

        return builder.build();
    }

    private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
        return extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                    .filter(tms -> tms.getId().equals(tileMatrixSetId))
                    .findAny()
                    .orElseThrow(() -> new ServerErrorException("TileMatrixSet not found: "+tileMatrixSetId, 500));
    }
}
