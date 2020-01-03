/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableSet;

import javax.ws.rs.InternalServerErrorException;
import java.util.Set;

/**
 * Cache for supported Tile Matrix Sets
 */
public final class TileMatrixSetCache {

    private static final Set<String> TILE_MATRIX_SET_IDS = ImmutableSet.of("WebMercatorQuad", "WorldCRS84Quad", "WorldMercatorWGS84Quad");
    private static final TileMatrixSet WEB_MERCATOR_QUAD = new WebMercatorQuad();
    private static final TileMatrixSet WORLD_CRS84_QUAD = new WorldCRS84Quad();
    private static final TileMatrixSet WORLD_MERCATOR_WGS84_QUAD = new WorldMercatorWGS84Quad();

    private TileMatrixSetCache() {

    }

    public static Set<String> getTileMatrixSetIds() {
        return TILE_MATRIX_SET_IDS;
    }

    public static TileMatrixSet getTileMatrixSet(String tileMatrixSetId) {
        switch (tileMatrixSetId) {
            case "WebMercatorQuad":
                return WEB_MERCATOR_QUAD;
            case "WorldCRS84Quad":
                return WORLD_CRS84_QUAD;
            case "WorldMercatorWGS84Quad":
                return WORLD_MERCATOR_WGS84_QUAD;
            default:
                throw new InternalServerErrorException("Unsupported tile matrix set: " + tileMatrixSetId);
        }
    }


}
