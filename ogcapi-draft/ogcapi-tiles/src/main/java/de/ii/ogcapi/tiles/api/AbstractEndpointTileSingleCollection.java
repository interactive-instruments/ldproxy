/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.api;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileStream.Builder;
import de.ii.ogcapi.tiles.domain.ImmutableTile;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileProvider;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEndpointTileSingleCollection extends EndpointSubCollection {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractEndpointTileSingleCollection.class);

  private final FeaturesCoreProviders providers;
  private final TilesQueriesHandler queryHandler;
  private final CrsTransformerFactory crsTransformerFactory;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileCache cache;
  private final StaticTileProviderStore staticTileProviderStore;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  public AbstractEndpointTileSingleCollection(
      FeaturesCoreProviders providers,
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      CrsTransformerFactory crsTransformerFactory,
      TileMatrixSetLimitsGenerator limitsGenerator,
      TileCache cache,
      StaticTileProviderStore staticTileProviderStore,
      TileMatrixSetRepository tileMatrixSetRepository) {
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
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    Optional<TilesConfiguration> config =
        apiData.getExtension(TilesConfiguration.class, collectionId);
    if (config.map(cfg -> !cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
      // Tiles are pre-generated as a static tile set
      return config.filter(ExtensionConfiguration::isEnabled).isPresent();
    } else {
      // Tiles are generated on-demand from a data source
      if (config
          .filter(TilesConfiguration::isEnabled)
          .filter(TilesConfiguration::isSingleCollectionEnabled)
          .isEmpty()) return false;
      // currently no vector tiles support for WFS backends
      return providers
          .getFeatureProvider(apiData, apiData.getCollections().get(collectionId))
          .map(FeatureProvider2::supportsHighLoad)
          .orElse(false);
    }
  }

  protected ApiEndpointDefinition computeDefinition(
      OgcApiDataV2 apiData,
      String apiEntrypoint,
      int sortPriority,
      String basePath,
      String subSubPath,
      List<String> tags) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint(apiEntrypoint)
            .sortPriority(sortPriority);
    final String path = basePath + subSubPath;
    final HttpMethods method = HttpMethods.GET;
    final List<OgcApiPathParameter> pathParameters =
        getPathParameters(extensionRegistry, apiData, path);
    final Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
    if (optCollectionIdParam.isEmpty()) {
      LOGGER.error(
          "Path parameter 'collectionId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
      boolean explode = collectionIdParam.isExplodeInOpenApi(apiData);
      final List<String> collectionIds =
          (explode) ? collectionIdParam.getValues(apiData) : ImmutableList.of("{collectionId}");
      for (String collectionId : collectionIds) {
        List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId);
        String operationSummary = "fetch a tile of the collection '" + collectionId + "'";
        Optional<String> operationDescription =
            Optional.of(
                "The tile in the requested tiling scheme ('{tileMatrixSetId}'), "
                    + "on the requested zoom level ('{tileMatrix}'), with the requested grid coordinates ('{tileRow}', '{tileCol}') is returned. "
                    + "The tile has a single layer with all selected features in the bounding box of the tile with the requested properties.");
        String resourcePath = path.replace("{collectionId}", collectionId);
        ImmutableOgcApiResourceData.Builder resourceBuilder =
            new ImmutableOgcApiResourceData.Builder()
                .path(resourcePath)
                .pathParameters(pathParameters);
        Map<MediaType, ApiMediaTypeContent> responseContent =
            collectionId.startsWith("{")
                ? getContent(apiData, Optional.empty(), subSubPath, HttpMethods.GET)
                : getContent(apiData, Optional.of(collectionId), subSubPath, HttpMethods.GET);
        ApiOperation.getResource(
                apiData,
                resourcePath,
                false,
                queryParameters,
                ImmutableList.of(),
                responseContent,
                operationSummary,
                operationDescription,
                Optional.empty(),
                tags)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }

    return definitionBuilder.build();
  }

  protected Response getTile(
      OgcApi api,
      ApiRequestContext requestContext,
      UriInfo uriInfo,
      String definitionPath,
      String collectionId,
      String tileMatrixSetId,
      String tileMatrix,
      String tileRow,
      String tileCol,
      TileProvider tileProvider)
      throws CrsTransformationException, IOException, NotFoundException {
    OgcApiDataV2 apiData = api.getData();
    Map<String, String> queryParams = toFlatMap(uriInfo.getQueryParameters());
    FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
    TilesConfiguration tilesConfiguration =
        featureType.getExtension(TilesConfiguration.class).orElseThrow();

    checkPathParameter(extensionRegistry, apiData, definitionPath, "collectionId", collectionId);
    checkPathParameter(
        extensionRegistry, apiData, definitionPath, "tileMatrixSetId", tileMatrixSetId);
    checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrix", tileMatrix);
    checkPathParameter(extensionRegistry, apiData, definitionPath, "tileRow", tileRow);
    checkPathParameter(extensionRegistry, apiData, definitionPath, "tileCol", tileCol);
    final List<OgcApiQueryParameter> allowedParameters =
        getQueryParameters(extensionRegistry, apiData, definitionPath, collectionId);

    int row;
    int col;
    int level;
    try {
      level = Integer.parseInt(tileMatrix);
      row = Integer.parseInt(tileRow);
      col = Integer.parseInt(tileCol);
    } catch (NumberFormatException e) {
      throw new ServerErrorException(
          "Could not convert tile coordinates that have been validated to integers", 500);
    }

    MinMax zoomLevels = tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId);
    if (zoomLevels.getMax() < level || zoomLevels.getMin() > level)
      throw new NotFoundException(
          "The requested tile is outside the zoom levels for this tile set.");

    TileMatrixSet tileMatrixSet =
        tileMatrixSetRepository
            .get(tileMatrixSetId)
            .orElseThrow(
                () -> new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId));

    TileMatrixSetLimits tileLimits =
        limitsGenerator
            .getCollectionTileMatrixSetLimits(api, collectionId, tileMatrixSet, zoomLevels)
            .stream()
            .filter(limits -> limits.getTileMatrix().equals(tileMatrix))
            .findAny()
            .orElse(null);

    if (tileLimits != null) {
      if (tileLimits.getMaxTileCol() < col
          || tileLimits.getMinTileCol() > col
          || tileLimits.getMaxTileRow() < row
          || tileLimits.getMinTileRow() > row)
        // return 404, if outside the range
        throw new NotFoundException(
            "The requested tile is outside of the limits for this zoom level and tile set.");
    }

    String path =
        definitionPath
            .replace("{collectionId}", collectionId)
            .replace("{tileMatrixSetId}", tileMatrixSetId)
            .replace("{tileMatrix}", tileMatrix)
            .replace("{tileRow}", tileRow)
            .replace("{tileCol}", tileCol);

    TileFormatExtension outputFormat =
        requestContext
            .getApi()
            .getOutputFormat(
                TileFormatExtension.class,
                requestContext.getMediaType(),
                path,
                Optional.of(collectionId))
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    Optional<FeatureProvider2> featureProvider = providers.getFeatureProvider(apiData, featureType);

    // check, if the cache can be used (no query parameters except f)
    boolean useCache =
        tileProvider.tilesMayBeCached()
            && tilesConfiguration.getCache() != TilesConfiguration.TileCacheType.NONE
            && (queryParams.isEmpty() || (queryParams.size() == 1 && queryParams.containsKey("f")));

    // don't store the tile in the cache if it is outside the range
    MinMax cacheMinMax = tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId);
    useCache =
        useCache
            && (Objects.isNull(cacheMinMax)
                || (level <= cacheMinMax.getMax() && level >= cacheMinMax.getMin()));

    Tile tile =
        new ImmutableTile.Builder()
            .tileMatrixSet(tileMatrixSet)
            .tileLevel(level)
            .tileRow(row)
            .tileCol(col)
            .api(api)
            .apiData(apiData)
            .outputFormat(outputFormat)
            .featureProvider(featureProvider)
            .collectionIds(ImmutableList.of(collectionId))
            .temporary(!useCache)
            .isDatasetTile(false)
            .build();

    QueryInput queryInput = null;

    // if cache can be used and the tile is cached for the requested format, return the cache
    if (useCache) {
      // get the tile from the cache and return it
      Optional<InputStream> tileStream = Optional.empty();
      try {
        tileStream = cache.getTile(tile);
      } catch (Exception e) {
        LOGGER.warn(
            "Failed to retrieve tile {}/{}/{}/{} for collection '{}' from the cache. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getTileLevel(),
            tile.getTileRow(),
            tile.getTileCol(),
            collectionId,
            e.getMessage());
      }
      if (tileStream.isPresent()) {
        queryInput =
            new Builder()
                .from(getGenericQueryInput(apiData))
                .tile(tile)
                .tileContent(tileStream.get())
                .build();
      }
    }

    // not cached or cache access failed
    if (Objects.isNull(queryInput))
      queryInput =
          tileProvider.getQueryInput(
              apiData,
              requestContext.getUriCustomizer(),
              queryParams,
              allowedParameters,
              getGenericQueryInput(apiData),
              tile);

    TilesQueriesHandler.Query query = null;
    if (queryInput instanceof TilesQueriesHandler.QueryInputTileMbtilesTile)
      query = TilesQueriesHandler.Query.MBTILES_TILE;
    else if (queryInput instanceof TilesQueriesHandler.QueryInputTileTileServerTile)
      query = TilesQueriesHandler.Query.TILESERVER_TILE;
    else if (queryInput instanceof TilesQueriesHandler.QueryInputTileEmpty)
      query = TilesQueriesHandler.Query.EMPTY_TILE;
    else if (queryInput instanceof TilesQueriesHandler.QueryInputTileStream)
      query = TilesQueriesHandler.Query.TILE_STREAM;
    else if (queryInput instanceof TilesQueriesHandler.QueryInputTileMultiLayer)
      query = TilesQueriesHandler.Query.MULTI_LAYER_TILE;
    else if (queryInput instanceof TilesQueriesHandler.QueryInputTileSingleLayer)
      query = TilesQueriesHandler.Query.SINGLE_LAYER_TILE;

    return queryHandler.handle(query, queryInput, requestContext);
  }

  private void ensureFeatureProviderSupportsQueries(FeatureProvider2 featureProvider) {
    if (!featureProvider.supportsQueries()) {
      throw new IllegalStateException("Feature provider does not support queries.");
    }
  }
}
