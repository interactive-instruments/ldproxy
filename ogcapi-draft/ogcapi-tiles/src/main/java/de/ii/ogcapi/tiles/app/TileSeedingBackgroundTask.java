/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.DATASET_TILES;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiBackgroundTask;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.domain.SeedingOptions;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.services.domain.ServicesContext;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class TileSeedingBackgroundTask implements OgcApiBackgroundTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileSeedingBackgroundTask.class);

  private final ExtensionRegistry extensionRegistry;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final URI servicesUri;
  private final FeaturesCoreProviders providers;
  private final TilesProviders tilesProviders;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  public TileSeedingBackgroundTask(
      ExtensionRegistry extensionRegistry,
      TileMatrixSetLimitsGenerator limitsGenerator,
      ServicesContext servicesContext,
      FeaturesCoreProviders providers,
      TilesProviders tilesProviders,
      TileMatrixSetRepository tileMatrixSetRepository) {
    this.extensionRegistry = extensionRegistry;
    this.limitsGenerator = limitsGenerator;
    this.servicesUri = servicesContext.getUri();
    this.providers = providers;
    this.tilesProviders = tilesProviders;
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
      taskContext.setStatusMessage("purging cache");
      tilesProviders.deleteTiles(api, Optional.empty(), Optional.empty(), Optional.empty());
      taskContext.setStatusMessage("purged cache successfully");
    }

    List<TileFormatExtension> outputFormats =
        extensionRegistry.getExtensionsForType(TileFormatExtension.class);

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
      // exceptions during seeding on shutdown are currently inevitable), but for other situations
      // we still add the error to the log
      if (!taskContext.isStopped()) {
        throw new RuntimeException(
            "An error occurred during seeding. Note that this may be a side-effect of a server shutdown.",
            e);
      }
    }
  }

  private void seedSingleLayerTiles(
      OgcApi api, List<TileFormatExtension> outputFormats, TaskContext taskContext)
      throws IOException {
    OgcApiDataV2 apiData = api.getData();
    Map<String, Map<String, MinMax>> seedingMap = getSeedingConfig(apiData);

    // TODO: isEnabled should check that we have a tile provider
    // TODO: different tile provider per collection
    TileProvider tileProvider = tilesProviders.getTileProviderOrThrow(apiData);
    List<TileFormatExtension> seedingFormats =
        outputFormats.stream()
            .filter(format -> tileProvider.generator().supports(format.getMediaType().type()))
            .collect(Collectors.toList());

    long numberOfTiles = getNumberOfTiles2(api, seedingFormats, seedingMap, taskContext);
    final double[] currentTile = {0.0};

    walkCollectionsAndTiles(
        api,
        seedingFormats,
        seedingMap,
        taskContext,
        (api1, collectionId, outputFormat, tileMatrixSet, level, row, col) -> {
          // skip collections without layer
          if (!tileProvider.getData().getLayers().containsKey(collectionId)) {
            return true;
          }

          URI uri =
              URI.create(
                  String.format(
                      "%s/%s/collections/%s/tiles/%s/%s/%s/%s",
                      servicesUri,
                      apiData.getId(),
                      collectionId,
                      tileMatrixSet.getId(),
                      level,
                      row,
                      col));

          ApiRequestContext requestContext =
              new ImmutableRequestContext.Builder()
                  .api(api)
                  .requestUri(uri)
                  .mediaType(outputFormat.getMediaType())
                  .build();

          TileQuery tileQuery =
              ImmutableTileQuery.builder()
                  .layer(collectionId)
                  .mediaType(outputFormat.getMediaType().type())
                  .tileMatrixSet(tileMatrixSet)
                  .level(level)
                  .row(row)
                  .col(col)
                  .build();
          ImmutableTileQuery.Builder tileQueryBuilder =
              ImmutableTileQuery.builder().from(tileQuery);
          tileQueryBuilder
              .generationParametersBuilder()
              .clipBoundingBox(
                  api1.getSpatialExtent(collectionId, tileQuery.getBoundingBox().getEpsgCrs()))
              .propertyTransformations(
                  api1.getData()
                      .getCollectionData(collectionId)
                      .flatMap(cd -> cd.getExtension(FeaturesCoreConfiguration.class))
                      .map(
                          pt ->
                              pt.withSubstitutions(
                                  FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                                      requestContext.getApiUri()))));

          TileQuery tile = tileQueryBuilder.build();

          taskContext.setStatusMessage(
              String.format(
                  "currently processing -> %s, %s/%s/%s/%s, %s",
                  collectionId,
                  tileMatrixSet.getId(),
                  level,
                  row,
                  col,
                  outputFormat.getExtension()));

          TileResult result = tileProvider.getTile(tile);

          if (result.isError()) {
            LOGGER.warn(
                "{}: processing failed -> {}, {}/{}/{}/{}, {} | {}",
                getLabel(),
                collectionId,
                tileMatrixSet.getId(),
                level,
                row,
                col,
                outputFormat.getExtension(),
                result.getError().get());
          }

          currentTile[0] += 1;
          taskContext.setCompleteness(currentTile[0] / numberOfTiles);

          return !taskContext.isStopped();
        });
  }

  private void seedMultiLayerTiles(
      OgcApi api, List<TileFormatExtension> outputFormats, TaskContext taskContext)
      throws IOException {
    OgcApiDataV2 apiData = api.getData();
    Map<String, MinMax> seedingConfig =
        apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isMultiCollectionEnabled)
            .map(TilesConfiguration::getEffectiveSeeding)
            .orElse(Map.of());

    // TODO: isEnabled should check that we have a tile provider
    // TODO: different tile provider per collection
    TileProvider tileProvider = tilesProviders.getTileProviderOrThrow(apiData);
    List<TileFormatExtension> seedingFormats =
        outputFormats.stream()
            .filter(format -> tileProvider.generator().supports(format.getMediaType().type()))
            .collect(Collectors.toList());

    long numberOfTiles = getNumberOfTiles(api, seedingFormats, seedingConfig, taskContext);
    final double[] currentTile = {0.0};

    walkTiles(
        api,
        DATASET_TILES,
        seedingFormats,
        seedingConfig,
        taskContext,
        (api1, layerName, outputFormat, tileMatrixSet, level, row, col) -> {
          // skip collections without layer
          if (!tileProvider.getData().getLayers().containsKey(DATASET_TILES)) {
            return true;
          }

          URI uri =
              URI.create(
                  String.format(
                      "%s/%s/tiles/%s/%s/%s/%s",
                      servicesUri, apiData.getId(), tileMatrixSet.getId(), level, row, col));

          ApiRequestContext requestContext =
              new ImmutableRequestContext.Builder()
                  .api(api)
                  .requestUri(uri)
                  .mediaType(outputFormat.getMediaType())
                  .build();

          TileQuery tileQuery =
              ImmutableTileQuery.builder()
                  .layer(layerName)
                  .mediaType(outputFormat.getMediaType().type())
                  .tileMatrixSet(tileMatrixSet)
                  .level(level)
                  .row(row)
                  .col(col)
                  .build();
          ImmutableTileQuery.Builder tileQueryBuilder =
              ImmutableTileQuery.builder().from(tileQuery);
          tileQueryBuilder
              .generationParametersBuilder()
              .clipBoundingBox(api1.getSpatialExtent(tileQuery.getBoundingBox().getEpsgCrs()))
              .propertyTransformations(
                  api1.getData()
                      .getExtension(FeaturesCoreConfiguration.class)
                      .map(
                          pt ->
                              pt.withSubstitutions(
                                  FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                                      requestContext.getApiUri()))));

          TileQuery tile = tileQueryBuilder.build();

          taskContext.setStatusMessage(
              String.format(
                  "currently processing -> %s, %s/%s/%s/%s, %s",
                  layerName, tileMatrixSet.getId(), level, row, col, outputFormat.getExtension()));

          TileResult result = tileProvider.getTile(tile);

          if (result.isError()) {
            LOGGER.warn(
                "{}: processing failed -> {}, {}/{}/{}/{}, {} | {}",
                getLabel(),
                layerName,
                tileMatrixSet.getId(),
                level,
                row,
                col,
                outputFormat.getExtension(),
                result.getError().get());
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
      List<TileFormatExtension> outputFormats,
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
      List<TileFormatExtension> outputFormats,
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
        TileFormatExtension outputFormat,
        TileMatrixSet tileMatrixSet,
        int level,
        int row,
        int col)
        throws IOException;
  }

  private void walkCollectionsAndTiles(
      OgcApi api,
      List<TileFormatExtension> outputFormats,
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
      List<TileFormatExtension> outputFormats,
      Map<String, MinMax> seeding,
      TaskContext taskContext,
      TileWalker tileWalker)
      throws IOException {
    for (TileFormatExtension outputFormat : outputFormats) {
      for (Map.Entry<String, MinMax> entry : seeding.entrySet()) {
        TileMatrixSet tileMatrixSet = getTileMatrixSetById(entry.getKey());
        MinMax zoomLevels = entry.getValue();
        List<TileMatrixSetLimits> allLimits =
            limitsGenerator.getTileMatrixSetLimits(
                api, tileMatrixSet, zoomLevels, Optional.empty());

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
