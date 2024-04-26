/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import de.ii.xtraplatform.tiles.domain.TilesFormat;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.Set;

public abstract class MapTileFormatExtension extends TileFormatExtension {

  public MapTileFormatExtension(TilesProviders tilesProviders) {
    super(tilesProviders);
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    Set<TilesFormat> formats =
        tilesProviders
            .getTilesetMetadata(apiData)
            .map(TilesetMetadata::getEncodings)
            .orElse(ImmutableSet.of());
    return isEnabledForApi(apiData)
        && (definitionPath.startsWith("/styles/{styleId}/map/tiles")
            || definitionPath.startsWith("/map/tiles"))
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(TilesFormat.of(getMediaType().label())));
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String collectionId, String definitionPath) {
    Set<TilesFormat> formats =
        tilesProviders
            .getTilesetMetadata(apiData, apiData.getCollectionData(collectionId))
            .map(TilesetMetadata::getEncodings)
            .orElse(ImmutableSet.of());
    return isEnabledForApi(apiData, collectionId)
        && (definitionPath.startsWith("/collections/{collectionId}/styles/{styleId}/map/tiles")
            || definitionPath.startsWith("/collections/{collectionId}/map/tiles"))
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(TilesFormat.of(getMediaType().label())));
  }

  public DataType getDataType() {
    return DataType.map;
  }
}
