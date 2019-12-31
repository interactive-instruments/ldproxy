/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.api.BoundingBox;

import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class MultitilesUtils {

    private MultitilesUtils() {

    }

    /**
     * Parse scaleDenominator parameter from the request
     * If missing, the whole extent of tile matrices supported by the tile matrix set is returned
     * @param sd scale denominator
     * @return list of all possible scales/tile matrices where tiles will be retrieved
     */
    public static List<Integer> parseScaleDenominator(String sd, TileMatrixSet tileMatrixSet) {
        Double[] values = {(double) tileMatrixSet.getMinLevel(), (double) tileMatrixSet.getMaxLevel()};
        if (sd != null && !sd.trim().isEmpty()) {
            values = Stream.of(sd.split(","))
                    .map(Double::parseDouble)
                    .toArray(Double[]::new);
        }
        if (values.length != 2 || values[0] >= values[1] || values[0] < tileMatrixSet.getMinLevel()
                || values[1] > tileMatrixSet.getMaxLevel()) {
            throw new NotFoundException("Scale denominator invalid or out-of-range");
        }
        List<Integer> tileMatrices = new ArrayList<>();
        for(int i = (int) Math.ceil(values[0]); i < values[1]; i++) {
            tileMatrices.add(i);

        }
        return tileMatrices;

    }

    /**
     * Parse bounding box parameter from the request
     * If unspecified, the whole extent of the map is returned
     * @param csv comma-separated string with coordinates
     * @return bounding box as an array of doubles
     */
    public static double[] parseBbox(String csv, TileMatrixSet tileMatrixSet) {
        if (csv == null || csv.trim().isEmpty()) {
            BoundingBox bbox = tileMatrixSet.getBoundingBox();
            return new double[]{bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax()};
        }
        double[] bbox = Stream.of(csv.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
        if (bbox.length != 4) {
            throw new NotFoundException("Incorrect number of arguments in the bbox parameter");
        }
        return bbox;
    }

    /**
     * Parse the value of the request parameter f-tile
     * @param tileFormatParam parameter value
     * @return tile format
     */
    public static String parseTileFormat(String tileFormatParam) {
        if (tileFormatParam == null || tileFormatParam.trim().isEmpty() || "json".equals(tileFormatParam)) {
            return "json";
        } else if ("mvt".equals(tileFormatParam)) {
            return tileFormatParam;
        }
        throw new NotFoundException("Unknown value of the tile format parameter");
    }

    /**
     * Convert (lon, lat) point coordinates to the coordinates of the enclosing tile
     * @param lon longitude coordinate of the point
     * @param lat latitude coordinate of the point
     * @param tileMatrix zoom level
     * @return list with XY coordinates of the tile in the grid
     */
    public static List<Integer> pointToTile(double lon, double lat, int tileMatrix, TileMatrixSet tileMatrixSet) {
        BoundingBox mapExtent = tileMatrixSet.getBoundingBox();
        double rows = Math.pow(2, tileMatrix) * tileMatrixSet.getInitialHeight();
        double cols = Math.pow(2, tileMatrix) * tileMatrixSet.getInitialWidth();
        double tileWidth = (mapExtent.getXmax() - mapExtent.getXmin()) / cols;
        double tileHeight = (mapExtent.getYmax() - mapExtent.getYmin()) / rows;

        int tileCol = (int) Math.floor((mapExtent.getXmax() + lon) / tileWidth);
        int tileRow = (int) Math.floor((mapExtent.getYmax() - lat) / tileHeight);

        return ImmutableList.of(tileRow, tileCol);
    }

}