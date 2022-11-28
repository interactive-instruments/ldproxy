/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.API_RESOURCES_DIR;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataHydratorExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableMinMax;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableTileMatrixSetsConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsConfiguration;
import de.ii.ogcapi.tiles.app.provider.MbtilesMetadata;
import de.ii.ogcapi.tiles.app.provider.MbtilesMetadata.MbtilesFormat;
import de.ii.ogcapi.tiles.app.provider.MbtilesTileset;
import de.ii.ogcapi.tiles.domain.ImmutableTileProviderMbtiles;
import de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration;
import de.ii.ogcapi.tiles.domain.TileProviderMbtiles;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.infra.EndpointTileSetsMultiCollection;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class TilesDataHydrator implements OgcApiDataHydratorExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(TilesDataHydrator.class);
  private static final String TILES_DIR_NAME = "tiles";

  private final AppContext appContext;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final ExtensionRegistry extensionRegistry;

  @Inject
  public TilesDataHydrator(
      AppContext appContext,
      TileMatrixSetRepository tileMatrixSetRepository,
      ExtensionRegistry extensionRegistry) {
    this.appContext = appContext;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public int getSortPriority() {
    return 200;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  public OgcApiDataV2 getHydratedData(OgcApiDataV2 apiData) {

    // TODO change to new TileProvider logic

    // 1. get Tiles configurations to derive metadata from Mbtiles tile providers
    // 2. for backwards compatibility: update TileMatrixSets configurations and enable them, if
    // Tiles is enabled

    ImmutableSet.Builder<String> tileMatrixSetsBuilder = ImmutableSet.builder();

    TilesConfiguration apiConfig = apiData.getExtension(TilesConfiguration.class).orElse(null);
    if (Objects.nonNull(apiConfig)) {
      apiConfig = process(apiData, apiConfig);
      tileMatrixSetsBuilder.addAll(apiConfig.getZoomLevelsDerived().keySet());
    }

    Map<String, TilesConfiguration> collectionConfigs =
        apiData.getCollections().entrySet().stream()
            .map(
                entry -> {
                  final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                  TilesConfiguration config =
                      collectionData.getExtension(TilesConfiguration.class).orElse(null);
                  TileMatrixSetsConfiguration tmsConfig =
                      collectionData.getExtension(TileMatrixSetsConfiguration.class).orElse(null);
                  if (Objects.isNull(config)) return null;

                  final String collectionId = entry.getKey();
                  config = process(apiData, config);
                  tileMatrixSetsBuilder.addAll(config.getZoomLevelsDerived().keySet());
                  return new AbstractMap.SimpleImmutableEntry<>(collectionId, config);
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Set<String> tileMatrixSetIds = tileMatrixSetsBuilder.build();
    TileMatrixSetsConfiguration tmsApiConfig =
        apiData.getExtension(TileMatrixSetsConfiguration.class).orElse(null);
    if (extensionRegistry.getExtensionsForType(EndpointTileSetsMultiCollection.class).stream()
        .anyMatch(tiles -> tiles.isEnabledForApi(apiData))) {
      if (Objects.nonNull(tmsApiConfig)) {
        tmsApiConfig = process(tmsApiConfig, tileMatrixSetIds);
      }
    }

    // update data with changes
    ImmutableOgcApiDataV2.Builder builder = new ImmutableOgcApiDataV2.Builder().from(apiData);

    if (Objects.nonNull(apiConfig) && Objects.nonNull(tmsApiConfig)) {
      final String buildingBlock = apiConfig.getBuildingBlock();
      final String tmsBuildingBlock = tmsApiConfig.getBuildingBlock();
      builder.extensions(
          new ImmutableList.Builder<ExtensionConfiguration>()
              // do not touch any other extensions
              .addAll(
                  apiData.getExtensions().stream()
                      .filter(
                          ext ->
                              !ext.getBuildingBlock().equals(buildingBlock)
                                  && !ext.getBuildingBlock().equals(tmsBuildingBlock))
                      .collect(Collectors.toUnmodifiableList()))
              // add the Tiles and TileMatrixSets configuration
              .add(apiConfig)
              .add(tmsApiConfig)
              .build());
    }

    builder.collections(
        apiData.getCollections().entrySet().stream()
            .map(
                entry -> {
                  final String collectionId = entry.getKey();
                  if (!collectionConfigs.containsKey(collectionId)) return entry;

                  final TilesConfiguration config = collectionConfigs.get(collectionId);
                  final String buildingBlock = config.getBuildingBlock();

                  return new AbstractMap.SimpleImmutableEntry<
                      String, FeatureTypeConfigurationOgcApi>(
                      collectionId,
                      new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                          .from(entry.getValue())
                          .extensions(
                              new ImmutableList.Builder<ExtensionConfiguration>()
                                  // do not touch any other extension
                                  .addAll(
                                      entry.getValue().getExtensions().stream()
                                          .filter(
                                              ext -> !ext.getBuildingBlock().equals(buildingBlock))
                                          .collect(Collectors.toUnmodifiableList()))
                                  // add the Tiles configuration
                                  .add(config)
                                  .build())
                          .build());
                })
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));

    return builder.build();
  }

  private TilesConfiguration process(OgcApiDataV2 apiData, TilesConfiguration config) {
    if (config.getTileProvider() instanceof TileProviderMbtiles) {
      TileProviderMbtiles tileProvider = (TileProviderMbtiles) config.getTileProvider();
      if (Objects.nonNull(tileProvider.getFilename())) {
        try {
          Path mbtilesFile =
              appContext
                  .getDataDir()
                  .resolve(API_RESOURCES_DIR)
                  .resolve(TILES_DIR_NAME)
                  .resolve(apiData.getId())
                  .resolve(tileProvider.getFilename());
          MbtilesMetadata metadata = new MbtilesTileset(mbtilesFile).getMetadata();

          String tileMatrixSetId = tileProvider.getTileMatrixSetId();
          TileMatrixSet tileMatrixSet =
              tileMatrixSetRepository
                  .get(tileMatrixSetId)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              String.format("Unknown tile matrix set: '%s'.", tileMatrixSetId)));
          int minzoom = metadata.getMinzoom().orElse(tileMatrixSet.getMinLevel());
          int maxzoom = metadata.getMaxzoom().orElse(tileMatrixSet.getMaxLevel());
          Optional<Integer> defzoom =
              metadata.getCenter().size() == 3
                  ? Optional.of(Math.round(metadata.getCenter().get(2).floatValue()))
                  : Optional.empty();
          List<Double> center =
              metadata.getCenter().size() >= 2
                  ? ImmutableList.of(
                      metadata.getCenter().get(0).doubleValue(),
                      metadata.getCenter().get(1).doubleValue())
                  : ImmutableList.of();
          Map<String, MinMax> zoomLevels =
              ImmutableMap.of(
                  tileMatrixSetId,
                  new ImmutableMinMax.Builder()
                      .min(minzoom)
                      .max(maxzoom)
                      .getDefault(defzoom)
                      .build());
          List<Double> bbox = metadata.getBounds();
          Optional<BoundingBox> bounds =
              bbox.size() == 4
                  ? Optional.of(
                      BoundingBox.of(
                          bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3), OgcCrs.CRS84))
                  : Optional.empty();
          String format = getFormat(metadata.getFormat());
          config =
              new ImmutableTilesConfiguration.Builder()
                  .from(config)
                  .tileProvider(
                      ImmutableTileProviderMbtiles.builder()
                          .from(tileProvider)
                          .zoomLevels(zoomLevels)
                          .tileEncoding(format)
                          .center(center)
                          .bounds(bounds)
                          .build())
                  .build();
        } catch (Exception e) {
          throw new RuntimeException("Could not derive metadata from Mbtiles tile provider.", e);
        }
      }
    }
    return config;
  }

  private String getFormat(MbtilesFormat format) {
    if (format == MbtilesMetadata.MbtilesFormat.pbf) return "MVT";
    else if (format == MbtilesMetadata.MbtilesFormat.jpg) return "JPEG";
    else if (format == MbtilesMetadata.MbtilesFormat.png) return "PNG";
    else if (format == MbtilesMetadata.MbtilesFormat.webp) return "WEBP";
    else if (format == MbtilesMetadata.MbtilesFormat.tiff) return "TIFF";

    throw new UnsupportedOperationException(
        String.format("Mbtiles format '%s' is currently not supported.", format));
  }

  private TileMatrixSetsConfiguration process(
      TileMatrixSetsConfiguration config, Set<String> tileMatrixSetIds) {
    if (!config.isEnabled()) {
      return new ImmutableTileMatrixSetsConfiguration.Builder()
          .from(config)
          .enabled(true)
          .includePredefined(tileMatrixSetIds)
          .build();
    }
    return config;
  }
}
