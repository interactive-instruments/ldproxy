/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.EpsgCrs;

import java.util.List;

/**
 * This class provides derived information from a tile matrix set.
 */
public interface TileMatrixSet {

    String getId();

    EpsgCrs getCrs();

    /**
     * fetch the bounding box of a tile
     * @param level the zoom level
     * @param row the row
     * @param col the column
     * @return the bounding box in the coordinate reference system of the tiling scheme
     */
    BoundingBox getTileBoundingBox(int level, int col, int row);

    /**
     * determine the Douglas-Peucker distance parameter for a tile
     * @param level the zoom level
     * @param row the row
     * @param col the column
     * @return the distance in the units of measure of the coordinate references system of the tiling scheme
     */
    double getMaxAllowableOffset(int level, int row, int col);

    /**
     * determine the Douglas-Peucker distance parameter for a tile
     * @param level the zoom level
     * @param row the row
     * @param col the column
     * @param crs the target coordinate references system
     * @param crsTransformation a coordinate references system transformation object
     * @return the distance in the units of measure of the target coordinate references system
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    double getMaxAllowableOffset(int level, int row, int col, EpsgCrs crs, CrsTransformation crsTransformation) throws CrsTransformationException;

    /**
     * fetch the maximum zoom level, typically 24 or less
     * @return the maximum zoom level
     */
    int getMaxLevel();

    /**
     * fetch the minimum zoom level, typically 0 or more
     * @return the minimum zoom level
     */
    int getMinLevel();

    /**
     * fetch the width/height of a tile, typically 256x256
     * @return the width/height of a tile
     */
    int getTileSize();

    /**
     * to produce a smoother visualization, internally tiles may use a different coordinate system than the
     * width/height of a tile, typically 4096x4096
     * @return the width/height of a tile in the internal coordinate system
     */
    int getTileExtent();

    /**
     * Fetch tile matrix set data (e.g. id, crs, bounding box, tile matrices, etc.)
     * @return tile matrix set data
     */
    TileMatrixSetData getTileMatrixSetData();

    /**
     * Fetch the scale denominator on level 0
     * @return scale denominator on level 0
     */
    double getInitialScaleDenominator();

    /**
     * Fetch the width of the tile grid on level 0
     * @return initial width in tiles
     */
    int getInitialWidth();

    /**
     * Fetch the height of the tile grid on level 0 in tiles
     * @return initial height in tiles
     */
    int getInitialHeight();

    /**
     * Fetch the bounding box of the tile matrix set
     * @return bounding box
     */
    BoundingBox getBoundingBox();

    /**
     * validate, whether the row is valid for the zoom level
     * @param level the zoom level
     * @param row the row
     * @return {@code true}, if the row is valid for the zoom level
     */
    default boolean validateRow(int level, int row) {
        if (level < getMinLevel() || level > getMaxLevel()) {
            return false;
        }
        if (row < 0 || row > getInitialHeight() * Math.pow(2, level)) {
            return false;
        }
        return true;
    }

    /**
     * Validate, whether the column is valid for the zoom level
     * @param level the zoom level
     * @param col the column
     * @return {@code true}, if the column is valid for the zoom level
     */

    default boolean validateCol(int level, int col) {
        if (level < getMinLevel() || level > getMaxLevel()) {
            return false;
        }
        if (col < 0 || col > getInitialWidth() * Math.pow(2, level)) {
            return false;
        }
        return true;
    }

    /**
     * Generate tile matrix information
     * @param maxLevel maximum tile matrix level
     * @param initScaleDenominator value of the scale denominator on level 0
     * @param topLeftCorner coordinates of the top left corner of the map
     * @return list of tile matrices
     */
    default List<TileMatrix> generateTileMatrices(int maxLevel, double initScaleDenominator, double[] topLeftCorner) {
        ImmutableList.Builder<TileMatrix> tileMatrices = new ImmutableList.Builder<>();
        for (int i = 0; i <= maxLevel; i++) {
            TileMatrix tileMatrix = ImmutableTileMatrix.builder()
                    .identifier(String.valueOf(i))
                    .tileWidth(getTileSize())
                    .tileHeight(getTileSize())
                    .matrixWidth(getInitialWidth() * Math.pow(2, i))
                    .matrixHeight(getInitialHeight() * Math.pow(2, i))
                    .scaleDenominator(initScaleDenominator / Math.pow(2, i))
                    .topLeftCorner(topLeftCorner)
                    .build();
            tileMatrices.add(tileMatrix);
        }
        return tileMatrices.build();
    }
}
