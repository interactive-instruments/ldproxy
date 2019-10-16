/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import de.ii.xtraplatform.crs.api.*;

/**
 * This is the most commonly used tiling scheme. It is used by Google Maps and most other web mapping applications.
 * In WMTS it is called "Google Maps Compatible", in the Tile Matrix Set standard "WebMercatorQuad".
 *
 */
public class WebMercatorQuad implements TileMatrixSet {

    /**
     * Web Mercator is the coordinate reference system of the tiling scheme, EPSG code is 3857
     */
    private static final EpsgCrs CRS = new EpsgCrs(3857);

    /**
     * The tile size is fixed to 256x256
     */
    private static final int TILE_SIZE = 256;

    /**
     * Based on experience the tile extent is set to 4096x4096 for a smoother look, since most display can show
     * more details in a "pixel" of the 256x256 tile
     */
    private static final int TILE_EXTENT = 4096;

    private TileMatrixSetData data;

    WebMercatorQuad() {
        // TODO
    }

    /**
     * @return for the default tiling scheme, a fixed id "WebMercatorQuad" is used
     */
    @Override
    public String getId() {
        return "WebMercatorQuad";
    };


    /**
     * @return the coordinate reference system of the default tiling scheme is EPSG 3857
     */
    @Override
    public EpsgCrs getCrs() {
        return CRS;
    };

    /**
     * Fetch a Bounding box from a tile.
     *
     * @param level the zoom level
     * @param row the row number
     * @param col the column number
     * @return the bounding box of the tiling scheme in the coordinate reference system EPSG 3857
     */
    @Override
    public BoundingBox getBoundingBox(int level, int col, int row) {

        // TODO optimize computations
        double x1 = -20037508.3427892;
        double x2 = 20037508.3427892;
        double y1 = -20037508.3427892;
        double y2 = 20037508.3427892;
        double rows = Math.pow(2, level);
        double cols = Math.pow(2, level);
        double tileWidth = (x2 - x1) / cols;
        double tileHeight = (y2 - y1) / rows;
        double minX = x1 + tileWidth * col;
        double maxX = minX + tileWidth;
        double maxY = y2 - tileHeight * row;
        double minY = maxY - tileHeight;
        return new BoundingBox(minX, minY, maxX, maxY, CRS);
    };


    /**
     * determine the Douglas-Peucker distance parameter for a tile
     *
     * @param level the zoom level
     * @param row the row number
     * @param col the column number
     * @return the distance in the units of measure of the coordinate references system EPSG 3857 of the tiling scheme
     */
    @Override
    public double getMaxAllowableOffset(int level, int row, int col) {
        return 40075016.6856 / Math.pow(2, level) / TILE_EXTENT;
    }
    /**
     * determine the Douglas-Peucker distance parameter for a tile
     *
     * @param level the zoom level
     * @param row the row number
     * @param col the column number
     * @param crs  the target coordinate references system
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the distance in the units of measure of the target coordinate references system
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    @Override
    public double getMaxAllowableOffset(int level, int row, int col, EpsgCrs crs, CrsTransformation crsTransformation) throws CrsTransformationException {
        BoundingBox bbox = getBoundingBox(level, col, row);
        if (crs!=null && !crs.equals(CRS)) {
            CrsTransformer transformer = crsTransformation.getTransformer(CRS, crs);
            BoundingBox bboxCrs = transformer.transformBoundingBox(bbox);
            bbox = bboxCrs;
        }
        return (bbox.getXmax()-bbox.getXmin())/TILE_EXTENT;
    }

    /**
     * @return the maximum zoom level for the default tiling scheme is 24
     */
    @Override
    public int getMaxLevel() {
        return 24;
    };
    /**
     * @return the minimum zoom level for the default tiling schemes is 0
     */
    @Override
    public int getMinLevel() {
        return 0;
    };

    /**
     * fetch the width/height of a tile, typically 256x256
     * @return the width/height of a tile
     */
    @Override
    public int getTileSize() { return TILE_SIZE; };

    /**
     * to produce a smoother visualization, internally tiles may use a different coordinate system than the
     * width/height of a tile, typically 4096x4096
     * @return the width/height of a tile in the internal coordinate system
     */
    @Override
    public int getTileExtent() {
        return TILE_EXTENT;
    }
    /**
     * validate, whether the row is valid for the zoom level
     * @param level the zoom level
     * @param row the row
     * @return {@code true}, if the row is valid for the zoom level
     */
    @Override
    public boolean validateRow(int level, int row) {
        if (level<getMinLevel() || level>getMaxLevel())
            return false;
        return true;
    }
    /**
     * validate, whether the column is valid for the zoom level
     * @param level the zoom level
     * @param col the column
     * @return {@code true}, if the column is valid for the zoom level
     */
    @Override
    public boolean validateCol(int level, int col) {
        if (level<getMinLevel() || level>getMaxLevel())
            return false;
        return true;
    }
}
