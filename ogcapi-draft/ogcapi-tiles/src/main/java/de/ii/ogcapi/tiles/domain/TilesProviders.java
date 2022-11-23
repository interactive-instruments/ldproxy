/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.provider.TileProvider;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.Optional;

public interface TilesProviders {

  boolean hasTileProvider(OgcApiDataV2 apiData);

  Optional<TileProvider> getTileProvider(OgcApiDataV2 apiData);

  TileProvider getTileProviderOrThrow(OgcApiDataV2 apiData);

  boolean hasTileProvider(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  Optional<TileProvider> getTileProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData);

  default boolean hasTileProvider(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? hasTileProvider(apiData, collectionData.get())
        : hasTileProvider(apiData);
  }

  default Optional<TileProvider> getTileProvider(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTileProvider(apiData, collectionData.get())
        : getTileProvider(apiData);
  }

  default TileProvider getTileProviderOrThrow(
      OgcApiDataV2 apiData, Optional<FeatureTypeConfigurationOgcApi> collectionData) {
    return collectionData.isPresent()
        ? getTileProviderOrThrow(apiData, collectionData.get())
        : getTileProviderOrThrow(apiData);
  }

  void deleteTiles(
      OgcApi api,
      Optional<String> collectionId,
      Optional<String> tileMatrixSetId,
      Optional<BoundingBox> boundingBox);
}
