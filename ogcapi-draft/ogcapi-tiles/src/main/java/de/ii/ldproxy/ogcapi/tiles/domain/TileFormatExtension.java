/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.Schema;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

public interface TileFormatExtension extends FormatExtension {

    String SCHEMA_REF_TILE = "#/components/schemas/Binary";
    Schema SCHEMA_TILE = new BinarySchema();

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(TilesConfiguration.class)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::getMultiCollectionEnabledDerived)
                      .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(TilesConfiguration.class)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::getSingleCollectionEnabledDerived)
                      .filter(config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default String getPathPattern() {
        return "^(?:/collections/"+COLLECTION_ID_PATTERN+")?/tiles/\\w+/\\w+/\\w+/\\w+/?$";
    }

    default boolean canMultiLayer() { return false; }

    default boolean canTransformFeatures() { return false; }

    String getExtension();

    default boolean getGzippedInMbtiles() { return false; }

    default boolean getSupportsEmptyTile() { return false; }

    TileSet.DataType getDataType();

    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

}
