/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.util.List;

@Singleton
@AutoBind
public class PathParameterTileMatrix implements OgcApiPathParameter {

    @Override
    public String getPattern() {
        return "\\d+";
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        return ImmutableList.of();
    }

    private final Schema schema = new StringSchema().pattern(getPattern());

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public String getId() {
        return "tileMatrixMapTile";
    }

    @Override
    public String getName() {
        return "tileMatrix";
    }

    @Override
    public String getDescription() {
        return "Zoom level of the tile. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps tiling scheme (WebMercatorQuad). " +
                "Example: In the WebMercatorQuad tiling scheme Ireland is fully within the tile with the following values: Level 5, Row 10 and Col 15";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               (definitionPath.equals("/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                definitionPath.equals("/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return MapTilesConfiguration.class;
    }
}
