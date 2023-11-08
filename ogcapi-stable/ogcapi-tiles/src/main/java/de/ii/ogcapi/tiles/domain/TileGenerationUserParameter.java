/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.xtraplatform.tiles.domain.ImmutableTileGenerationParametersTransient;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import java.util.Optional;

@AutoMultiBind
public interface TileGenerationUserParameter {

  void applyTo(
      ImmutableTileGenerationParametersTransient.Builder userParametersBuilder,
      QueryParameterSet parameters,
      Optional<TileGenerationSchema> generationSchema);

  default boolean isEnabledForApi(OgcApiDataV2 apiData, TilesProviders tilesProviders) {
    Optional<TilesConfiguration> config =
        apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(cfg -> cfg.hasDatasetTiles(tilesProviders, apiData));
    Optional<de.ii.xtraplatform.tiles.domain.TileProvider> tileProvider =
        tilesProviders
            .getTileProvider(apiData)
            .filter(de.ii.xtraplatform.tiles.domain.TileProvider::supportsGeneration);

    return config.isPresent() && tileProvider.isPresent();
  }

  default boolean isEnabledForApi(
      OgcApiDataV2 apiData, String collectionId, TilesProviders tilesProviders) {
    Optional<TilesConfiguration> config =
        apiData
            .getCollectionData(collectionId)
            .flatMap(cd -> cd.getExtension(TilesConfiguration.class))
            .filter(TilesConfiguration::isEnabled)
            .filter(cfg -> cfg.hasCollectionTiles(tilesProviders, apiData, collectionId));
    Optional<de.ii.xtraplatform.tiles.domain.TileProvider> tileProvider =
        tilesProviders
            .getTileProvider(apiData, apiData.getCollectionData(collectionId))
            .filter(TileProvider::supportsGeneration);

    return config.isPresent() && tileProvider.isPresent();
  }
}
