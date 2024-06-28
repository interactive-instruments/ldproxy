/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.WithChangeListeners;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiBackgroundTask;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.TilesProvidersCache;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.entities.domain.ValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult.MODE;
import de.ii.xtraplatform.features.domain.DatasetChangeListener;
import de.ii.xtraplatform.features.domain.FeatureChangeListener;
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.tiles.domain.Cache.Storage;
import de.ii.xtraplatform.tiles.domain.ImmutableTileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.SeedingOptions;
import de.ii.xtraplatform.tiles.domain.TileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.TileSeedingJobSet;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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
public class TileSeedingBackgroundTask implements OgcApiBackgroundTask, WithChangeListeners {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileSeedingBackgroundTask.class);

  private final ExtensionRegistry extensionRegistry;
  private final FeaturesCoreProviders providers;
  private final TilesProviders tilesProviders;
  private final TilesProvidersCache tilesProvidersCache;
  private final VolatileRegistry volatileRegistry;
  private final JobQueue jobQueue;
  private Consumer<OgcApi> trigger;

  @Inject
  public TileSeedingBackgroundTask(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      TilesProviders tilesProviders,
      TilesProvidersCache tilesProvidersCache,
      VolatileRegistry volatileRegistry,
      JobQueue jobQueue) {
    this.extensionRegistry = extensionRegistry;
    this.providers = providers;
    this.tilesProviders = tilesProviders;
    this.tilesProvidersCache = tilesProvidersCache;
    this.volatileRegistry = volatileRegistry;
    this.jobQueue = jobQueue;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    // check that we have a tile provider with seeding support
    if (tilesProviders
        .getTileProvider(apiData)
        .map(provider -> provider.seeding().isSupported())
        .isEmpty()) {
      return false;
    }

    return OgcApiBackgroundTask.super.isEnabledForApi(apiData);
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
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    providers
        .getFeatureProvider(api.getData())
        .ifPresent(provider -> updateChangeListeners(provider.changes(), api));

    return ValidationResult.of();
  }

  @Override
  public void onShutdown(OgcApi api) {
    providers
        .getFeatureProvider(api.getData())
        .ifPresent(provider -> removeChangeListeners(provider.changes(), api));

    OgcApiBackgroundTask.super.onShutdown(api);
  }

  // TODO: seeding options without available
  @Override
  public boolean runOnStart(OgcApi api) {
    return isEnabledForApi(api.getData())
        && tilesProviders
            .getTileProvider(api.getData())
            .filter(provider -> provider.seeding().isSupported())
            .map(provider -> provider.seeding().get().getOptions())
            .filter(SeedingOptions::shouldRunOnStartup)
            .isPresent();
  }

  @Override
  public Optional<String> runPeriodic(OgcApi api) {
    if (!isEnabledForApi(api.getData())) {
      return Optional.empty();
    }
    return tilesProviders
        .getTileProvider(api.getData())
        .filter(provider -> provider.seeding().isSupported())
        .map(provider -> provider.seeding().get().getOptions())
        .flatMap(SeedingOptions::getCronExpression);
  }

  @Override
  public int getMaxPartials(OgcApi api) {
    return 1;
  }

  @Override
  public void setTrigger(Consumer<OgcApi> trigger) {
    this.trigger = trigger;
  }

  @Override
  public boolean isSilent() {
    return true;
  }

  private boolean shouldPurge(OgcApi api) {
    return tilesProviders
        .getTileProvider(api.getData())
        .filter(provider -> provider.seeding().isSupported())
        .map(provider -> provider.seeding().get().getOptions())
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
    CompletableFuture<Void> waitForVolatiles =
        volatileRegistry
            .onAvailable(tilesProvidersCache, tilesProviders.getTileProviderOrThrow(api.getData()))
            .toCompletableFuture();

    if (!waitForVolatiles.isDone()) {
      LOGGER.info("Tile cache seeding suspended");
      waitForVolatiles.join();
      LOGGER.info("Tile cache seeding resumed");
    }

    boolean reseed = shouldPurge(api);

    try {
      if (!taskContext.isStopped()) {
        seedTilesets(api, reseed);
      }

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

  private void seedTilesets(OgcApi api, boolean reseed) throws IOException {
    OgcApiDataV2 apiData = api.getData();

    TileProvider tileProvider = tilesProviders.getTileProviderOrThrow(apiData);

    if (!tileProvider.seeding().isAvailable() || !tileProvider.generator().isAvailable()) {
      LOGGER.debug("Tile provider '{}' does not support seeding", tileProvider.getId());
      return;
    }

    boolean inProgress =
        jobQueue.getSets().stream()
            .anyMatch(
                jobSet ->
                    Objects.equals(jobSet.getType(), TileSeedingJobSet.TYPE)
                        && !jobSet.isDone()
                        && jobSet
                            .getEntity()
                            .filter(e -> Objects.equals(e, tileProvider.getId()))
                            .isPresent());

    if (inProgress) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("{} is already in progress, skipping new task", TileSeedingJobSet.LABEL);
      }

      return;
    }

    Map<String, TileGenerationParameters> tilesets = new LinkedHashMap<>();
    Map<String, TileGenerationParameters> combinedTilesets = new LinkedHashMap<>();

    for (String collectionId : apiData.getCollections().keySet()) {
      getTilesConfiguration(apiData, collectionId)
          .filter(cfg -> cfg.hasCollectionTiles(tilesProviders, apiData, collectionId))
          .map(cfg -> cfg.getCollectionTileset(collectionId))
          .filter(tileset -> !tilesets.containsKey(tileset))
          .ifPresent(
              tileset -> {
                TileGenerationParameters generationParameters =
                    new ImmutableTileGenerationParameters.Builder()
                        .clipBoundingBox(api.getSpatialExtent(collectionId))
                        .substitutions(
                            FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                                api.getUri().toString()))
                        .build();

                if (((TileProviderFeaturesData) tileProvider.getData())
                    .getTilesets()
                    .get(tileset)
                    .isCombined()) {
                  combinedTilesets.put(tileset, generationParameters);
                } else {
                  tilesets.put(tileset, generationParameters);
                }
              });
    }

    apiData
        .getExtension(TilesConfiguration.class)
        .filter(cfg -> cfg.hasDatasetVectorTiles(tilesProviders, apiData))
        .map(TilesConfiguration::getDatasetTileset)
        .filter(tileset -> !tilesets.containsKey(tileset))
        .ifPresent(
            tileset -> {
              TileGenerationParameters generationParameters =
                  new ImmutableTileGenerationParameters.Builder()
                      .clipBoundingBox(api.getSpatialExtent())
                      .substitutions(
                          FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                              api.getUri().toString()))
                      .build();

              if (((TileProviderFeaturesData) tileProvider.getData())
                  .getTilesets()
                  .get(tileset)
                  .isCombined()) {
                combinedTilesets.put(tileset, generationParameters);
              } else {
                tilesets.put(tileset, generationParameters);
              }
            });

    boolean perJob =
        ((TileProviderFeaturesData) tileProvider.getData())
            .getCaches().stream()
                .anyMatch(cache -> cache.getSeeded() && cache.getStorage() == Storage.PER_JOB);

    Map<String, List<String>> rasterForVectorTilesets =
        tilesets.entrySet().stream()
            .map(
                entry ->
                    Map.entry(
                        entry.getKey(),
                        tileProvider.access().get().getMapStyles(entry.getKey()).stream()
                            .map(
                                style ->
                                    tileProvider
                                        .access()
                                        .get()
                                        .getMapStyleTileset(entry.getKey(), style))
                            .collect(Collectors.toList())))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    Map<String, List<String>> rasterForVectorCombinedTilesets =
        combinedTilesets.entrySet().stream()
            .map(
                entry ->
                    Map.entry(
                        entry.getKey(),
                        tileProvider.access().get().getMapStyles(entry.getKey()).stream()
                            .map(
                                style ->
                                    tileProvider
                                        .access()
                                        .get()
                                        .getMapStyleTileset(entry.getKey(), style))
                            .collect(Collectors.toList())))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    JobSet jobSet = TileSeedingJobSet.of(tileProvider.getId(), tilesets, reseed);

    Map<String, TileGenerationParameters> rasterTilesets =
        tilesets.entrySet().stream()
            .flatMap(
                ts ->
                    rasterForVectorTilesets.get(ts.getKey()).stream()
                        .map(rts -> Map.entry(rts, ts.getValue())))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    if (!rasterTilesets.isEmpty()) {
      if (!perJob) {
        jobSet = jobSet.with(TileSeedingJobSet.of(tileProvider.getId(), rasterTilesets, reseed));
      } else {
        jobSet = TileSeedingJobSet.with(jobSet, rasterTilesets);
      }
    }

    if (!combinedTilesets.isEmpty()) {
      JobSet combinedJobSet = TileSeedingJobSet.of(tileProvider.getId(), combinedTilesets, reseed);

      Map<String, TileGenerationParameters> rasterCombinedTilesets =
          combinedTilesets.entrySet().stream()
              .flatMap(
                  ts ->
                      rasterForVectorCombinedTilesets.get(ts.getKey()).stream()
                          .map(rts -> Map.entry(rts, ts.getValue())))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      if (!rasterCombinedTilesets.isEmpty()) {
        if (!perJob) {
          combinedJobSet =
              combinedJobSet.with(
                  TileSeedingJobSet.of(tileProvider.getId(), rasterCombinedTilesets, reseed));
        } else {
          combinedJobSet = TileSeedingJobSet.with(combinedJobSet, rasterCombinedTilesets);
        }
      }

      jobSet = jobSet.with(combinedJobSet);
    }

    jobQueue.push(jobSet);
  }

  @Override
  public DatasetChangeListener onDatasetChange(OgcApi api) {
    return change -> {
      for (String featureType : change.getFeatureTypes()) {
        String collectionId = FeaturesCoreConfiguration.getCollectionId(api.getData(), featureType);

        try {
          tilesProvidersCache.deleteTiles(
              api, Optional.of(collectionId), Optional.empty(), Optional.empty());
        } catch (Exception e) {
          if (LOGGER.isErrorEnabled()) {
            LOGGER.error(
                "Error while deleting tiles from the tile cache after a dataset change.", e);
          }
        }
      }

      if (Objects.nonNull(trigger)) {
        trigger.accept(api);
      }
    };
  }

  @Override
  public FeatureChangeListener onFeatureChange(OgcApi api) {
    return change -> {
      String collectionId =
          FeaturesCoreConfiguration.getCollectionId(api.getData(), change.getFeatureType());
      boolean tilesDeleted = false;
      switch (change.getAction()) {
        case UPDATE:
          // if old and new bbox intersect, merge them, otherwise delete tiles separately
          change
              .getOldBoundingBox()
              .flatMap(
                  oldBbox ->
                      change
                          .getNewBoundingBox()
                          .filter(newBbox -> BoundingBox.intersects(oldBbox, newBbox))
                          .map(newBbox -> BoundingBox.merge(oldBbox, newBbox)))
              .ifPresentOrElse(
                  bbox -> deleteTiles(api, collectionId, bbox),
                  () -> {
                    change
                        .getOldBoundingBox()
                        .ifPresent(bbox -> deleteTiles(api, collectionId, bbox));
                    change
                        .getNewBoundingBox()
                        .ifPresent(bbox -> deleteTiles(api, collectionId, bbox));
                  });
          tilesDeleted =
              change.getOldBoundingBox().isPresent() || change.getNewBoundingBox().isPresent();
          break;
        case CREATE:
          change.getNewBoundingBox().ifPresent(bbox -> deleteTiles(api, collectionId, bbox));
          tilesDeleted = change.getNewBoundingBox().isPresent();
          break;
        case DELETE:
          change.getOldBoundingBox().ifPresent(bbox -> deleteTiles(api, collectionId, bbox));
          tilesDeleted = change.getOldBoundingBox().isPresent();
          break;
      }
      if (tilesDeleted && Objects.nonNull(trigger)) {
        trigger.accept(api);
      }
    };
  }

  private void deleteTiles(OgcApi api, String collectionId, BoundingBox bbox) {
    try {
      tilesProvidersCache.deleteTiles(
          api, Optional.of(collectionId), Optional.empty(), Optional.of(bbox));
    } catch (Exception e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Error while deleting tiles from the tile cache after a feature change.", e);
      }
    }
  }

  private Optional<TilesConfiguration> getTilesConfiguration(
      OgcApiDataV2 apiData, String collectionId) {
    return Optional.ofNullable(apiData.getCollections().get(collectionId))
        .flatMap(featureType -> featureType.getExtension(TilesConfiguration.class))
        .filter(TilesConfiguration::isEnabled);
  }
}
