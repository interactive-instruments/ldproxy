/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.Set;

public abstract class TileFormatExtension implements FormatExtension {

  private final TilesProviders tilesProviders;

  public TileFormatExtension(TilesProviders tilesProviders) {
    this.tilesProviders = tilesProviders;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(cfg -> cfg.hasDatasetTiles(tilesProviders, apiData))
            .isPresent()
        && tilesProviders
            .getTilesetMetadata(apiData)
            .filter(metadata -> metadata.getEncodings().contains(this.getMediaType().label()))
            .isPresent();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
            .getExtension(TilesConfiguration.class, collectionId)
            .filter(TilesConfiguration::isEnabled)
            .filter(cfg -> cfg.hasCollectionTiles(tilesProviders, apiData, collectionId))
            .isPresent()
        && tilesProviders
            .getTilesetMetadata(apiData, apiData.getCollectionData(collectionId))
            .filter(metadata -> metadata.getEncodings().contains(this.getMediaType().label()))
            .isPresent();
  }

  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    Set<String> formats =
        tilesProviders
            .getTilesetMetadata(apiData)
            .map(TilesetMetadata::getEncodings)
            .orElse(ImmutableSet.of());
    return isEnabledForApi(apiData)
        && definitionPath.startsWith("/tiles")
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(getMediaType().label()));
  }

  public boolean isApplicable(OgcApiDataV2 apiData, String collectionId, String definitionPath) {
    Set<String> formats =
        tilesProviders
            .getTilesetMetadata(apiData, apiData.getCollectionData(collectionId))
            .map(TilesetMetadata::getEncodings)
            .orElse(ImmutableSet.of());
    return isEnabledForApi(apiData, collectionId)
        && definitionPath.startsWith("/collections/{collectionId}/tiles")
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(getMediaType().label()));
  }

  public abstract String getExtension();

  public abstract TileSet.DataType getDataType();

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }
}
