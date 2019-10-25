/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
@Provides
@Instantiate
public class MultitilesGenerator implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultitilesGenerator.class);

    private static final TileMatrixSet TILE_MATRIX_SET = new DefaultTileMatrixSet();

    private static final double INITIAL_RESOLUTION = 2 * Math.PI * 6378137 / TILE_MATRIX_SET.getTileSize();

    MultitilesGenerator() {
    }


    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/multitiles";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * Construct a response for a multiple tiles request
     * @param tileMatrixSetId identifier of tile matrix set
     * @param bboxParam value of the bbox request parameter
     * @param scaleDenominatorParam value of the scaleDenominator request parameter
     * @param multiTileType value of the multiTileType request parameter
     * @param uriCustomizer uri customizer
     * @return multiple tiles
     */
    Response getMultitiles(String tileMatrixSetId, String bboxParam, String scaleDenominatorParam, String multiTileType,
                           URICustomizer uriCustomizer, String tileFormat) {

        checkTileMatrixSet(tileMatrixSetId);
        List<Integer> tileMatrices = parseScaleDenominator(scaleDenominatorParam);
        double[] bbox = parseBbox(bboxParam);
        LOGGER.debug("GET TILE MULTITILES {} {}-{} {}", bbox, tileMatrices.get(0), tileMatrices.get(tileMatrices.size()-1), multiTileType);
        List<TileSetEntry> tileSetEntries = generateTilesetEntries(bbox, tileMatrices, uriCustomizer, getTileFormat(tileFormat));

        if ("url".equals(multiTileType)) {
            return Response.ok(ImmutableMap.of("tileSet", tileSetEntries))
                    .type("application/geo+json")
                    .build();
        } else {
            //generateZip(tileSetEntries)
            return Response.ok(null)
                    .type("application/zip")
                    .build();

        }

    }

    /**
     * Check if tileMatrixSet is supported
     * @param tileMatrixSetId the local identifier of a specific tile matrix set
     */
    protected static void checkTileMatrixSet(String tileMatrixSetId) {
        if (!TILE_MATRIX_SET.getId().equals(tileMatrixSetId)) {
            throw new NotFoundException("Unsupported tile matrix set");
        }
    }

    /**
     * Check if the value of multiTileType is valid
     * @param multiTileType multiTileType parameter's value
     * @return true if the value is supported
     * @throws NotFoundException when the value is unsupported or incorrect
     */
    protected boolean checkMultiTileTypeParameter(String multiTileType) {
        if (multiTileType == null || "url".equals(multiTileType) || "tiles".equals(multiTileType) || "full".equals(multiTileType)) {
            return true;
        }
        throw new NotFoundException("Invalid value of multiTileType parameter");
    }

    /**
     * Parse scaleDenominator parameter from the request
     * If missing, the whole extent of tile matrices supported by the tile matrix set is returned
     * @param sd scale denominator
     * @return list of all possible scales/tile matrices where tiles will be retrieved
     */
    protected static List<Integer> parseScaleDenominator(String sd) {
        Double[] values = {(double) TILE_MATRIX_SET.getMinLevel(), (double) TILE_MATRIX_SET.getMaxLevel()};
        if (sd != null && !sd.trim().isEmpty()) {
            values = Stream.of(sd.split(","))
                    .map(Double::parseDouble)
                    .toArray(Double[]::new);
        }
        if (values.length != 2 || values[0] >= values[1] || values[0] < TILE_MATRIX_SET.getMinLevel()
                || values[1] > TILE_MATRIX_SET.getMaxLevel()) {
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
    protected static double[] parseBbox(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new double[]{-20026376.39, -20048966.10, 20026376.39, 20048966.10};
        }
        double[] bbox = Stream.of(csv.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
        if (bbox.length != 4) {
            throw new NotFoundException("Incorrect number of arguments in the bbox parameter");
        }
        return bbox;
    }

    protected static String getTileFormat(String tileFormat) {
        if (tileFormat == null || tileFormat.trim().isEmpty() || "json".equals(tileFormat)) {
            return "json";
        } else if ("mvt".equals(tileFormat)) {
            return tileFormat;
        }
        throw new NotFoundException("Unknown value of the tile format parameter");
    }

    /**
     * Generate a list of tiles that cover the bounding box for given tile matrices
     * @param bbox bounding box specified by two points and their longitude and latitude coordinates (WGS 84)
     * @param tileMatrices all tile matrices to be retrieved
     * @param uriCustomizer uri customizer
     * @return list of TileSet objects
     */
    private List<TileSetEntry> generateTilesetEntries(double[] bbox, List<Integer> tileMatrices, URICustomizer uriCustomizer,
                                                      String tileFormat) {
        List<TileSetEntry> tileSets = new ArrayList<>();
        for (int tileMatrix : tileMatrices) {
            List<Integer> bottomTile = pointToTile(bbox[0], bbox[1], tileMatrix);
            List<Integer> topTile = pointToTile(bbox[2], bbox[3], tileMatrix);
            for (int row = bottomTile.get(0); row <= topTile.get(0); row++){
                for (int col = topTile.get(1); col <= bottomTile.get(1); col++) {
                    tileSets.add(ImmutableTileSetEntry.builder()
                            .tileURL(uriCustomizer.copy()
                                    .clearParameters()
                                    .ensureLastPathSegments(Integer.toString(tileMatrix), Integer.toString(row), Integer.toString(col))
                                    .ensureNoTrailingSlash()
                                    .addParameter("f", tileFormat)
                                    .toString())
                            .tileMatrix(tileMatrix)
                            .tileRow(row)
                            .tileCol(col)
                            .build());
                }
            }
        }
        return tileSets;
    }

    /**
     * Convert point coordinates to coordinates of the enclosing tile
     * @param lon longitude coordinate of the point
     * @param lat latitude coordinate of the point
     * @param tileMatrix zoom level
     * @return list with XY coordinates of the tile in the grid
     */
    protected static List<Integer> pointToTile(double lon, double lat, int tileMatrix) {
        // convert the point from EPSG:3857 to pyramid pixel coordinates in given zoom level
        double resolution = INITIAL_RESOLUTION / Math.pow(2, tileMatrix);
        double originShift = 2 * Math.PI * 6378137 / 2.0;
        double px = (lon + originShift) / resolution;
        double py = (lat + originShift) / resolution;
        // convert pyramid pixel coordinates to the coordinates of the enclosing tile
        return pixelsToTile(px, py, tileMatrix);
    }

    /**
     * Determine a tile covering the region in given pixel coordinates
     * @param px x-coordinate of the pixel
     * @param py y-coordinate of the pixel
     * @param tileMatrix zoom level
     * @return list with XY coordinates of the tile in the grid
     */
    protected static List<Integer> pixelsToTile(double px, double py, int tileMatrix) {
        int tileX = (int) (Math.ceil(px / TILE_MATRIX_SET.getTileSize()) - 1);
        int tileY = (int) (Math.pow(2, tileMatrix) - Math.ceil(py / TILE_MATRIX_SET.getTileSize()));
        return ImmutableList.of(tileX, tileY);
    }

    private File generateZip(List<TileSetEntry> tileSetEntries, String tileMatrixSetId, boolean isFull) {

        return null;
    }


}
