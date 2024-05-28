/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.tiles.domain.TilesFormat;

public abstract class TileFormatExtension implements FormatExtension {

  protected final TilesProviders tilesProviders;

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
        && (tilesProviders
                .getTilesetMetadata(apiData)
                .filter(
                    metadata ->
                        metadata
                            .getEncodings()
                            .contains(TilesFormat.of(this.getMediaType().label())))
                .isPresent()
            || tilesProviders.getRasterTilesetMetadata(apiData).values().stream()
                .anyMatch(
                    metadata ->
                        metadata
                            .getEncodings()
                            .contains(TilesFormat.of(this.getMediaType().label()))));
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
            .getExtension(TilesConfiguration.class, collectionId)
            .filter(TilesConfiguration::isEnabled)
            .filter(cfg -> cfg.hasCollectionTiles(tilesProviders, apiData, collectionId))
            .isPresent()
        && (tilesProviders
                .getTilesetMetadata(apiData, apiData.getCollectionData(collectionId))
                .filter(
                    metadata ->
                        metadata
                            .getEncodings()
                            .contains(TilesFormat.of(this.getMediaType().label())))
                .isPresent()
            || tilesProviders
                .getRasterTilesetMetadata(apiData, apiData.getCollectionData(collectionId))
                .values()
                .stream()
                .anyMatch(
                    metadata ->
                        metadata
                            .getEncodings()
                            .contains(TilesFormat.of(this.getMediaType().label()))));
  }

  public abstract String getExtension();

  public abstract boolean isApplicable(OgcApiDataV2 apiData, String definitionPath);

  public abstract boolean isApplicable(
      OgcApiDataV2 apiData, String collectionId, String definitionPath);

  public abstract TileSet.DataType getDataType();

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }
}
