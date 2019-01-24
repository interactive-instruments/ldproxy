/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.EpsgCrs;

/**
 * This interface specifies the characteristics of tiling schemes / tile matrix sets for vector tiles.
 *
 * TODO: This is preliminary and will change as a result of discussions in the OGC Vector Tile Pilot.
 *
 * @author portele
 */
public interface TilingScheme {

    /**
     * fetch the identifier of the scheme; the id is used in the URI path
     * @return the identifier
     */
    String getId();

    /**
     * fetch the coordinate reference system that is the basis of this tiling scheme
     * @return the coordinate reference system
     */
    EpsgCrs getCrs();

    /**
     * fetch the bounding box of a tile
     * @param level the zoom level
     * @param row the row
     * @param col the column
     * @return the bounding box in the coordinate reference system of the tiling scheme
     */
    BoundingBox getBoundingBox(int level, int col, int row);

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
     * validate, whether the row is valid for the zoom level
     * @param level the zoom level
     * @param row the row
     * @return {@code true}, if the row is valid for the zoom level
     */
    boolean validateRow(int level, int row);

    /**
     * validate, whether the column is valid for the zoom level
     * @param level the zoom level
     * @param col the column
     * @return {@code true}, if the column is valid for the zoom level
     */
    boolean validateCol(int level, int col);
}
