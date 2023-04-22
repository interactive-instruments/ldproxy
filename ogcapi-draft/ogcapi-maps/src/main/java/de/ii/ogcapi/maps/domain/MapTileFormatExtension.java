/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.Set;

public abstract class MapTileFormatExtension implements FormatExtension {

  private final TilesProviders tilesProviders;

  public MapTileFormatExtension(TilesProviders tilesProviders) {
    this.tilesProviders = tilesProviders;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(MapTilesConfiguration.class)
            .filter(MapTilesConfiguration::isEnabled)
            .filter(MapTilesConfiguration::isMultiCollectionEnabled)
            .filter(
                config ->
                    tilesProviders
                        .getTilesetMetadataOrThrow(apiData)
                        .getTileEncodings()
                        .contains(this.getMediaType().label()))
            .isPresent()
        && apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(TilesConfiguration::hasDatasetTiles)
            .isPresent();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
            .getExtension(MapTilesConfiguration.class, collectionId)
            .filter(MapTilesConfiguration::isEnabled)
            .filter(MapTilesConfiguration::isSingleCollectionEnabled)
            .filter(
                config ->
                    tilesProviders
                        .getTilesetMetadataOrThrow(apiData, apiData.getCollectionData(collectionId))
                        .getTileEncodings()
                        .contains(this.getMediaType().label()))
            .isPresent()
        && apiData
            .getExtension(TilesConfiguration.class, collectionId)
            .filter(TilesConfiguration::isEnabled)
            .filter(TilesConfiguration::hasCollectionTiles)
            .isPresent();
  }

  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    Set<String> formats =
        tilesProviders
            .getTilesetMetadata(apiData)
            .map(TilesetMetadata::getTileEncodings)
            .orElse(ImmutableSet.of());
    return isEnabledForApi(apiData)
        && definitionPath.startsWith("/map/tiles")
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(getMediaType().label()));
  }

  public boolean isApplicable(OgcApiDataV2 apiData, String collectionId, String definitionPath) {
    Set<String> formats =
        tilesProviders
            .getTilesetMetadata(apiData, apiData.getCollectionData(collectionId))
            .map(TilesetMetadata::getTileEncodings)
            .orElse(ImmutableSet.of());
    return isEnabledForApi(apiData, collectionId)
        && definitionPath.startsWith("/collections/{collectionId}/map/tiles")
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(getMediaType().label()));
  }

  public TileSet.DataType getDataType() {
    return TileSet.DataType.map;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return MapTilesConfiguration.class;
  }

  public abstract String getExtension();
}
