/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ContentExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * This class provides derived information from a tile matrix set.
 */
public interface TileMatrixSet extends ContentExtension {

    @Override
    default String getResourceName() { return "TileMatrixSet"; }

    /**
     * fetch the local identifier for the tiling scheme
     * @return the identifier, e.g. "WebMercatorQuad"
     */
    String getId();

    /**
     * fetch the local identifier for the tiling scheme
     * @return the identifier, e.g. "WebMercatorQuad"
     */
    default Optional<URI> getURI() { return Optional.empty(); }

    /**
     * fetch a title of the tiling scheme for presentation to humans
     * @return the title, e.g. "Google Maps Compatible for the World"
     */
    default Optional<String> getTitle() {
        return Optional.empty();
    }

    /**
     * fetch a description of the tiling scheme for presentation to humans
     * @return the description, e.g. "The most common TileMatrixSet, used in most of the main IT map browsers. It was initially popularized by Google Maps."
     */
    default Optional<String> getDescription() { return Optional.empty(); }

    /**
     * fetch a list of keywords relevant for the tiling scheme
     * @return the keywords
     */
    default List<String> getKeywords() {
        return ImmutableList.of();
    }

    /**
     * fetch the base coordinate reference system of the tiling scheme
     * @return the CRS
     */
    EpsgCrs getCrs();

    /**
     * fetch the axes labels in the correct order
     * @return the axes labels
     */
    default List<String> getOrderedAxes() {
        return ImmutableList.of();
    }

    /**
     * fetch a well known scale set URI, if one exists
     * @return the URI
     */
    default Optional<URI> getWellKnownScaleSet() {
        return Optional.empty();
    }

    /**
     * fetch the bounding box of a tile
     * @param level the zoom level
     * @param row the row
     * @param col the column
     * @return the bounding box in the coordinate reference system of the tiling scheme
     */
    default BoundingBox getTileBoundingBox(int level, int col, int row) {
        BoundingBox bbox = getBoundingBox();
        double rows = getRows(level);
        double cols = getCols(level);
        double tileWidth = (bbox.getXmax()-bbox.getXmin()) / cols;
        double tileHeight = (bbox.getYmax()-bbox.getYmin()) / rows;
        double minX = bbox.getXmin() + tileWidth * col;
        double maxX = minX + tileWidth;
        double maxY = bbox.getYmax() - tileHeight * row;
        double minY = maxY - tileHeight;
        return BoundingBox.of(minX, minY, maxX, maxY, getCrs());
    }

    /**
     * determine the number of columns for a tile matrix
     * @param level the zoom level
     * @return the number of columns
     */
    default int getCols(int level) {
        return (int) Math.round(Math.pow(2, level) * getInitialWidth());
    }

    /**
     * determine the number of rows for a tile matrix
     * @param level the zoom level
     * @return the number of rows
     */
    default int getRows(int level) {
        return (int) Math.round(Math.pow(2, level) * getInitialHeight());
    }

    /**
     * determine the Douglas-Peucker distance parameter for a tile
     * @param level the zoom level
     * @param row the row
     * @param col the column
     * @return the distance in the units of measure of the coordinate references system of the tiling scheme
     */
    default double getMaxAllowableOffset(int level, int row, int col) {
        BoundingBox bbox = getBoundingBox();
        return (bbox.getXmax()-bbox.getXmin()) / getCols(level) / getTileExtent();
    }

    /**
     * determine the Douglas-Peucker distance parameter for a tile
     * @param level the zoom level
     * @param row the row
     * @param col the column
     * @param crs the target coordinate references system
     * @param crsTransformerFactory a coordinate references system transformation object
     * @return the distance in the units of measure of the target coordinate references system
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    default double getMaxAllowableOffset(int level, int row, int col, EpsgCrs crs, CrsTransformerFactory crsTransformerFactory) throws CrsTransformationException {
        BoundingBox bbox = getTileBoundingBox(level, col, row);
        Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(getCrs(), crs);
        if (transformer.isPresent()) {
            bbox = transformer.get().transformBoundingBox(bbox);
        }
        return (bbox.getXmax()-bbox.getXmin())/getTileExtent();
    }

    /**
     * get the number of meters for each coordinate unit.
     * In a CRS with coordinates expressed in meters, 1.0. This is set as a default.
     * In CRS with coordinates expressed in degrees the result is 360/(EquatorialRadius*2*PI).
     * @return the number of meters
     */
    default double getMetersPerUnit() { return 1.0; }

    /**
     * fetch the maximum zoom level, typically 24 or less
     * @return the maximum zoom level
     */
    int getMaxLevel();

    /**
     * fetch the minimum zoom level, typically 0 or more
     * @return the minimum zoom level
     */
    default int getMinLevel() {
        return 0;
    }

    /**
     * fetch the width/height of a tile, typically 256x256
     * @return the width/height of a tile
     */
    default int getTileSize() {
        return 256;
    }

    /**
     * to produce a smoother visualization, internally tiles may use a different coordinate system than the
     * width/height of a tile, typically 4096x4096
     * @return the width/height of a tile in the internal coordinate system
     */
    default int getTileExtent() {
        return 4096;
    }

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
        return row >= 0 && row <= getRows(level);
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
        return col >= 0 && col <= getCols(level);
    }

    /**
     * Generate tile matrix information
     * @param minLevel minimum tile matrix level
     * @param maxLevel maximum tile matrix level
     * @return list of tile matrices
     */
    default List<TileMatrix> getTileMatrices(int minLevel, int maxLevel) {
        ImmutableList.Builder<TileMatrix> tileMatrices = new ImmutableList.Builder<>();
        for (int i = minLevel; i <= maxLevel; i++) {
            TileMatrix tileMatrix = getTileMatrix(i);
            tileMatrices.add(tileMatrix);
        }
        return tileMatrices.build();
    }

    /**
     * Generate tile matrix information for a single level
     * @param level the tile matrix level
     * @return the tile matrix
     */
    default TileMatrix getTileMatrix(int level) {
        double initScaleDenominator = getInitialScaleDenominator();
        BoundingBox bbox = getBoundingBox();
        return ImmutableTileMatrix.builder()
                                  .tileLevel(level)
                                  .tileWidth(getTileSize())
                                  .tileHeight(getTileSize())
                                  .matrixWidth(getCols(level))
                                  .matrixHeight(getRows(level))
                                  .metersPerUnit(getMetersPerUnit())
                                  .scaleDenominator(getBigDecimal(initScaleDenominator / Math.pow(2, level)))
                                  .pointOfOrigin(new BigDecimal[]{ getBigDecimal(bbox.getXmin()), getBigDecimal(bbox.getYmax()) })
                                  .build();
    }

    default BigDecimal getBigDecimal(double value) {
        BigDecimal decimalValue = new BigDecimal(value);
        return decimalValue.setScale(TileMatrix.SIGNIFICANT_DIGITS - decimalValue.precision() + decimalValue.scale(), RoundingMode.HALF_UP)
                           .stripTrailingZeros();
    }

    /**
     * Get the tile coordinates for a point (in the CRS of the tile matrix set) and a zoom level
     * @param x first coordinate of the point
     * @param y second coordinate of the point
     * @param level zoom level
     * @return list with row/col coordinates of the tile in the grid
     */
    default List<Integer> getRowCol(double x, double y, int level) {
        BoundingBox bbox = getBoundingBox();
        int cols = getCols(level);
        int rows = getRows(level);
        double tileWidth = (bbox.getXmax() - bbox.getXmin()) / cols;
        double tileHeight = (bbox.getYmax() - bbox.getYmin()) / rows;

        int tileCol = (int) Math.floor((x - bbox.getXmin()) / tileWidth);
        int tileRow = (int) Math.floor((bbox.getYmax() - y) / tileHeight);

        return ImmutableList.of(Math.min(Math.max(tileRow,0),rows-1), Math.min(Math.max(tileCol,0),cols-1));
    }

    /**
     * Construct the TileMatrixSetLimits for the given bounding box and a tile matrix
     * @param level tile matrix / zoom level
     * @param bbox bounding box in the CRS of the tile matrix set
     * @return list of TileMatrixSetLimits
     */
    default TileMatrixSetLimits getLimits(int level, BoundingBox bbox) {
        List<Integer> upperLeftCornerTile = getRowCol(bbox.getXmin(), bbox.getYmax(), level);
        List<Integer> lowerRightCornerTile = getRowCol(bbox.getXmax(), bbox.getYmin(), level);
        return new ImmutableTileMatrixSetLimits.Builder()
                .minTileRow(upperLeftCornerTile.get(0))
                .maxTileRow(lowerRightCornerTile.get(0))
                .minTileCol(upperLeftCornerTile.get(1))
                .maxTileCol(lowerRightCornerTile.get(1))
                .tileMatrix(Integer.toString(level))
                .build();
    }

    /**
     * Construct a list of TileMatrixSetLimits for the given bounding box and tileMatrix range
     * @param tileMatrixRange range of tileMatrix values
     * @param bbox bounding box in the CRS of the tile matrix set
     * @return list of TileMatrixSetLimits
     */
    default List<TileMatrixSetLimits> getLimitsList(MinMax tileMatrixRange, BoundingBox bbox) {
        ImmutableList.Builder<TileMatrixSetLimits> limits = new ImmutableList.Builder<>();
        for (int tileMatrix = tileMatrixRange.getMin(); tileMatrix <= tileMatrixRange.getMax(); tileMatrix++) {
            limits.add(getLimits(tileMatrix, bbox));
        }
        return limits.build();
    }
}
