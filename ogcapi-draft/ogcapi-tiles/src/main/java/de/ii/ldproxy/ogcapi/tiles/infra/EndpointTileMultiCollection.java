/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.tiles.app.TileProviderMbtiles;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileEmpty;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileFile;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileMbtilesTile;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileMultiLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTile;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesCache;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handle responses under '/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileMultiCollection extends Endpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileMultiCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access multi-layer tiles");

    private final FeaturesCoreProviders providers;
    private final TilesQueriesHandler queryHandler;
    private final CrsTransformerFactory crsTransformerFactory;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final TilesCache cache;
    private final StaticTileProviderStore staticTileProviderStore;

    EndpointTileMultiCollection(@Requires FeaturesCoreProviders providers,
                                @Requires ExtensionRegistry extensionRegistry,
                                @Requires TilesQueriesHandler queryHandler,
                                @Requires CrsTransformerFactory crsTransformerFactory,
                                @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                                @Requires TilesCache cache,
                                @Requires StaticTileProviderStore staticTileProviderStore) {
        super(extensionRegistry);
        this.providers = providers;
        this.queryHandler = queryHandler;
        this.crsTransformerFactory = crsTransformerFactory;
        this.limitsGenerator = limitsGenerator;
        this.cache = cache;
        this.staticTileProviderStore = staticTileProviderStore;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/core");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
        if (config.map(cfg -> cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
            // Tiles are pre-generated as a static tile set
            return config.map(ExtensionConfiguration::isEnabled).orElse(false);
        } else {
            // Tiles are generated on-demand from a data source
            if (config.filter(TilesConfiguration::isEnabled)
                      .filter(TilesConfiguration::getMultiCollectionEnabledDerived)
                      .isEmpty()) return false;
            // currently no vector tiles support for WFS backends
            return providers.getFeatureProvider(apiData).supportsHighLoad();
        }
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileFormatExtension.class);
        return formats;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("tiles")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_TILE);
        final String path = "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}";
        final HttpMethods method = HttpMethods.GET;
        final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        final List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
        String operationSummary = "fetch a tile with multiple layers, one per collection";
        Optional<String> operationDescription = Optional.of("The tile in the requested tiling scheme ('{tileMatrixSetId}'), " +
                "on the requested zoom level ('{tileMatrix}'), with the requested grid coordinates ('{tileRow}', '{tileCol}') is returned. " +
                "The tile has one layer per collection with all selected features in the bounding box of the tile with the requested properties.");
        ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                .path(path)
                .pathParameters(pathParameters);
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
        if (operation != null)
            resourceBuilder.putOperations(method.name(), operation);
        definitionBuilder.putResources(path, resourceBuilder.build());

        return definitionBuilder.build();
    }

    @Path("/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    public Response getTile(@Auth Optional<User> optionalUser, @Context OgcApi api,
                            @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                            @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                            @Context UriInfo uriInfo, @Context ApiRequestContext requestContext)
            throws CrsTransformationException, IOException, NotFoundException {

        OgcApiDataV2 apiData = api.getData();
        Map<String, String> queryParams = toFlatMap(uriInfo.getQueryParameters());
        TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();

        TileProvider tileProvider = tilesConfiguration.getTileProvider();

        boolean requiresQuerySupport = tileProvider.requiresQuerySupport();

        String definitionPath = "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}";
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrixSetId", tileMatrixSetId);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrix", tileMatrix);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileRow", tileRow);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileCol", tileCol);
        final List<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), definitionPath);

        int row;
        int col;
        int level;
        try {
            level = Integer.parseInt(tileMatrix);
            row = Integer.parseInt(tileRow);
            col = Integer.parseInt(tileCol);
        } catch (NumberFormatException e) {
            throw new ServerErrorException("Could not convert tile coordinates that have been validated to integers", 500);
        }

        MinMax zoomLevels = tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId);
        if (zoomLevels.getMax() < level || zoomLevels.getMin() > level)
            throw new NotFoundException("The requested tile is outside the zoom levels for this tile set.");

        TileMatrixSet tileMatrixSet = extensionRegistry.getExtensionsForType(TileMatrixSet.class).stream()
                .filter(tms -> tms.getId().equals(tileMatrixSetId))
                .findAny()
                .orElseThrow(() -> new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId));

        TileMatrixSetLimits tileLimits = limitsGenerator.getTileMatrixSetLimits(apiData, tileMatrixSet, zoomLevels, crsTransformerFactory)
                .stream()
                .filter(limits -> limits.getTileMatrix().equals(tileMatrix))
                .findAny()
                .orElse(null);

        if (tileLimits!=null) {
            if (tileLimits.getMaxTileCol()<col || tileLimits.getMinTileCol()>col ||
                    tileLimits.getMaxTileRow()<row || tileLimits.getMinTileRow()>row)
                // return 404, if outside the range
                throw new NotFoundException("The requested tile is outside of the limits for this zoom level and tile set.");
        }

        String path = definitionPath.replace("{tileMatrixSetId}", tileMatrixSetId)
                .replace("{tileMatrix}", tileMatrix)
                .replace("{tileRow}", tileRow)
                .replace("{tileCol}", tileCol);

        TileFormatExtension outputFormat = api.getOutputFormat(TileFormatExtension.class, requestContext.getMediaType(), path, Optional.empty())
                .orElseThrow(() -> new NotAcceptableException(MessageFormat.format("The requested media type ''{0}'' is not supported for this resource.", requestContext.getMediaType())));

        if (!requiresQuerySupport) {
            // return a static tile
            if (!(tileProvider instanceof TileProviderMbtiles))
                throw new RuntimeException(String.format("Unexpected tile provider, must be MBTILES. Found: %s", tileProvider.getClass().getSimpleName()));

            Tile tile = new ImmutableTile.Builder()
                    .tileMatrixSet(tileMatrixSet)
                    .tileLevel(level)
                    .tileRow(row)
                    .tileCol(col)
                    .api(api)
                    .outputFormat(outputFormat)
                    .temporary(false)
                    .build();

            String mbtilesFilename = ((TileProviderMbtiles) tileProvider).getFilename();

            java.nio.file.Path provider = staticTileProviderStore.getTileProvider(apiData, mbtilesFilename);

            if (!provider.toFile().exists())
                throw new RuntimeException(String.format("Mbtiles file '%s' does not exist", provider));

            TilesQueriesHandler.QueryInputTileMbtilesTile queryInput = new ImmutableQueryInputTileMbtilesTile.Builder()
                    .from(getGenericQueryInput(api.getData()))
                    .tile(tile)
                    .tileProvider(provider)
                    .build();

            return queryHandler.handle(TilesQueriesHandler.Query.MBTILES_TILE, queryInput, requestContext);
        }

        FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData);
        ensureFeatureProviderSupportsQueries(featureProvider);

        List<String> collections = queryParams.containsKey("collections") ?
                Splitter.on(",")
                        .splitToList(queryParams.get("collections")) :
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                        .filter(collection -> {
                            Optional<TilesConfiguration> layerConfiguration = collection.getExtension(TilesConfiguration.class);
                            if (layerConfiguration.isEmpty() || !layerConfiguration.get().isEnabled() || !layerConfiguration.get().getMultiCollectionEnabledDerived())
                                return false;
                            MinMax levels = layerConfiguration.get().getZoomLevelsDerived().get(tileMatrixSetId);
                            return !Objects.nonNull(levels) || (levels.getMax() >= level && levels.getMin() <= level);
                        })
                        .map(FeatureTypeConfiguration::getId)
                        .collect(Collectors.toList());

        if (!outputFormat.canMultiLayer() && collections.size() > 1)
            throw new NotAcceptableException("The requested tile format supports only a single layer. Please select only a single collection.");

        // check, if the cache can be used (no query parameters except f)
        boolean useCache = queryParams.isEmpty() || (queryParams.size()==1 && queryParams.containsKey("f"));

        Tile multiLayerTile = new ImmutableTile.Builder()
                .collectionIds(collections)
                .tileMatrixSet(tileMatrixSet)
                .tileLevel(level)
                .tileRow(row)
                .tileCol(col)
                .api(api)
                .temporary(!useCache)
                .featureProvider(featureProvider)
                .outputFormat(outputFormat)
                .build();

        if (collections.isEmpty()) {
            TilesQueriesHandler.QueryInputTileEmpty queryInput = new ImmutableQueryInputTileEmpty.Builder()
                    .from(getGenericQueryInput(api.getData()))
                    .tile(multiLayerTile)
                    .build();

            return queryHandler.handle(TilesQueriesHandler.Query.EMPTY_TILE, queryInput, requestContext);
        }

        // if cache can be used and the tile is cached for the requested format, return the cache
        if (useCache) {
            // get the tile from the cache and return it
            java.nio.file.Path tileFile = cache.getFile(multiLayerTile);
            if (Files.exists(tileFile)) {
                TilesQueriesHandler.QueryInputTileFile queryInput = new ImmutableQueryInputTileFile.Builder()
                        .from(getGenericQueryInput(api.getData()))
                        .tile(multiLayerTile)
                        .tileFile(tileFile)
                        .build();

                return queryHandler.handle(TilesQueriesHandler.Query.TILE_FILE, queryInput, requestContext);
            }
        }

        // don't store the tile in the cache if it is outside the range
        MinMax cacheMinMax = tilesConfiguration.getZoomLevelsDerived()
                                               .get(tileMatrixSetId);
        Tile finalMultiLayerTile = Objects.isNull(cacheMinMax) || (level <= cacheMinMax.getMax() && level >= cacheMinMax.getMin()) ?
                multiLayerTile :
                new ImmutableTile.Builder()
                        .from(multiLayerTile)
                        .temporary(true)
                        .build();

        Map<String, Tile> singleLayerTileMap = collections.stream()
                                                          .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> new ImmutableTile.Builder()
                        .from(finalMultiLayerTile)
                        .collectionIds(ImmutableList.of(collectionId))
                        .build()));

        // first execute the information that is passed as processing parameters (e.g., "properties")
        Map<String, Object> processingParameters = new HashMap<>();
        for (OgcApiQueryParameter parameter : allowedParameters) {
            processingParameters = parameter.transformContext(null, processingParameters, queryParams, api.getData());
        }

        Map<String, FeatureQuery> queryMap = collections.stream()
                // skip collections without spatial queryable
                .filter(collectionId -> {
                    Optional<FeaturesCoreConfiguration> featuresConfiguration = apiData.getCollections()
                                                                                       .get(collectionId)
                                                                                       .getExtension(FeaturesCoreConfiguration.class);
                    return featuresConfiguration.isPresent()
                            && featuresConfiguration.get().getQueryables().isPresent()
                            && !featuresConfiguration.get().getQueryables().get().getSpatial().isEmpty();
                })
                .collect(ImmutableMap.toImmutableMap(collectionId -> collectionId, collectionId -> {
                    String featureTypeId = apiData.getCollections()
                                                  .get(collectionId)
                                                  .getExtension(FeaturesCoreConfiguration.class)
                                                  .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                                  .orElse(collectionId);
                    TilesConfiguration layerConfiguration = apiData.getCollections()
                                                                   .get(collectionId)
                                                                   .getExtension(TilesConfiguration.class)
                                                                   .orElse(tilesConfiguration);
                    FeatureQuery query = outputFormat.getQuery(singleLayerTileMap.get(collectionId), allowedParameters, queryParams, layerConfiguration, requestContext.getUriCustomizer());
                    return ImmutableFeatureQuery.builder()
                                                .from(query)
                                                .type(featureTypeId)
                                                .build();
                }));

        FeaturesCoreConfiguration coreConfiguration = apiData.getExtension(FeaturesCoreConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileMultiLayer queryInput = new ImmutableQueryInputTileMultiLayer.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tile(finalMultiLayerTile)
                .singleLayerTileMap(singleLayerTileMap)
                .queryMap(queryMap)
                .processingParameters(processingParameters)
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.MULTI_LAYER_TILE, queryInput, requestContext);
    }

    private void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("Feature provider does not support queries.");
        }
    }
}
