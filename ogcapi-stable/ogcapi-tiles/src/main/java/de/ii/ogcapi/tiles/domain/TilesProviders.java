/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.tiles.app.TilesBuildingBlock.DATASET_TILES;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.Optional;

public interface TilesProviders {

  static String toTilesId(String apiId) {
    return String.format("%s-tiles", apiId);
  }

  boolean hasTileProvider(OgcApiDataV2 apiData);

  Optional<de.ii.xtraplatform.tiles.domain.TileProvider> getTileProvider(OgcApiDataV2 apiData);

  de.ii.xtraplatform.tiles.domain.TileProvider getTileProviderOrThrow(OgcApiDataV2 apiData);

  default Optional<TilesetMetadata> getTilesetMetadata(OgcApiDataV2 apiData) {
    Optional<TileProvider> optionalProvider = getTileProvider(apiData);
    return apiData
        .getExtension(TilesConfiguration.class)
        .map(cfg -> Optional.ofNullable(cfg.getTileProviderTileset()).orElse(DATASET_TILES))
        .flatMap(tilesetId -> optionalProvider.flatMap(provider -> provider.metadata(tilesetId)));
  }

  default TilesetMetadata getTilesetMetadataOrThrow(OgcApiDataV2 apiData) {
    return getTilesetMetadata(apiData)
        .orElseThrow(() -> new IllegalStateException("No tileset metadata found."));
  }

  boolean hasTileProvider(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  Optional<de.ii.xtraplatform.tiles.domain.TileProvider> getTileProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  de.ii.xtraplatform.tiles.domain.TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  default Optional<TilesetMetadata> getTilesetMetadata(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    Optional<TileProvider> optionalProvider = getTileProvider(apiData, collectionData);
    return collectionData
        .getExtension(TilesConfiguration.class)
        .map(
            cfg -> Optional.ofNullable(cfg.getTileProviderTileset()).orElse(collectionData.getId()))
        .flatMap(tilesetId -> optionalProvider.flatMap(provider -> provider.metadata(tilesetId)));
  }

  default TilesetMetadata getTilesetMetadataOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData) {
    return getTilesetMetadata(apiData, collectionData)
        .orElseThrow(() -> new IllegalStateException("No tileset metadata found."));
  }

  default boolean hasTileProvider(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? hasTileProvider(apiData, collectionData.get())
        : hasTileProvider(apiData);
  }

  default Optional<de.ii.xtraplatform.tiles.domain.TileProvider> getTileProvider(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTileProvider(apiData, collectionData.get())
        : getTileProvider(apiData);
  }

  default de.ii.xtraplatform.tiles.domain.TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTileProviderOrThrow(apiData, collectionData.get())
        : getTileProviderOrThrow(apiData);
  }

  default Optional<TilesetMetadata> getTilesetMetadata(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTilesetMetadata(apiData, collectionData.get())
        : getTilesetMetadata(apiData);
  }

  default TilesetMetadata getTilesetMetadataOrThrow(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTilesetMetadataOrThrow(apiData, collectionData.get())
        : getTilesetMetadataOrThrow(apiData);
  }
}
