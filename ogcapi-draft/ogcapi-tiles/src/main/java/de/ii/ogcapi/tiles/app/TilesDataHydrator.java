/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataHydratorExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.ImmutableMinMax.Builder;
import de.ii.ogcapi.tiles.domain.ImmutableTileProviderMbtiles;
import de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration;
import de.ii.ogcapi.tiles.domain.MinMax;
import de.ii.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ogcapi.tiles.domain.TileProviderMbtiles;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class TilesDataHydrator implements OgcApiDataHydratorExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(TilesDataHydrator.class);

  private final StaticTileProviderStore staticTileProviderStore;

  @Inject
  public TilesDataHydrator(StaticTileProviderStore staticTileProviderStore) {
    this.staticTileProviderStore = staticTileProviderStore;
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

    // get Tiles configurations to derive metadata from Mbtiles tile providers

    TilesConfiguration apiConfig = apiData.getExtension(TilesConfiguration.class).orElse(null);
    if (Objects.nonNull(apiConfig)) {
      apiConfig = process(apiData, apiConfig);
    }

    Map<String, TilesConfiguration> collectionConfigs =
        apiData.getCollections().entrySet().stream()
            .map(
                entry -> {
                  final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                  TilesConfiguration config =
                      collectionData.getExtension(TilesConfiguration.class).orElse(null);
                  if (Objects.isNull(config)) return null;

                  final String collectionId = entry.getKey();
                  config = process(apiData, config);
                  return new AbstractMap.SimpleImmutableEntry<>(collectionId, config);
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // update data with changes
    ImmutableOgcApiDataV2.Builder builder = new ImmutableOgcApiDataV2.Builder().from(apiData);

    if (Objects.nonNull(apiConfig)) {
      final String buildingBlock = apiConfig.getBuildingBlock();
      builder.extensions(
          new ImmutableList.Builder<ExtensionConfiguration>()
              // do not touch any other extension
              .addAll(
                  apiData.getExtensions().stream()
                      .filter(ext -> !ext.getBuildingBlock().equals(buildingBlock))
                      .collect(Collectors.toUnmodifiableList()))
              // add the Tiles configuration
              .add(apiConfig)
              .build());
    }

    builder
        .collections(
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
                                                  ext ->
                                                      !ext.getBuildingBlock().equals(buildingBlock))
                                              .collect(Collectors.toUnmodifiableList()))
                                      // add the Tiles configuration
                                      .add(config)
                                      .build())
                              .build());
                    })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
        .build();

    return builder.build();
  }

  private TilesConfiguration process(OgcApiDataV2 apiData, TilesConfiguration config) {
    if (Objects.nonNull(config.getTileProvider())
        && config.getTileProvider() instanceof TileProviderMbtiles) {
      try {
        TileProviderMbtiles tileProvider = (TileProviderMbtiles) config.getTileProvider();
        String filename = tileProvider.getFilename();
        Optional<Integer> minzoom = staticTileProviderStore.getMinzoom(apiData, filename);
        Optional<Integer> maxzoom = staticTileProviderStore.getMaxzoom(apiData, filename);
        Optional<Integer> defzoom = staticTileProviderStore.getDefaultzoom(apiData, filename);
        Map<String, MinMax> zoomLevels =
            ImmutableMap.of(
                "WebMercatorQuad",
                new Builder()
                    .min(minzoom.orElse(0))
                    .max(maxzoom.orElse(24))
                    .getDefault(defzoom)
                    .build());
        String format = staticTileProviderStore.getFormat(apiData, filename);
        List<Double> center = staticTileProviderStore.getCenter(apiData, filename);
        config =
            new ImmutableTilesConfiguration.Builder()
                .from(config)
                .tileProvider(
                    ImmutableTileProviderMbtiles.builder()
                        .from(tileProvider)
                        .zoomLevels(zoomLevels)
                        .tileEncoding(format)
                        .center(center)
                        .build())
                .build();
      } catch (Exception e) {
        throw new RuntimeException("Could not derive metadata from Mbtiles tile provider.", e);
      }
    }
    return config;
  }
}
