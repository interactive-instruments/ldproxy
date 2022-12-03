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
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiBackgroundTask;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.SeedingOptions;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileFormatWithQuerySupportExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileGenerationParameters.Builder;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationParameters;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
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
  private final FeaturesCoreProviders providers;
  private final TilesProviders tilesProviders;

  @Inject
  public TileSeedingBackgroundTask(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      TilesProviders tilesProviders) {
    this.extensionRegistry = extensionRegistry;
    this.providers = providers;
    this.tilesProviders = tilesProviders;
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
            .flatMap(TilesConfiguration::getSeedingOptionsDerived)
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
        .flatMap(TilesConfiguration::getSeedingOptionsDerived)
        .flatMap(SeedingOptions::getCronExpression);
  }

  @Override
  public int getMaxPartials(OgcApi api) {
    return api.getData()
        .getExtension(TilesConfiguration.class)
        .flatMap(TilesConfiguration::getSeedingOptionsDerived)
        .map(SeedingOptions::getEffectiveMaxThreads)
        .orElse(1);
  }

  private boolean shouldPurge(OgcApi api) {
    return api.getData()
        .getExtension(TilesConfiguration.class)
        .flatMap(TilesConfiguration::getSeedingOptionsDerived)
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
    boolean reseed = shouldPurge(api);
    List<TileFormatExtension> outputFormats =
        extensionRegistry.getExtensionsForType(TileFormatExtension.class);

    try {
      if (!taskContext.isStopped()) {
        seedLayers(api, outputFormats, reseed, taskContext);
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

  private void seedLayers(
      OgcApi api, List<TileFormatExtension> outputFormats, boolean reseed, TaskContext taskContext)
      throws IOException {
    OgcApiDataV2 apiData = api.getData();

    // TODO: isEnabled should check that we have a tile provider
    // TODO: different tile provider per collection
    TileProvider tileProvider = tilesProviders.getTileProviderOrThrow(apiData);

    if (!tileProvider.supportsSeeding()) {
      LOGGER.debug("Tile provider '{}' does not support seeding", tileProvider.getId());
      return;
    }

    List<MediaType> formats =
        outputFormats.stream()
            .filter(format -> tileProvider.generator().supports(format.getMediaType().type()))
            .map(format -> format.getMediaType().type())
            .collect(Collectors.toList());

    Map<String, TileGenerationParameters> layers = new LinkedHashMap<>();

    for (String collectionId : apiData.getCollections().keySet()) {
      Optional<TilesConfiguration> tilesConfiguration =
          getTilesConfiguration(api.getData(), collectionId);

      if (tilesConfiguration.isPresent()) {
        TileGenerationParameters generationParameters =
            new Builder()
                .clipBoundingBox(api.getSpatialExtent(collectionId))
                .propertyTransformations(
                    api.getData()
                        .getCollectionData(collectionId)
                        .flatMap(cd -> cd.getExtension(FeaturesCoreConfiguration.class))
                        .map(
                            pt ->
                                pt.withSubstitutions(
                                    FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                                        api.getUri().toString()))))
                .build();

        layers.put(collectionId, generationParameters);
      }
    }

    Optional<TilesConfiguration> tilesConfiguration =
        apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isMultiCollectionEnabled);

    if (tilesConfiguration.isPresent()) {
      TileGenerationParameters generationParameters =
          new Builder()
              .clipBoundingBox(api.getSpatialExtent())
              .propertyTransformations(
                  api.getData()
                      .getExtension(FeaturesCoreConfiguration.class)
                      .map(
                          pt ->
                              pt.withSubstitutions(
                                  FeaturesCoreProviders.DEFAULT_SUBSTITUTIONS.apply(
                                      api.getUri().toString()))))
              .build();

      layers.put(DATASET_TILES, generationParameters);
    }

    tileProvider.seeding().seed(layers, formats, reseed, taskContext);
  }

  private Optional<TilesConfiguration> getTilesConfiguration(
      OgcApiDataV2 apiData, String collectionId) {
    return Optional.ofNullable(apiData.getCollections().get(collectionId))
        .flatMap(featureType -> featureType.getExtension(TilesConfiguration.class))
        .filter(TilesConfiguration::isEnabled);
  }
}
