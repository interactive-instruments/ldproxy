/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import java.util.List;

public abstract class TileFormatExtension implements FormatExtension {

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(TilesConfiguration.class)
        .filter(TilesConfiguration::isEnabled)
        .filter(TilesConfiguration::hasDatasetTiles)
        .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
        .isPresent();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
        .getExtension(TilesConfiguration.class, collectionId)
        .filter(TilesConfiguration::isEnabled)
        .filter(TilesConfiguration::hasCollectionTiles)
        .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
        .isPresent();
  }

  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    List<String> formats =
        apiData
            .getExtension(TilesConfiguration.class)
            .map(TilesConfiguration::getTileEncodingsDerived)
            .orElse(ImmutableList.of());
    return isEnabledForApi(apiData)
        && definitionPath.startsWith("/tiles")
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(getMediaType().label()));
  }

  public boolean isApplicable(OgcApiDataV2 apiData, String collectionId, String definitionPath) {
    List<String> formats =
        apiData
            .getExtension(TilesConfiguration.class, collectionId)
            .map(TilesConfiguration::getTileEncodingsDerived)
            .orElse(ImmutableList.of());
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
