/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;

import java.util.List;
import java.util.Map;

public class TileEndpointsHelper {

    public static TilesQueriesHandler.QueryInputTileSet getTileSetQueryInput(TilesConfiguration tilesConfiguration, QueryInput genericQueryInput,
                                                                             String tileMatrixSetId) {
        return new ImmutableQueryInputTileSet.Builder()
                .from(genericQueryInput)
                .tileMatrixSetId(tileMatrixSetId)
                .center(tilesConfiguration.getCenterDerived())
                .zoomLevels(tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId))
                .build();
    }

    public static TilesQueriesHandler.QueryInputTileSet getTileSetQueryInput(TilesConfiguration tilesConfiguration, QueryInput genericQueryInput,
                                                                             String tileMatrixSetId, String collectionId) {
        return new ImmutableQueryInputTileSet.Builder()
                .from(genericQueryInput)
                .collectionId(collectionId)
                .tileMatrixSetId(tileMatrixSetId)
                .center(tilesConfiguration.getCenterDerived())
                .zoomLevels(tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId))
                .build();
    }

    public static TilesQueriesHandler.QueryInputTileSets getTileSetsQueryInput(OgcApiDataV2 apiData, QueryInput genericQueryInput,
                                                                               String collectionId) {
        return new ImmutableQueryInputTileSets.Builder()
                .from(genericQueryInput)
                .collectionId(collectionId)
                .center(getCenter(apiData))
                .tileMatrixSetZoomLevels(getTileMatrixSetZoomLevels(apiData, collectionId))
                .build();
    }

    public static TilesQueriesHandler.QueryInputTileSets getTileSetsQueryInput(TilesConfiguration tilesConfiguration, QueryInput genericQueryInput) {
        return new ImmutableQueryInputTileSets.Builder()
                .from(genericQueryInput)
                .center(tilesConfiguration.getCenterDerived())
                .tileMatrixSetZoomLevels(tilesConfiguration.getZoomLevelsDerived())
                .build();
    }

    private static List<Double> getCenter(OgcApiDataV2 data) {
        TilesConfiguration tilesConfiguration = data.getExtension(TilesConfiguration.class).get();
        return tilesConfiguration.getCenterDerived();
    }

    private static Map<String, MinMax> getTileMatrixSetZoomLevels(OgcApiDataV2 data, String collectionId) {
        TilesConfiguration tilesConfiguration = data.getCollections().get(collectionId).getExtension(TilesConfiguration.class).get();
        return tilesConfiguration.getZoomLevelsDerived();
    }
}
