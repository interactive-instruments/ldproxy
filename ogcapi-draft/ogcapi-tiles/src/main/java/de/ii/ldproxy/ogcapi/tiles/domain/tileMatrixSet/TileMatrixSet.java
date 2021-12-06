/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet.TileMatrixSetImpl;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class provides derived information from a tile matrix set.
 */
public interface TileMatrixSet {

    Logger LOGGER = LoggerFactory.getLogger(TileMatrixSet.class);

    static Optional<TileMatrixSet> fromWellKnownId(String tileMatrixSetId) {
        InputStream inputStream;
        try {
            inputStream = Resources.asByteSource(Resources.getResource(TileMatrixSetImpl.class, "/tileMatrixSets/" + tileMatrixSetId + ".json"))
                                   .openStream();
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Tile matrix set '{}' not found: {}", tileMatrixSetId, e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.error("Could not load tile matrix set '{}': {}", tileMatrixSetId, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace: ", e);
            }
            return Optional.empty();
        }

        return fromInputStream(inputStream, tileMatrixSetId);
    }

    static Optional<TileMatrixSet> fromDefinition(String tileMatrixSetId, Path customTileMatrixSetsStore) {
        File dir = customTileMatrixSetsStore.toFile();
        if (!dir.exists())
            dir.mkdirs();
        Optional<File> tileMatrixSetFile = Arrays.stream(Objects.requireNonNullElse(dir.listFiles(), ImmutableList.of().toArray(File[]::new)))
                                                 .filter(file -> !file.isHidden())
                                                 .filter(file -> Files.getFileExtension(file.getName()).equals("json"))
                                                 .filter(file -> Files.getNameWithoutExtension(file.getName()).equals(tileMatrixSetId))
                                                 .findAny();
        if (tileMatrixSetFile.isEmpty()) {
            LOGGER.debug("Tile matrix set '{}' not found.", tileMatrixSetId);
            return Optional.empty();
        }

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(tileMatrixSetFile.get());
        } catch (FileNotFoundException e) {
            LOGGER.debug("Tile matrix set '{}' not found: {}", tileMatrixSetId, e.getMessage());
            return Optional.empty();
        }

        return fromInputStream(inputStream, tileMatrixSetId);
    }

    static Optional<TileMatrixSet> fromInputStream(InputStream tileMatrixSetInputStream, String tileMatrixSetId) {
        // prepare Jackson mapper for deserialization
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new GuavaModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

        TileMatrixSetData data;
        try {
            data = mapper.readValue(tileMatrixSetInputStream, TileMatrixSetData.class);
        } catch (IOException e) {
            LOGGER.error("Could not deserialize tile matrix set '{}': {}", tileMatrixSetId, e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace: ", e);
            }
            return Optional.empty();
        }

        return Optional.of(new TileMatrixSetImpl(data));
    }

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
     * Fetch the bounding box of the tile matrix set in the CRS of the tile matrix set
     * @return bounding box
     */
    BoundingBox getBoundingBox();

    /**
     * Fetch the bounding box of the tile matrix set in CRS84
     * @return bounding box
     */
    BoundingBox getBoundingBoxCrs84(CrsTransformerFactory crsTransformerFactory) throws CrsTransformationException;

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
    TileMatrix getTileMatrix(int level);

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

    /**
     * convert a row in the XYZ scheme to a row in the TMS scheme
     * @param level level of the row
     * @param row row number in the XYZ scheme
     * @return row number in the TMS scheme
     */
    default int getTmsRow(int level, int row) {
        return getRows(level) - 1 - row;
    }
}
