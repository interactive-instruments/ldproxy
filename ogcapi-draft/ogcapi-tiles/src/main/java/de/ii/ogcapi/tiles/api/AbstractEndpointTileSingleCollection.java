/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
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
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileEmpty;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileMbtilesTile;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileMultiLayer;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileSingleLayer;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileTileServerTile;
import de.ii.ogcapi.tiles.domain.ImmutableTile;
import de.ii.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TileProvider;
import de.ii.ogcapi.tiles.domain.TileProviderFeatures;
import de.ii.ogcapi.tiles.domain.TileProviderMbtiles;
import de.ii.ogcapi.tiles.domain.TileProviderTileServer;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
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
          .getFeatureProvider(apiData)
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
      Optional<String> collectionId,
      String tileMatrixSetId,
      String tileLevel,
      String tileRow,
      String tileCol)
      throws CrsTransformationException, IOException, NotFoundException {
    OgcApiDataV2 apiData = api.getData();
    Optional<FeatureTypeConfigurationOgcApi> collectionData =
        collectionId.map(id -> apiData.getCollections().get(id));
    TilesConfiguration tilesConfiguration =
        collectionData.isPresent()
            ? collectionData.get().getExtension(TilesConfiguration.class).orElseThrow()
            : apiData.getExtension(TilesConfiguration.class).orElseThrow();
    Map<String, String> parameterValues = toFlatMap(uriInfo.getQueryParameters());
    final List<OgcApiQueryParameter> parameterDefinitions =
        collectionId.isPresent()
            ? getQueryParameters(extensionRegistry, apiData, definitionPath, collectionId.get())
            : getQueryParameters(extensionRegistry, apiData, definitionPath);

    if (collectionId.isPresent()) {
      checkPathParameter(
          extensionRegistry, apiData, definitionPath, "collectionId", collectionId.get());
    }
    checkPathParameter(
        extensionRegistry, apiData, definitionPath, "tileMatrixSetId", tileMatrixSetId);
    checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrix", tileLevel);
    checkPathParameter(extensionRegistry, apiData, definitionPath, "tileRow", tileRow);
    checkPathParameter(extensionRegistry, apiData, definitionPath, "tileCol", tileCol);

    int row;
    int col;
    int level;
    try {
      level = Integer.parseInt(tileLevel);
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
        limitsGenerator.getTileMatrixSetLimits(api, tileMatrixSet, level, collectionId);

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
            .replace("{collectionId}", collectionId.orElse(""))
            .replace("{tileMatrixSetId}", tileMatrixSetId)
            .replace("{tileMatrix}", tileLevel)
            .replace("{tileRow}", tileRow)
            .replace("{tileCol}", tileCol);

    TileFormatExtension outputFormat =
        requestContext
            .getApi()
            .getOutputFormat(
                TileFormatExtension.class, requestContext.getMediaType(), path, collectionId)
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    QueryInput queryInput =
        new ImmutableQueryInputTileSingleLayer.Builder()
            .from(getGenericQueryInput(apiData))
            .collectionId(collectionId)
            .outputFormat(outputFormat)
            .tileMatrixSet(tileMatrixSet)
            .tileLevel(level)
            .tileRow(row)
            .tileCol(col)
            .parameters(
                QueryParameterSet.of(parameterDefinitions, parameterValues)
                    .hydrate(apiData, collectionData))
            .build();

    return queryHandler.handle(
        TilesQueriesHandler.Query.SINGLE_LAYER_TILE, queryInput, requestContext);
  }

  public static QueryInput getQueryInput(
      TileProvider tileProviderData,
      OgcApiDataV2 apiData,
      URICustomizer uriCustomizer,
      Map<String, String> queryParameters,
      List<OgcApiQueryParameter> allowedParameters,
      QueryInput genericInput,
      Tile tile) {

    if (tileProviderData instanceof TileProviderMbtiles) {
      return new ImmutableQueryInputTileMbtilesTile.Builder()
          .from(genericInput)
          .tile(tile)
          .provider((TileProviderMbtiles) tileProviderData)
          .build();
    } else if (tileProviderData instanceof TileProviderTileServer) {
      return new ImmutableQueryInputTileTileServerTile.Builder()
          .from(genericInput)
          .tile(tile)
          .provider((TileProviderTileServer) tileProviderData)
          .build();
    } else if (tileProviderData instanceof TileProviderFeatures) {
      if (!tile.getFeatureProvider().map(FeatureProvider2::supportsQueries).orElse(false)) {
        throw new IllegalStateException(
            "Tile cannot be generated. The feature provider does not support feature queries.");
      }

      TileFormatExtension outputFormat = tile.getOutputFormat();
      List<String> collections = tile.getCollectionIds();

      if (collections.isEmpty()) {
        return new ImmutableQueryInputTileEmpty.Builder().from(genericInput).tile(tile).build();
      }

      if (!(outputFormat instanceof TileFormatWithQuerySupportExtension))
        throw new RuntimeException(
            String.format(
                "Unexpected tile format without query support. Found: %s",
                outputFormat.getClass().getSimpleName()));

      if (tile.isDatasetTile()) {
        if (!outputFormat.canMultiLayer() && collections.size() > 1)
          throw new NotAcceptableException(
              "The requested tile format supports only a single layer. Please select only a single collection.");

        Map<String, Tile> singleLayerTileMap =
            collections.stream()
                .collect(
                    ImmutableMap.toImmutableMap(
                        collectionId -> collectionId,
                        collectionId ->
                            new ImmutableTile.Builder()
                                .from(tile)
                                .collectionIds(ImmutableList.of(collectionId))
                                .isDatasetTile(false)
                                .build()));

        Map<String, FeatureQuery> queryMap =
            collections.stream()
                .collect(
                    ImmutableMap.toImmutableMap(
                        collectionId -> collectionId,
                        collectionId -> {
                          String featureTypeId =
                              apiData
                                  .getCollections()
                                  .get(collectionId)
                                  .getExtension(FeaturesCoreConfiguration.class)
                                  .map(cfg -> cfg.getFeatureType().orElse(collectionId))
                                  .orElse(collectionId);
                          TilesConfiguration layerConfiguration =
                              apiData
                                  .getExtension(TilesConfiguration.class, collectionId)
                                  .orElseThrow();
                          FeatureQuery query =
                              ((TileFormatWithQuerySupportExtension) outputFormat)
                                  .getQuery(
                                      singleLayerTileMap.get(collectionId),
                                      allowedParameters,
                                      queryParameters,
                                      layerConfiguration,
                                      uriCustomizer);
                          return ImmutableFeatureQuery.builder()
                              .from(query)
                              .type(featureTypeId)
                              .build();
                        }));

        FeaturesCoreConfiguration coreConfiguration =
            apiData.getExtension(FeaturesCoreConfiguration.class).orElseThrow();

        return new ImmutableQueryInputTileMultiLayer.Builder()
            .from(genericInput)
            .tile(tile)
            .singleLayerTileMap(singleLayerTileMap)
            .queryMap(queryMap)
            .defaultCrs(
                apiData
                    .getExtension(FeaturesCoreConfiguration.class)
                    .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                    .orElseThrow())
            .build();
      }
    }

    return null;
  }
}
