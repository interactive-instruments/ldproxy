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
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataHydratorExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableTileMatrixSetsConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsConfiguration;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.infra.EndpointTileSetsMultiCollection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Deprecated(since = "3.4")
@Singleton
@AutoBind
public class TilesDataHydrator implements OgcApiDataHydratorExtension {
  private final ExtensionRegistry extensionRegistry;

  @Inject
  public TilesDataHydrator(ExtensionRegistry extensionRegistry) {
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

    // for backwards compatibility: update TileMatrixSets configurations and enable them, if
    // Tiles is enabled

    ImmutableSet.Builder<String> tileMatrixSetsBuilder = ImmutableSet.builder();

    TilesConfiguration apiConfig = apiData.getExtension(TilesConfiguration.class).orElse(null);
    if (Objects.nonNull(apiConfig)) {
      tileMatrixSetsBuilder.addAll(apiConfig.getZoomLevelsDerived().keySet());
    }

    apiData
        .getCollections()
        .forEach(
            (key, collectionData) -> {
              TilesConfiguration config =
                  collectionData.getExtension(TilesConfiguration.class).orElse(null);
              if (Objects.isNull(config)) return;

              tileMatrixSetsBuilder.addAll(config.getZoomLevelsDerived().keySet());
            });

    Set<String> tileMatrixSetIds = tileMatrixSetsBuilder.build();

    TileMatrixSetsConfiguration tmsApiConfig =
        apiData
            .getExtension(TileMatrixSetsConfiguration.class)
            .map(cfg -> process(cfg, tileMatrixSetIds))
            .orElse(null);
    if (Objects.nonNull(tmsApiConfig)
        && extensionRegistry.getExtensionsForType(EndpointTileSetsMultiCollection.class).stream()
            .anyMatch(tiles -> tiles.isEnabledForApi(apiData))) {
      // update data with changes
      return new ImmutableOgcApiDataV2.Builder()
          .from(apiData)
          .extensions(
              new ImmutableList.Builder<ExtensionConfiguration>()
                  // do not touch any other extensions
                  .addAll(
                      apiData.getExtensions().stream()
                          .filter(
                              ext ->
                                  !ext.getBuildingBlock().equals(tmsApiConfig.getBuildingBlock()))
                          .collect(Collectors.toUnmodifiableList()))
                  // add the Tiles and TileMatrixSets configuration
                  .add(tmsApiConfig)
                  .build())
          .build();
    }

    return apiData;
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
