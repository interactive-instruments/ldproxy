/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiBackgroundTask;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.ParameterExtension;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileMultiLayer;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileSingleLayer;
import de.ii.ogcapi.tiles.domain.ImmutableTile;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.ogcapi.tiles.domain.SeedingOptions;
import de.ii.ogcapi.tiles.domain.Tile;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimits;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureTypeConfiguration;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for a automatic generation of the Tiles. The range is specified in the
 * config. The automatic generation is executed, when the server is started/restarted.
 */
@Singleton
@AutoBind
public class VectorTileSeeding implements OgcApiBackgroundTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(VectorTileSeeding.class);

  private final CrsTransformerFactory crsTransformerFactory;
  private final ExtensionRegistry extensionRegistry;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileCache tileCache;
  private final URI servicesUri;
  private final FeaturesCoreProviders providers;
  private final TilesQueriesHandler queryHandler;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  public VectorTileSeeding(
      CrsTransformerFactory crsTransformerFactory,
      ExtensionRegistry extensionRegistry,
      TileMatrixSetLimitsGenerator limitsGenerator,
      TileCache tileCache,
      ServicesContext servicesContext,
      FeaturesCoreProviders providers,
      TilesQueriesHandler queryHandler,
      TileMatrixSetRepository tileMatrixSetRepository) {
    this.crsTransformerFactory = crsTransformerFactory;
    this.extensionRegistry = extensionRegistry;
    this.limitsGenerator = limitsGenerator;
    this.tileCache = tileCache;
    this.servicesUri = servicesContext.getUri();
    this.providers = providers;
    this.queryHandler = queryHandler;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    if (!apiData.getEnabled()) {
      return false;
    }
    // no vector tiles support for WFS backends
    if (!providers
        .getFeatureProvider(apiData)
        .map(FeatureProvider2::supportsHighLoad)
        .orElse(false)) {
      return false;
    }

    // no formats available
    if (extensionRegistry
        .getExtensionsForType(TileFormatWithQuerySupportExtension.class)
        .isEmpty()) {
      return false;
    }

    return apiData
        .getExtension(TilesConfiguration.class)
        .filter(TilesConfiguration::isEnabled)
        // seeding only for features as tile providers
        .filter(config -> config.getTileProvider().requiresQuerySupport())
        .filter(cfg -> cfg.getCache() != TilesConfiguration.TileCacheType.NONE)
        .filter(config -> !config.getSeedingDerived().isEmpty())
        .isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  public Class<OgcApi> getServiceType() {
    return OgcApi.class;
  }

  @Override
  public String getLabel() {
    return "Tile cache seeding";
  }

  @Override
  public boolean runOnStart(OgcApi api) {
    return isEnabledForApi(api.getData())
        && api.getData()
            .getExtension(TilesConfiguration.class)
            .flatMap(TilesConfiguration::getSeedingOptions)
            .filter(seedingOptions -> !seedingOptions.shouldRunOnStartup())
            .isEmpty();
  }

  @Override
  public Optional<String> runPeriodic(OgcApi api) {
    if (!isEnabledForApi(api.getData())) {
      return Optional.empty();
    }
    return api.getData()
        .getExtension(TilesConfiguration.class)
        .flatMap(TilesConfiguration::getSeedingOptions)
        .flatMap(SeedingOptions::getCronExpression);
  }

  @Override
  public int getMaxPartials(OgcApi api) {
    return api.getData()
        .getExtension(TilesConfiguration.class)
        .flatMap(TilesConfiguration::getSeedingOptions)
        .map(SeedingOptions::getEffectiveMaxThreads)
        .orElse(1);
  }

  private boolean shouldPurge(OgcApi api) {
    return api.getData()
        .getExtension(TilesConfiguration.class)
        .flatMap(TilesConfiguration::getSeedingOptions)
        .filter(SeedingOptions::shouldPurge)
        .isPresent();
  }

  /**
   * Run the seeding
   *
   * @param api
   * @param taskContext
   */
  @Override
  public void run(OgcApi api, TaskContext taskContext) {
    if (shouldPurge(api) && taskContext.isFirstPartial()) {
      try {
        taskContext.setStatusMessage("purging cache");
        tileCache.deleteTiles(api, Optional.empty(), Optional.empty(), Optional.empty());
        taskContext.setStatusMessage("purged cache successfully");
      } catch (IOException | SQLException e) {
        LOGGER.debug("{}: purging failed | {}", getLabel(), e.getMessage());
      }
    }

    List<TileFormatWithQuerySupportExtension> outputFormats =
        extensionRegistry.getExtensionsForType(TileFormatWithQuerySupportExtension.class);

    try {
      // first seed the multi-layer tiles, which also generates the necessary single-layer tiles
      if (!taskContext.isStopped()) seedMultiLayerTiles(api, outputFormats, taskContext);

      // add any additional single-layer tiles
      if (!taskContext.isStopped()) seedSingleLayerTiles(api, outputFormats, taskContext);

    } catch (IOException e) {
      if (!taskContext.isStopped()) {
        throw new RuntimeException("Error accessing the tile cache during seeding.", e);
      }
    } catch (Throwable e) {
      // in general, this should only happen on shutdown (as we cannot influence shutdown order,
      // exceptions
      // during seeding on shutdown are currently inevitable), but for other situations we still add
      // the error
      // to the log
      if (!taskContext.isStopped()) {
        throw new RuntimeException(
            "An error occurred during seeding. Note that this may be a side-effect of a server shutdown.",
            e);
      }
    }
  }

  private void seedSingleLayerTiles(
      OgcApi api, List<TileFormatWithQuerySupportExtension> outputFormats, TaskContext taskContext)
      throws IOException {
    OgcApiDataV2 apiData = api.getData();
    // isEnabled checks that we have a feature provider
    FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(apiData);
    Map<String, Map<String, MinMax>> seedingMap = getSeedingConfig(apiData);

    long numberOfTiles = getNumberOfTiles2(api, outputFormats, seedingMap, taskContext);
    final double[] currentTile = {0.0};

    walkCollectionsAndTiles(
        api,
        outputFormats,
        seedingMap,
        taskContext,
        (api1, collectionId, outputFormat, tileMatrixSet, level, row, col) -> {
          TilesConfiguration tilesConfiguration =
              getTilesConfiguration(apiData, collectionId).get();
          Tile tile =
              new ImmutableTile.Builder()
                  .collectionIds(ImmutableList.of(collectionId))
                  .tileMatrixSet(tileMatrixSet)
                  .tileLevel(level)
                  .tileRow(row)
                  .tileCol(col)
                  .api(api)
                  .apiData(apiData)
                  .temporary(false)
                  .isDatasetTile(false)
                  .featureProvider(featureProvider)
                  .outputFormat(outputFormat)
                  .build();
          try {
            if (tileCache.tileExists(tile)) {
              // already there, nothing to create, but advance progress
              currentTile[0] += 1;
              return true;
            }
          } catch (Exception e) {
            LOGGER.warn(
                "Failed to retrieve tile {}/{}/{}/{} for collection {} from the cache. Reason: {}",
                tile.getTileMatrixSet().getId(),
                tile.getTileLevel(),
                tile.getTileRow(),
                tile.getTileCol(),
                collectionId,
                e.getMessage());
          }

          URI uri;
          String uriString =
              String.format(
                  "%s/%s/collections/%s/tiles/%s/%s/%s/%s",
                  servicesUri,
                  apiData.getId(),
                  collectionId,
                  tileMatrixSet.getId(),
                  level,
                  row,
                  col);
          try {
            uri = new URI(uriString);
          } catch (URISyntaxException e) {
            LOGGER.error("Stopping seeding. Invalid request URI during seeding: " + uriString);
            return false;
          }

          URICustomizer uriCustomizer = new URICustomizer(uri);
          ApiRequestContext requestContext =
              new ImmutableRequestContext.Builder()
                  .api(api)
                  .requestUri(uri)
                  .mediaType(outputFormat.getMediaType())
                  .build();

          FeatureQuery query =
              outputFormat.getQuery(
                  tile, ImmutableList.of(), ImmutableMap.of(), tilesConfiguration, uriCustomizer);

          FeaturesCoreConfiguration coreConfiguration =
              apiData.getExtension(FeaturesCoreConfiguration.class).get();

          // skip collections without spatial queryable
          if (coreConfiguration.getQueryables().isEmpty()
              || coreConfiguration.getQueryables().get().getSpatial().isEmpty()) return true;

          TilesQueriesHandler.QueryInputTileSingleLayer queryInput =
              new ImmutableQueryInputTileSingleLayer.Builder()
                  .tile(tile)
                  .query(query)
                  .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                  .build();

          taskContext.setStatusMessage(
              String.format(
                  "currently processing -> %s, %s/%s/%s/%s, %s",
                  collectionId,
                  tileMatrixSet.getId(),
                  level,
                  row,
                  col,
                  outputFormat.getExtension()));

          try {
            queryHandler.handle(
                TilesQueriesHandler.Query.SINGLE_LAYER_TILE, queryInput, requestContext);
          } catch (Throwable e) {
            LOGGER.warn(
                "{}: processing failed -> {}, {}/{}/{}/{}, {} | {}",
                getLabel(),
                collectionId,
                tileMatrixSet.getId(),
                level,
                row,
                col,
                outputFormat.getExtension(),
                e.getMessage());
            if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE))
              LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
          }

          currentTile[0] += 1;
          taskContext.setCompleteness(currentTile[0] / numberOfTiles);

          return !taskContext.isStopped();
        });
  }

  private void seedMultiLayerTiles(
      OgcApi api, List<TileFormatWithQuerySupportExtension> outputFormats, TaskContext taskContext)
      throws IOException {
    OgcApiDataV2 apiData = api.getData();
    // isEnabled checks that we have a feature provider
    FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(apiData);
    Map<String, MinMax> multiLayerTilesSeeding = ImmutableMap.of();
    Optional<TilesConfiguration> tilesConfiguration =
        apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isMultiCollectionEnabled);

    if (tilesConfiguration.isPresent()) {
      Map<String, MinMax> seedingConfig = tilesConfiguration.get().getEffectiveSeeding();
      if (seedingConfig != null && !seedingConfig.isEmpty()) multiLayerTilesSeeding = seedingConfig;
    }

    List<TileFormatWithQuerySupportExtension> multiLayerFormats =
        outputFormats.stream()
            .filter(TileFormatWithQuerySupportExtension::canMultiLayer)
            .collect(Collectors.toList());

    long numberOfTiles =
        getNumberOfTiles(api, multiLayerFormats, multiLayerTilesSeeding, taskContext);
    final double[] currentTile = {0.0};

    walkTiles(
        api,
        "multi-layer",
        multiLayerFormats,
        multiLayerTilesSeeding,
        taskContext,
        (api1, layerName, outputFormat, tileMatrixSet, level, row, col) -> {
          List<String> collectionIds =
              apiData.getCollections().values().stream()
                  .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                  // skip collections without spatial queryable
                  .filter(
                      collection -> {
                        Optional<FeaturesCoreConfiguration> featuresConfiguration =
                            collection.getExtension(FeaturesCoreConfiguration.class);
                        return featuresConfiguration.isPresent()
                            && featuresConfiguration.get().getQueryables().isPresent()
                            && !featuresConfiguration
                                .get()
                                .getQueryables()
                                .get()
                                .getSpatial()
                                .isEmpty();
                      })
                  .filter(
                      collection -> {
                        Optional<TilesConfiguration> layerConfiguration =
                            collection.getExtension(TilesConfiguration.class);
                        if (layerConfiguration.isEmpty()
                            || !layerConfiguration.get().isEnabled()
                            || !layerConfiguration.get().isMultiCollectionEnabled()) return false;
                        MinMax levels =
                            layerConfiguration
                                .get()
                                .getZoomLevelsDerived()
                                .get(tileMatrixSet.getId());
                        return !Objects.nonNull(levels)
                            || (levels.getMax() >= level && levels.getMin() <= level);
                      })
                  .map(FeatureTypeConfiguration::getId)
                  .collect(Collectors.toList());

          if (collectionIds.isEmpty()) {
            // nothing to generate, still advance progress
            currentTile[0] += 1;
            return true;
          }

          Tile multiLayerTile =
              new ImmutableTile.Builder()
                  .collectionIds(collectionIds)
                  .tileMatrixSet(tileMatrixSet)
                  .tileLevel(level)
                  .tileRow(row)
                  .tileCol(col)
                  .api(api)
                  .apiData(apiData)
                  .temporary(false)
                  .isDatasetTile(true)
                  .featureProvider(featureProvider)
                  .outputFormat(outputFormat)
                  .build();
          try {
            if (tileCache.tileExists(multiLayerTile)) {
              // already there, nothing to create, but still count for progress
              currentTile[0] += 1;
              return true;
            }
          } catch (Exception e) {
            LOGGER.warn(
                "Failed to retrieve multi-collection tile {}/{}/{}/{} from the cache. Reason: {}",
                multiLayerTile.getTileMatrixSet().getId(),
                multiLayerTile.getTileLevel(),
                multiLayerTile.getTileRow(),
                multiLayerTile.getTileCol(),
                e.getMessage());
          }

          URI uri;
          String uriString =
              String.format(
                  "%s/%s/tiles/%s/%s/%s/%s",
                  servicesUri, apiData.getId(), tileMatrixSet.getId(), level, row, col);
          try {
            uri = new URI(uriString);
          } catch (URISyntaxException e) {
            LOGGER.error("Stopping seeding. Invalid request URI during seeding: " + uriString);
            return false;
          }

          ApiRequestContext requestContext =
              new ImmutableRequestContext.Builder()
                  .api(api)
                  .requestUri(uri)
                  .mediaType(outputFormat.getMediaType())
                  .build();

          Map<String, Tile> singleLayerTileMap =
              collectionIds.stream()
                  .collect(
                      ImmutableMap.toImmutableMap(
                          collectionId -> collectionId,
                          collectionId ->
                              new ImmutableTile.Builder()
                                  .from(multiLayerTile)
                                  .collectionIds(ImmutableList.of(collectionId))
                                  .isDatasetTile(false)
                                  .build()));

          Map<String, FeatureQuery> queryMap =
              collectionIds.stream()
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
                            List<OgcApiQueryParameter> allowedParameters =
                                extensionRegistry
                                    .getExtensionsForType(OgcApiQueryParameter.class)
                                    .stream()
                                    .filter(
                                        param ->
                                            param.isApplicable(
                                                apiData,
                                                "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                                                collectionId,
                                                HttpMethods.GET))
                                    .sorted(Comparator.comparing(ParameterExtension::getName))
                                    .collect(ImmutableList.toImmutableList());
                            TilesConfiguration layerConfiguration =
                                apiData
                                    .getCollections()
                                    .get(collectionId)
                                    .getExtension(TilesConfiguration.class)
                                    .orElse(tilesConfiguration.get());
                            FeatureQuery query =
                                outputFormat.getQuery(
                                    singleLayerTileMap.get(collectionId),
                                    allowedParameters,
                                    ImmutableMap.of(),
                                    layerConfiguration,
                                    requestContext.getUriCustomizer());
                            return ImmutableFeatureQuery.builder()
                                .from(query)
                                .type(featureTypeId)
                                .build();
                          }));

          FeaturesCoreConfiguration coreConfiguration =
              apiData.getExtension(FeaturesCoreConfiguration.class).get();

          TilesQueriesHandler.QueryInputTileMultiLayer queryInput =
              new ImmutableQueryInputTileMultiLayer.Builder()
                  .tile(multiLayerTile)
                  .singleLayerTileMap(singleLayerTileMap)
                  .queryMap(queryMap)
                  .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
                  .build();

          taskContext.setStatusMessage(
              String.format(
                  "currently processing -> %s, %s/%s/%s/%s, %s",
                  layerName, tileMatrixSet.getId(), level, row, col, outputFormat.getExtension()));

          try {
            queryHandler.handle(
                TilesQueriesHandler.Query.MULTI_LAYER_TILE, queryInput, requestContext);
          } catch (Throwable e) {
            LOGGER.warn(
                "{}: processing failed -> {}, {}/{}/{}/{}, {} | {}",
                getLabel(),
                layerName,
                tileMatrixSet.getId(),
                level,
                row,
                col,
                outputFormat.getExtension(),
                e.getMessage());
            if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE))
              LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
          }

          currentTile[0] += 1;
          taskContext.setCompleteness(currentTile[0] / numberOfTiles);

          return !taskContext.isStopped();
        });
  }

  /**
   * checks if the tiles extension is available and returns a Map with entries for each collection
   * and their zoomLevel or seeding
   *
   * @param apiData the service data of the API
   * @return a map with all collectionIds and the seeding configuration
   */
  private Map<String, Map<String, MinMax>> getSeedingConfig(OgcApiDataV2 apiData) {
    Map<String, Map<String, MinMax>> minMaxMap = new HashMap<>();

    for (FeatureTypeConfigurationOgcApi featureType : apiData.getCollections().values()) {
      final Optional<TilesConfiguration> tilesConfiguration =
          featureType.getExtension(TilesConfiguration.class);

      if (tilesConfiguration.isPresent()) {
        Map<String, MinMax> seedingConfig = tilesConfiguration.get().getEffectiveSeeding();
        if (seedingConfig != null && !seedingConfig.isEmpty())
          minMaxMap.put(featureType.getId(), seedingConfig);
      }
    }
    return minMaxMap;
  }

  private long getNumberOfTiles2(
      OgcApi api,
      List<TileFormatWithQuerySupportExtension> outputFormats,
      Map<String, Map<String, MinMax>> seeding,
      TaskContext taskContext) {
    final long[] numberOfTiles = {0};

    try {
      walkCollectionsAndTiles(
          api,
          outputFormats,
          seeding,
          taskContext,
          (ignore1, collectionId, ignore2, ignore3, ignore4, ignore5, ignore6) -> {
            numberOfTiles[0]++;
            return true;
          });
    } catch (IOException e) {
      // ignore
    }

    return numberOfTiles[0];
  }

  private long getNumberOfTiles(
      OgcApi api,
      List<TileFormatWithQuerySupportExtension> outputFormats,
      Map<String, MinMax> seeding,
      TaskContext taskContext) {
    final long[] numberOfTiles = {0};

    try {
      walkTiles(
          api,
          "",
          outputFormats,
          seeding,
          taskContext,
          (ignore1, collectionId, ignore2, ignore3, ignore4, ignore5, ignore6) -> {
            numberOfTiles[0]++;
            return true;
          });
    } catch (IOException e) {
      // ignore
    }

    return numberOfTiles[0];
  }

  interface TileWalker {
    boolean visit(
        OgcApi api,
        String collectionId,
        TileFormatWithQuerySupportExtension outputFormat,
        TileMatrixSet tileMatrixSet,
        int level,
        int row,
        int col)
        throws IOException;
  }

  private void walkCollectionsAndTiles(
      OgcApi api,
      List<TileFormatWithQuerySupportExtension> outputFormats,
      Map<String, Map<String, MinMax>> seeding,
      TaskContext taskContext,
      TileWalker tileWalker)
      throws IOException {
    for (Map.Entry<String, Map<String, MinMax>> entry : seeding.entrySet()) {
      String collectionId = entry.getKey();
      Map<String, MinMax> seedingConfig = entry.getValue();
      Optional<TilesConfiguration> tilesConfiguration =
          getTilesConfiguration(api.getData(), collectionId);
      if (tilesConfiguration.isPresent()) {
        walkTiles(api, collectionId, outputFormats, seedingConfig, taskContext, tileWalker);
      }
    }
  }

  private void walkTiles(
      OgcApi api,
      String collectionId,
      List<TileFormatWithQuerySupportExtension> outputFormats,
      Map<String, MinMax> seeding,
      TaskContext taskContext,
      TileWalker tileWalker)
      throws IOException {
    for (TileFormatWithQuerySupportExtension outputFormat : outputFormats) {
      for (Map.Entry<String, MinMax> entry : seeding.entrySet()) {
        TileMatrixSet tileMatrixSet = getTileMatrixSetById(entry.getKey());
        MinMax zoomLevels = entry.getValue();
        List<TileMatrixSetLimits> allLimits =
            limitsGenerator.getTileMatrixSetLimits(api, tileMatrixSet, zoomLevels);

        for (TileMatrixSetLimits limits : allLimits) {
          int level = Integer.parseInt(limits.getTileMatrix());

          for (int row = limits.getMinTileRow(); row <= limits.getMaxTileRow(); row++) {
            for (int col = limits.getMinTileCol(); col <= limits.getMaxTileCol(); col++) {
              if (taskContext.isPartial() && !taskContext.matchesPartialModulo(col)) {
                continue;
              }
              boolean shouldContinue =
                  tileWalker.visit(api, collectionId, outputFormat, tileMatrixSet, level, row, col);
              if (!shouldContinue) {
                return;
              }
            }
          }
        }
      }
    }
  }

  private Optional<TilesConfiguration> getTilesConfiguration(
      OgcApiDataV2 apiData, String collectionId) {
    return Optional.ofNullable(apiData.getCollections().get(collectionId))
        .flatMap(featureType -> featureType.getExtension(TilesConfiguration.class))
        .filter(TilesConfiguration::isEnabled);
  }

  private TileMatrixSet getTileMatrixSetById(String tileMatrixSetId) {
    return tileMatrixSetRepository.get(tileMatrixSetId).orElse(null);
  }
}
