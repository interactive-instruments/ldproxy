/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.math.BigDecimal;
import java.util.List;

@Component
@Provides
@Instantiate
public class PathParameterTileCol implements OgcApiPathParameter {

    @Override
    public String getPattern() {
        return "\\d+";
    }

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        return ImmutableList.of();
    }

    private final Schema schema = new IntegerSchema().minimum(BigDecimal.ZERO);

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public String getId() {
        return "tileColMapTile";
    }

    @Override
    public String getName() {
        return "tileCol";
    }

    @Override
    public String getDescription() {
        return "Column index of the tile on the selected zoom level. See http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/ for more information about Level, Row and Column in the Google Maps tiling scheme (WebMercatorQuad). " +
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
