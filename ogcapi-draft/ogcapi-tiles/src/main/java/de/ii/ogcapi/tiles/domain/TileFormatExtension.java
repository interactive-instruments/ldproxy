/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;

public abstract class TileFormatExtension implements FormatExtension {

  protected String SCHEMA_REF_TILE = "#/components/schemas/Binary";
  protected Schema SCHEMA_TILE = new BinarySchema();

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(TilesConfiguration.class)
        .filter(TilesConfiguration::isEnabled)
        .filter(TilesConfiguration::isMultiCollectionEnabled)
        .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
        .isPresent();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
        .getExtension(TilesConfiguration.class, collectionId)
        .filter(TilesConfiguration::isEnabled)
        .filter(TilesConfiguration::isSingleCollectionEnabled)
        .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
        .isPresent();
  }

  @Override
  public String getPathPattern() {
    return "^(?:/collections/" + COLLECTION_ID_PATTERN + ")?/tiles/\\w+/\\w+/\\w+/\\w+/?$";
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

  public boolean canMultiLayer() {
    return false;
  }

  public boolean supportsFeatureQuery() {
    return this instanceof TileFromFeatureQuery;
  }

  public abstract String getExtension();

  public boolean getGzippedInMbtiles() {
    return false;
  }

  public boolean getSupportsEmptyTile() {
    return false;
  }

  public byte[] getEmptyTile(Tile tile) {
    throw new IllegalStateException(
        String.format(
            "No empty tile available for tile format %s.", this.getClass().getSimpleName()));
  }

  public abstract TileSet.DataType getDataType();

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }
}
