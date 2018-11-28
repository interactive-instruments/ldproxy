/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import de.ii.ldproxy.wfs3.ImmutableFeatureTransformationContextGeneric;
import de.ii.ldproxy.wfs3.ImmutableWfs3RequestContextImpl;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.*;
import de.ii.ldproxy.wfs3.core.Wfs3EndpointCore;
import de.ii.xtraplatform.crs.api.*;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static de.ii.ldproxy.wfs3.vt.TilesConfiguration.EXTENSION_KEY;


/**
 * This class represents a vector tile
 *
 * @author portele
 */
class VectorTile {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);

    private final int level;
    private final int row;
    private final int col;
    private final String collectionId;
    private final TilingScheme tilingScheme;
    private final Wfs3ServiceData serviceData;
    private final TransformingFeatureProvider featureProvider;
    private final boolean temporary;
    private final String fileName;
    private final Wfs3OutputFormatExtension wfs3OutputFormatGeoJson;


    /**
     * specify the vector tile
     *  @param collectionId            the id of the collection in which the tile belongs
     * @param tilingSchemeId          the local identifier of a specific tiling scheme
     * @param level                   the zoom level as a string
     * @param row                     the row number as a string
     * @param col                     the column number as a string
     * @param serviceData             the wfs3 service Data
     * @param temporary               the info, if this is a temporary or permanent vector tile
     * @param cache                   the tile cache
     * @param featureProvider         the wfs3 service feature provider
     * @param wfs3OutputFormatGeoJson
     */

    VectorTile(String collectionId, String tilingSchemeId, String level, String row, String col, Wfs3ServiceData serviceData, boolean temporary, VectorTilesCache cache, TransformingFeatureProvider featureProvider, Wfs3OutputFormatExtension wfs3OutputFormatGeoJson) throws FileNotFoundException {
        this.wfs3OutputFormatGeoJson = wfs3OutputFormatGeoJson;

        // check and process parameters

        // check, if collectionId is valid
        if (collectionId != null) {
            Set<String> collectionIds = serviceData.getFeatureTypes()
                                                   .keySet();
            if (collectionId.isEmpty() || !collectionIds.contains(collectionId)) {
                throw new NotFoundException();
            }
        }
        this.collectionId = collectionId;

        // get the TilingScheme
        if (tilingSchemeId.equalsIgnoreCase("default")) {
            tilingScheme = new DefaultTilingScheme();
        } else {
            throw new NotFoundException();
            // TODO implement loading from a file
            // File file = cache.getTilingScheme(tilingSchemeId);
            // tilingScheme = new TilingSchemeImpl(file);
        }

        this.level = checkZoomLevel(tilingScheme, level);
        if (this.level == -1)
            throw new FileNotFoundException();

        this.row = checkRow(tilingScheme, this.level, row);
        if (this.row == -1)
            throw new FileNotFoundException();

        this.col = checkColumn(tilingScheme, this.level, col);
        if (this.col == -1)
            throw new FileNotFoundException();

        this.serviceData = serviceData;

        this.featureProvider = featureProvider;
        this.temporary = temporary;

        if (this.temporary) {
            fileName = UUID.randomUUID()
                           .toString();
        } else {
            fileName = String.format("%s_%s_%s", Integer.toString(this.level), Integer.toString(this.row), Integer.toString(this.col));
        }
    }

    public int getLevel(){
        return level;
    }

    public int getRow(){
        return row;
    }

    public int getCol(){
        return col;
    }

    public String getCollectionId(){
        return collectionId;
    }


    public Wfs3OutputFormatExtension getWfs3OutputFormatGeoJson(){
        return wfs3OutputFormatGeoJson;
    }

    /**
     * @return the tiling scheme object
     */
    public TilingScheme getTilingScheme() {
        return tilingScheme;
    }

    public Wfs3ServiceData getServiceData(){
        return serviceData;
    }

    public TransformingFeatureProvider getFeatureProvider(){
        return featureProvider;
    }

    public boolean getTemporary(){
        return  temporary;
    }


    /**
     * Verify that the zoom level is an integer value in the valid range for the tiling scheme
     *
     * @param tilingScheme the tiling scheme used in the request
     * @param level        the zoom level as a string
     * @return the zoom level as an integer, or -1 in case of an invalid zoom level
     */
    private int checkZoomLevel(TilingScheme tilingScheme, String level) {
        int l;
        try {
            l = Integer.parseInt(level);
            if (l > tilingScheme.getMaxLevel() || l < tilingScheme.getMinLevel()) {
                l = -1;
            }
        } catch (NumberFormatException e) {
            l = -1;
        }
        return l;
    }

    /**
     * Verify that the row number is an integer value
     *
     * @param tilingScheme the tiling scheme used in the request
     * @param level        the zoom level
     * @param row          the row number as a string
     * @return the row number as an integer, or -1 in case of an invalid value
     */
    private int checkRow(TilingScheme tilingScheme, int level, String row) {
        int r;
        try {
            r = Integer.parseInt(row);
            if (!tilingScheme.validateRow(level, r))
                r = -1;
        } catch (NumberFormatException e) {
            r = -1;
        }
        return r;
    }

    /**
     * Verify that the column number is an integer value
     *
     * @param tilingScheme the tiling scheme used in the request
     * @param level        the zoom level
     * @param col          the column number as a string
     * @return the column number as an integer, or -1 in case of an invalid value
     */
    private int checkColumn(TilingScheme tilingScheme, int level, String col) {
        int c;
        try {
            c = Integer.parseInt(col);
            if (!tilingScheme.validateCol(level, c))
                c = -1;
        } catch (NumberFormatException e) {
            c = -1;
        }
        return c;
    }

    /**
     * fetch the file object of a tile in the cache
     *
     * @param cache     the tile cache
     * @param extension the file extension of the cached file
     * @return the file object of the tile in the cache
     */
    File getFile(VectorTilesCache cache, String extension) {
        return new File(this.getTileDirectory(cache), String.format("%s.%s", fileName, extension));
    }

    /**
     * retrieve the subdirectory in the tiles directory for the selected service, tiling scheme, etc.
     *
     * @param cache the tile cache
     * @return the directory with the cached tile files
     */
    private File getTileDirectory(VectorTilesCache cache) {
        File subDir;
        if (temporary)
            subDir = cache.getTmpDirectory();
        else
            subDir = new File(new File(new File(cache.getTilesDirectory(), serviceData.getId()), (collectionId == null ? "__all__" : collectionId)), tilingScheme.getId());

        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        return subDir;
    }



    /**
     * generate the spatial filter for a tile in the native coordinate reference system of the dataset in CQL
     *
     * @param geometryField     the name of the geometry field to use for the filter
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the spatial filter in CQL
     * @throws CrsTransformationException if the bounding box could not be converted
     */
    public String getSpatialFilter(String geometryField, CrsTransformation crsTransformation) throws CrsTransformationException {

        // calculate bbox in the native CRS of the dataset
        BoundingBox bboxNativeCrs = getBoundingBoxNativeCrs(crsTransformation);

        return String.format(Locale.US, "BBOX(%s, %.3f, %.3f, %.3f, %.3f, '%s')", geometryField, bboxNativeCrs.getXmin(), bboxNativeCrs.getYmin(), bboxNativeCrs.getXmax(), bboxNativeCrs.getYmax(), bboxNativeCrs.getEpsgCrs()
                                                                                                                                                                                                                  .getAsSimple());
    }

    /**
     * Creates an affine transformation for converting geometries in lon/lat to tile coordinates.
     *
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the transform
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    public AffineTransformation createTransformLonLatToTile(CrsTransformation crsTransformation) throws CrsTransformationException {

        BoundingBox bbox = getBoundingBox(DEFAULT_CRS, crsTransformation);

        double lonMin = bbox.getXmin();
        double lonMax = bbox.getXmax();
        double latMin = bbox.getYmin();
        double latMax = bbox.getYmax();

        double tileSize = tilingScheme.getTileSize();
        double xScale = tileSize / (lonMax - lonMin);
        double yScale = tileSize / (latMax - latMin);

        double xOffset = -lonMin * xScale;
        double yOffset = latMin * yScale + tileSize;

        return new AffineTransformation(xScale, 0.0d, xOffset, 0.0d, -yScale, yOffset);
    }


    /**
     * @return the bounding box of the tiling scheme object
     */
    private BoundingBox getBoundingBox() {
        return tilingScheme.getBoundingBox(level, col, row);
    }

    /**
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the bounding box of the tiling scheme in the form of the native crs
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    private BoundingBox getBoundingBoxNativeCrs(CrsTransformation crsTransformation) throws CrsTransformationException {
        EpsgCrs crs = serviceData.getFeatureProvider()
                                 .getNativeCrs();
        BoundingBox bboxTilingSchemeCrs = getBoundingBox();
        if (crs == tilingScheme.getCrs())
            return bboxTilingSchemeCrs;

        CrsTransformer transformer = crsTransformation.getTransformer(tilingScheme.getCrs(), crs);
        return transformer.transformBoundingBox(bboxTilingSchemeCrs);
    }

    /**
     * @param crs               the target coordinate references system
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the bounding box of the tiling scheme in the form of the target crs
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    private BoundingBox getBoundingBox(EpsgCrs crs, CrsTransformation crsTransformation) throws CrsTransformationException {
        BoundingBox bboxTilingSchemeCrs = getBoundingBox();
        if (crs == tilingScheme.getCrs())
            return bboxTilingSchemeCrs;

        CrsTransformer transformer = crsTransformation.getTransformer(tilingScheme.getCrs(), crs);
        return transformer.transformBoundingBox(bboxTilingSchemeCrs);
    }

    /**
     * @param filters           filters specified in the query
     * @param filterableFields  all possible fields you can use as a filter
     * @return the CQL from the delivered filters
     */
    public String getCQLFromFilters(Map<String, String> filters, Map<String, String> filterableFields) {
        return filters.entrySet()
                      .stream()
                      .map(f -> {
                          if (f.getKey()
                               .equals("time")) {
                              try {
                                  Interval fromIso8601Period = Interval.parse(f.getValue());
                                  return String.format("%s DURING %s", filterableFields.get(f.getKey()), fromIso8601Period);
                              } catch (DateTimeParseException ignore) {
                                  try {
                                      Instant fromIso8601 = Instant.parse(f.getValue());
                                      return String.format("%s TEQUALS %s", filterableFields.get(f.getKey()), fromIso8601);
                                  } catch (DateTimeParseException e) {
                                      LOGGER.debug("TIME PARSER ERROR", e);
                                      throw new BadRequestException();
                                  }
                              }
                          }
                          if (f.getValue()
                               .contains("*")) {
                              return String.format("%s LIKE '%s'", filterableFields.get(f.getKey()), f.getValue());
                          }

                          return String.format("%s = '%s'", filterableFields.get(f.getKey()), f.getValue());
                      })
                      .collect(Collectors.joining(" AND "));
    }


    /**
     * checks if the requested format for the tile is specified in the Config and therefore valid. If it is not valid then throw a NotAcceptableException.
     *
     * @param serviceData       the wfs3ServiceData
     * @param collectionId      the id of the collection you want to check the formats for
     * @param mediaType         the requested format
     * @param forLinksOrDataset boolean, false if it is requested from a collection, true if requested from Dataset or Link.
     * @return false if forLinksOrDataset is true and the format is not supported.
     *         NotAcceptableException if forLinksOrDataset is false and the format is not supported
     *         true if the format is supported
     */
    public static boolean checkFormat(Wfs3ServiceData serviceData, String collectionId, String mediaType, boolean forLinksOrDataset) {
        try {

            if (serviceData.getFeatureTypes().get(collectionId).getExtensions().containsKey(EXTENSION_KEY)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) serviceData.getFeatureTypes()
                        .get(collectionId)
                        .getExtensions()
                        .get(EXTENSION_KEY);

                ImmutableMap<Integer, List<String>> formatsList = tilesConfiguration.getTiles()
                        .stream()
                        .collect(ImmutableMap.toImmutableMap(TilesConfiguration.Tiles::getId, TilesConfiguration.Tiles::getFormats));
                List<String> formats = formatsList.values().asList().get(0);
                String s = "123";

                if (!formats.contains(mediaType)) {
                    if (forLinksOrDataset)
                        return false;
                    else
                        throw new NotAcceptableException();
                }


            }
        }catch(NullPointerException ignored){
            return true;
        }

        return true;
    }

    /**
     * checks if the zoom level is valid for the tilingScheme and the specified min and max values in the config. If no Value is specified in the config
     * the whole zoom level range of the TilingScheme is supported. If the zoom Level is not valid generate empty JSON Tiles or MVT.
     *
     * @param zoomLevel                 the zoom level of the tile, which should be checked
     * @param wfsService                the wfs3Service
     * @param wfs3OutputFormatGeoJson   the wfs3OutputFormatGeoJson
     * @param collectionId              the id of the collection of the tile
     * @param tilingSchemeId            the id of the tilingScheme of the tile
     * @param mediaType                 the media type of the tile, either application/json or application/vnd.mapbox-vector-tile
     * @param row                       the row of the tile
     * @param col                       the column of the Tile
     * @param doNotCache                boolean value if temporary tile or not
     * @param cache                     the tile cache
     * @param isCollection              boolean collection or dataset Tile
     * @param wfs3Request               the request
     * @param crsTransformation         the coordinate reference system transformation object to transform coordinates
     * @throws FileNotFoundException
     */
    public static void checkZoomLevel(int zoomLevel, Wfs3Service wfsService, Wfs3OutputFormatExtension wfs3OutputFormatGeoJson, String collectionId, String tilingSchemeId, String mediaType, String row, String col, boolean doNotCache, VectorTilesCache cache, boolean isCollection, Wfs3RequestContext wfs3Request, CrsTransformation crsTransformation) throws FileNotFoundException {
        try {

            if(wfsService.getData().getFeatureTypes().get(collectionId).getExtensions().containsKey(EXTENSION_KEY)) {
                final TilesConfiguration tilesConfiguration = (TilesConfiguration) wfsService.getData().getFeatureTypes()
                        .get(collectionId)
                        .getExtensions()
                        .get(EXTENSION_KEY);

                ImmutableMap<Integer, Map<String, TilesConfiguration.Tiles.MinMax>> minMaxList = tilesConfiguration.getTiles()
                        .stream()
                        .collect(ImmutableMap.toImmutableMap(TilesConfiguration.Tiles::getId, TilesConfiguration.Tiles::getZoomLevels));

                Map<String, TilesConfiguration.Tiles.MinMax> tilesZoomLevels = minMaxList.values().asList().get(0);

                int maxZoom = 0;
                int minZoom = 0;

                if (tilesZoomLevels.size() != 0) {
                    maxZoom = tilesZoomLevels.get(tilingSchemeId)
                            .getMax();
                    minZoom = tilesZoomLevels.get(tilingSchemeId)
                            .getMin();
                } else {
                    //if there is no member "zoomLevels" in configuration
                    if (tilingSchemeId.equals("default")) {
                        TilingScheme tilingScheme = new DefaultTilingScheme();
                        minZoom = tilingScheme.getMinLevel();
                        maxZoom = tilingScheme.getMaxLevel();
                    }
                }
                if (tilingSchemeId.equals("default")) { //TODO only default supported
                    TilingScheme tilingScheme = new DefaultTilingScheme();
                    //check if min or max zoom are valid values for the tiling scheme
                    if (minZoom > tilingScheme.getMaxLevel() || minZoom < tilingScheme.getMinLevel() || maxZoom > tilingScheme.getMaxLevel() || maxZoom < tilingScheme.getMinLevel()) {
                        throw new NotFoundException();
                    }
                }

                //if zoom Level is not in range
                if (zoomLevel < minZoom || zoomLevel > maxZoom || minZoom > maxZoom) {
                    //generate empty Feature collection
                    if (mediaType.equals("application/json")) {
                        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, Integer.toString(zoomLevel), row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);
                        File tileFileJSON = tile.getFile(cache, "json");
                        if (!tileFileJSON.exists()) {
                            TileGeneratorJson.generateEmptyJSON(tileFileJSON, new DefaultTilingScheme(), wfsService.getData(), wfs3OutputFormatGeoJson, collectionId, isCollection, wfs3Request, zoomLevel, Integer.parseInt(row), Integer.parseInt(col), crsTransformation, wfsService);
                        }
                    }
                    //generate empty MVT
                    if (mediaType.equals("application/vnd.mapbox-vector-tile")) {
                        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, Integer.toString(zoomLevel), row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);
                        File tileFileMvt = tile.getFile(cache, "pbf");

                        if (!tileFileMvt.exists()) {
                            VectorTile jsonTile = new VectorTile(collectionId, tilingSchemeId, Integer.toString(zoomLevel), row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);
                            File tileFileJSON = jsonTile.getFile(cache, "json");
                            if (!tileFileJSON.exists()) {
                                TileGeneratorJson.generateEmptyJSON(tileFileJSON, new DefaultTilingScheme(), wfsService.getData(), wfs3OutputFormatGeoJson, collectionId, isCollection, wfs3Request, zoomLevel, Integer.parseInt(row), Integer.parseInt(col), crsTransformation, wfsService);
                            }
                            TileGeneratorMvt.generateEmptyMVT(tileFileMvt, new DefaultTilingScheme());
                        }
                    }
                }
            }

        } catch (NullPointerException ignored) {
        }
    }







}

