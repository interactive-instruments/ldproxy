/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
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
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileMbtilesTile;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSingleLayer;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileStream;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTile;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TileCache;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
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
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Handle responses under '/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileSingleCollection extends EndpointSubCollection implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileSingleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access single-layer tiles");

    private final FeaturesCoreProviders providers;
    private final TilesQueriesHandler queryHandler;
    private final CrsTransformerFactory crsTransformerFactory;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final TileCache cache;
    private final StaticTileProviderStore staticTileProviderStore;
    private final TileMatrixSetRepository tileMatrixSetRepository;

    EndpointTileSingleCollection(@Requires FeaturesCoreProviders providers,
                                 @Requires ExtensionRegistry extensionRegistry,
                                 @Requires TilesQueriesHandler queryHandler,
                                 @Requires CrsTransformerFactory crsTransformerFactory,
                                 @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                                 @Requires TileCache cache,
                                 @Requires StaticTileProviderStore staticTileProviderStore,
                                 @Requires TileMatrixSetRepository tileMatrixSetRepository) {
        super(extensionRegistry);
        this.providers = providers;
        this.queryHandler = queryHandler;
        this.crsTransformerFactory = crsTransformerFactory;
        this.limitsGenerator = limitsGenerator;
        this.cache = cache;
        this.staticTileProviderStore = staticTileProviderStore;
        this.tileMatrixSetRepository = tileMatrixSetRepository;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/core");
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
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        Optional<TilesConfiguration> config = apiData.getCollections()
                                                     .get(collectionId)
                                                     .getExtension(TilesConfiguration.class);
        if (config.map(cfg -> cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
            // Tiles are pre-generated as a static tile set
            return config.map(ExtensionConfiguration::isEnabled).orElse(false);
        } else {
            // Tiles are generated on-demand from a data source
            if (config.filter(TilesConfiguration::isEnabled)
                      .filter(TilesConfiguration::isSingleCollectionEnabled)
                      .isEmpty()) return false;
            // currently no vector tiles support for WFS backends
            return providers.getFeatureProvider(apiData).supportsHighLoad();
        }
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("collections")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_TILE_COLLECTION);
        final String subSubPath = "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}";
        final String path = "/collections/{collectionId}" + subSubPath;
        final HttpMethods method = HttpMethods.GET;
        final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
        if (optCollectionIdParam.isEmpty()) {
            LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
        } else {
            final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
            boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);
            final List<String> collectionIds = (explode) ?
                    collectionIdParam.getValues(apiData) :
                    ImmutableList.of("{collectionId}");
            for (String collectionId : collectionIds) {
                List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                String operationSummary = "fetch a tile of the collection '"+collectionId+"'";
                Optional<String> operationDescription = Optional.of("The tile in the requested tiling scheme ('{tileMatrixSetId}'), " +
                        "on the requested zoom level ('{tileMatrix}'), with the requested grid coordinates ('{tileRow}', '{tileCol}') is returned. " +
                        "The tile has a single layer with all selected features in the bounding box of the tile with the requested properties.");
                String resourcePath = path.replace("{collectionId}", collectionId);
                ImmutableOgcApiResourceData.Builder resourceBuilder = new ImmutableOgcApiResourceData.Builder()
                        .path(resourcePath)
                        .pathParameters(pathParameters);
                ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                if (operation != null)
                    resourceBuilder.putOperations(method.name(), operation);
                definitionBuilder.putResources(resourcePath, resourceBuilder.build());
            }
        }

        return definitionBuilder.build();
    }

    @Path("/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    public Response getTile(@Auth Optional< User > optionalUser, @Context OgcApi api, @PathParam("collectionId") String collectionId,
                            @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                            @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                            @Context UriInfo uriInfo, @Context ApiRequestContext requestContext)
            throws CrsTransformationException, IOException, NotFoundException {

        OgcApiDataV2 apiData = api.getData();
        FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
        TilesConfiguration tilesConfiguration = featureType.getExtension(TilesConfiguration.class).get();

        TileProvider tileProvider = tilesConfiguration.getTileProvider();

        boolean requiresQuerySupport = tileProvider.requiresQuerySupport();

        String definitionPath = "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}";
        checkPathParameter(extensionRegistry, apiData, definitionPath, "collectionId", collectionId);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrixSetId", tileMatrixSetId);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrix", tileMatrix);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileRow", tileRow);
        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileCol", tileCol);
        final List<OgcApiQueryParameter> allowedParameters = getQueryParameters(extensionRegistry, api.getData(), definitionPath, collectionId);

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

        TileMatrixSet tileMatrixSet = tileMatrixSetRepository.get(tileMatrixSetId)
                                                             .orElseThrow(() -> new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId));

        TileMatrixSetLimits tileLimits = limitsGenerator.getCollectionTileMatrixSetLimits(apiData, collectionId, tileMatrixSet, zoomLevels)
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

        String path = definitionPath.replace("{collectionId}", collectionId)
                .replace("{tileMatrixSetId}", tileMatrixSetId)
                .replace("{tileMatrix}", tileMatrix)
                .replace("{tileRow}", tileRow)
                .replace("{tileCol}", tileCol);
        TileFormatExtension outputFormat = api.getOutputFormat(TileFormatExtension.class, requestContext.getMediaType(), path, Optional.of(collectionId))
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
                    .apiData(apiData)
                    .outputFormat(outputFormat)
                    .temporary(false)
                    .isDatasetTile(false)
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

        // check, if the cache can be used (no query parameters except f)
        Map<String, String> queryParams = toFlatMap(uriInfo.getQueryParameters());
        boolean useCache = queryParams.isEmpty() || (queryParams.size()==1 && queryParams.containsKey("f"));

        Tile tile = new ImmutableTile.Builder()
                .collectionIds(ImmutableList.of(collectionId))
                .tileMatrixSet(tileMatrixSet)
                .tileLevel(level)
                .tileRow(row)
                .tileCol(col)
                .apiData(apiData)
                .temporary(!useCache)
                .isDatasetTile(false)
                .featureProvider(featureProvider)
                .outputFormat(outputFormat)
                .build();

        // if cache can be used and the tile is cached for the requested format, return the cache
        if (useCache) {
            // get the tile from the cache and return it
            Optional<InputStream> tileStream = null;
            try {
                tileStream = cache.getTile(tile);
            } catch (Exception e) {
                LOGGER.warn("Failed to retrieve tile {}/{}/{}/{} for collection {} from the cache. Reason: {}",
                            tile.getTileMatrixSet().getId(), tile.getTileLevel(), tile.getTileRow(),
                            tile.getTileCol(), collectionId, e.getMessage());
            }
            if (tileStream.isPresent()) {
                TilesQueriesHandler.QueryInputTileStream queryInput = new ImmutableQueryInputTileStream.Builder()
                        .from(getGenericQueryInput(api.getData()))
                        .tile(tile)
                        .tileContent(tileStream.get())
                        .build();

                return queryHandler.handle(TilesQueriesHandler.Query.TILE_STREAM, queryInput, requestContext);
            }
        }

        // don't store the tile in the cache if it is outside the range
        MinMax cacheMinMax = tilesConfiguration.getZoomLevelsCacheDerived()
                                               .get(tileMatrixSetId);
        Tile finalTile = Objects.isNull(cacheMinMax) || (level <= cacheMinMax.getMax() && level >= cacheMinMax.getMin()) ?
                tile :
                new ImmutableTile.Builder()
                        .from(tile)
                        .temporary(true)
                        .build();

        // first execute the information that is passed as processing parameters (e.g., "properties")
        Map<String, Object> processingParameters = new HashMap<>();
        for (OgcApiQueryParameter parameter : allowedParameters) {
            processingParameters = parameter.transformContext(null, processingParameters, queryParams, api.getData());
        }

        FeatureQuery query = outputFormat.getQuery(tile, allowedParameters, queryParams, tilesConfiguration, requestContext.getUriCustomizer());

        FeaturesCoreConfiguration coreConfiguration = featureType.getExtension(FeaturesCoreConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileSingleLayer queryInput = new ImmutableQueryInputTileSingleLayer.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tile(finalTile)
                .query(query)
                .processingParameters(processingParameters)
                .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.SINGLE_LAYER_TILE, queryInput, requestContext);
    }

    private void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
        if (!featureProvider.supportsQueries()) {
            throw new IllegalStateException("Feature provider does not support queries.");
        }
    }
}
