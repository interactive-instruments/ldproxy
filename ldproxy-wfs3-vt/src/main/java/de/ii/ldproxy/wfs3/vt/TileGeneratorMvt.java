/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * This class is responsible for generation and deletion of Mapbox Vector Tiles.
 */
public class TileGeneratorMvt {

    private static final Logger LOGGER = LoggerFactory.getLogger(TileGeneratorMvt.class);

    /**
     * generate the Mapbox Vector Tile file in the cache
     *
     * @param tileFileMvt       the file object of the tile in the cache
     * @param layers            map of the layers names and the file objects of the existing GeoJSON tiles in the cache; these files must exist
     * @param propertyNames     a list of all property Names
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @param tile              the tile which should be generated
     * @param seeding           flag to indicate that the tile creation is part of a seeding process
     * @return true, if the file was generated successfully, false, if an error occurred
     */
    static boolean generateTileMvt(File tileFileMvt, Map<String, File> layers, Set<String> propertyNames,
                                   CrsTransformation crsTransformation, VectorTile tile, boolean seeding) {

        // Prepare MVT output
        TileMatrixSet tileMatrixSet = tile.getTileMatrixSet();
        OgcApiDatasetData serviceData = tile.getApiData();
        int level = tile.getLevel();
        int row = tile.getRow();
        int col = tile.getCol();
        //checkZoomLevels(level, service,collectionId,tileMatrixSet.getId());

        VectorTileEncoder encoder = new VectorTileEncoder(tileMatrixSet.getTileExtent());
        AffineTransformation transform;
        try {
            transform = tile.createTransformLonLatToTile(crsTransformation);
        } catch (CrsTransformationException e) {
            String msg = "Internal server error: error converting coordinates.";
            LOGGER.error(msg);
            e.printStackTrace();
            return false;
        }

        GeoJsonReader reader = new GeoJsonReader();

        // TODO: these are just arbitrary numbers...
        int srfLimit = 10000;
        int crvLimit = 10000;
        int pntLimit = 10000;

        for (Map.Entry entry : layers.entrySet()) {
            String layerName = (String) entry.getKey();
            File tileFileJson = (File) entry.getValue();

            // Jackson parser
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> jsonFeatureCollection = null;
            if (tileFileJson != null) {
                int count = 0;
                while (true) {
                    try {
                        if (new BufferedReader(new FileReader(tileFileJson)).readLine() != null) {
                            jsonFeatureCollection = mapper.readValue(tileFileJson, new TypeReference<LinkedHashMap>() {
                            });
                        }
                        break;
                    } catch (IOException e) {
                        if (seeding && count++ < 5) {
                            // maybe the file is still generated, try to wait twice before giving up
                            String msg = "Failure to read the GeoJSON file of tile {}/{}/{} in dataset '{}', layer '{}'. Trying again ...";
                            LOGGER.info(msg, Integer.toString(level), Integer.toString(row), Integer.toString(col), serviceData.getId(), layerName);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                                // ignore and just continue
                            }
                        } else {
                            String msg = "Internal server error: exception reading the GeoJSON file of tile {}/{}/{} in dataset '{}', layer '{}'.";
                            LOGGER.error(msg, Integer.toString(level), Integer.toString(row), Integer.toString(col), serviceData.getId(), layerName);
                            e.printStackTrace();
                            return false;
                        }
                    }
                }
            }

            //empty Collection or no features in the collection
            if (jsonFeatureCollection != null) {

                Geometry jtsGeom = null;
                List<Object> jsonFeatures = (List<Object>) jsonFeatureCollection.get("features");

                int pntCount = 0;
                int crvCount = 0;
                int srfCount = 0;

                for (Object object : jsonFeatures) {
                    Map<String, Object> jsonFeature = (Map<String, Object>) object;
                    Map<String, Object> jsonGeometry = (Map<String, Object>) jsonFeature.get("geometry");

                    // read JTS geometry in WGS 84 lon/lat
                    try {
                        if (jsonGeometry.get("type")
                                        .equals("MultiLineString") && !(jsonGeometry.get("coordinates")
                                                                                    .toString()
                                                                                    .contains("],")))
                            continue; // TODO: skip MultiLineStrings with a single LineString for now because of an issue with the JTS code
                        jtsGeom = reader.read(mapper.writeValueAsString(jsonGeometry));

                    } catch (ParseException e) {
                        String msg = "Internal server error: exception parsing the GeoJSON file of tile {}/{}/{} in dataset '{}', layer {}.";
                        LOGGER.error(msg, Integer.toString(level), Integer.toString(row), Integer.toString(col), serviceData.getId(), layerName);
                        e.printStackTrace();
                        return false;
                    } catch (JsonProcessingException e) {
                        String msg = "Internal server error: exception processing the GeoJSON file of tile {}/{}/{} in dataset '{}', layer {}.";
                        LOGGER.error(msg, Integer.toString(level), Integer.toString(row), Integer.toString(col), serviceData.getId(), layerName);
                        e.printStackTrace();
                        return false;
                    }

                    if (jsonGeometry.get("type")
                                    .equals("Polygon") || jsonGeometry.get("type")
                                                                      .equals("MultiPolygon"))
                        jtsGeom = jtsGeom.reverse();
                    jtsGeom.apply(transform);
                    // filter features
                    // TODO: this should be more sophisticated...
                    String geomType = jtsGeom.getGeometryType();
                    if (geomType.contains("Polygon")) {
                        double area = jtsGeom.getArea();
                        if (area <= 4)
                            continue;
                        if (srfCount++ > srfLimit)
                            continue;
                    } else if (geomType.contains("LineString")) {
                        double length = jtsGeom.getLength();
                        if (length <= 2)
                            continue;
                        if (crvCount++ > crvLimit)
                            continue;
                    } else if (geomType.contains("Point")) {
                        if (pntCount++ > pntLimit)
                            continue;
                    }

                    Map<String, Object> jsonProperties = (Map<String, Object>) jsonFeature.get("properties");

                    if (jsonProperties == null) {
                        jsonProperties = new HashMap<>();
                    } else {
                        // remove properties that have not been requested
                        if (propertyNames != null) {
                            jsonProperties.entrySet()
                                          .removeIf(property -> !propertyNames.contains(property.getKey()));
                        }

                        // remove null values
                        jsonProperties.entrySet()
                                      .removeIf(property -> property.getValue() == null);
                        // TODO: these are temporary fixes for TDS data
                        jsonProperties.entrySet()
                                      .removeIf(property -> property.getValue() instanceof String && ((String) property.getValue()).toLowerCase()
                                                                                                                                   .matches("^(no[ ]?information|\\-999999)$"));
                        jsonProperties.entrySet()
                                      .removeIf(property -> property.getValue() instanceof Number && ((Number) property.getValue()).intValue() == -999999);
                    }

                    // If we have an id that happens to be a long value, use it
                    Object ids = jsonFeature.get("id");
                    Long id = null;
                    if (ids != null && ids instanceof String) {
                        try {
                            id = Long.parseLong((String) ids);
                        } catch (Exception e) {
                            // nothing to do
                        }
                    }

                    // Add the feature with the layer name, a Map with attributes and the JTS Geometry.
                    if (id != null)
                        encoder.addFeature(layerName, jsonProperties, jtsGeom, id);
                    else
                        encoder.addFeature(layerName, jsonProperties, jtsGeom);
                }

                if (srfCount > srfLimit || crvCount > crvLimit || pntCount > pntLimit) {
                    LOGGER.info("Feature counts above limits for tile {}/{}/{} in dataset '{}', layer {}: {} points, {} curves, {} surfaces.", Integer.toString(level), Integer.toString(row), Integer.toString(col), serviceData.getId(), layerName, Integer.toString(pntCount), Integer.toString(crvCount), Integer.toString(srfCount));
                }
            }
        }

        // Finally, get the byte array and write it to the cache
        byte[] encoded = encoder.encode();
        try {
            Files.write(encoded, tileFileMvt);
        } catch (IOException e) {
            String msg = "Internal server error: exception writing the protocol buffer file of a tile.";
            LOGGER.error(msg);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * generates an empty MVT.
     *
     * @param tileFileMvt  the file object of the tile in the cache
     * @param tileMatrixSet the tile matrix set the MVT should have
     */
    public static void generateEmptyMVT(File tileFileMvt, TileMatrixSet tileMatrixSet) {
        VectorTileEncoder encoder = new VectorTileEncoder(tileMatrixSet.getTileExtent());
        byte[] encoded = encoder.encode();
        try {
            Files.write(encoded, tileFileMvt);
        } catch (IOException e) {
            String msg = "Internal server error: exception writing the protocol buffer file of a tile.";
            LOGGER.error(msg);
            e.printStackTrace();
        }
    }


}
