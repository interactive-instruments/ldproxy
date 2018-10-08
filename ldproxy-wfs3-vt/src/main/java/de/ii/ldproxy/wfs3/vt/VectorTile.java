package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.wfs3.Wfs3Service;
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
import de.ii.ldproxy.wfs3.core.Wfs3EndpointCore;
import org.threeten.extra.Interval;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static de.ii.ldproxy.target.geojson.Wfs3OutputFormatGeoJson.createJsonGenerator;
import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;


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
    private final Wfs3Service service;
    private final boolean temporary;
    private final String fileName;

    /**
     * specify the vector tile
     *
     * @param collectionId   the id of the collection in which the tile belongs
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @param level          the zoom level as a string
     * @param row            the row number as a string
     * @param col            the column number as a string
     * @param service        the wfs3 service
     * @param temporary      the info, if this is a temporary or permanent vector tile
     * @param cache          the tile cache
     */

    VectorTile(String collectionId, String tilingSchemeId, String level, String row, String col, Wfs3Service service, boolean temporary, VectorTilesCache cache) throws FileNotFoundException {

        // check and process parameters

        // check, if collectionId is valid
        if (collectionId != null) {
            Set<String> collectionIds = service.getData().getFeatureTypes().keySet();
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

        this.service = service;

        this.temporary = temporary;

        if (this.temporary) {
            fileName = UUID.randomUUID().toString();
        } else {
            fileName = String.format("%s_%s_%s", Integer.toString(this.level), Integer.toString(this.row), Integer.toString(this.col));
        }
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
            subDir = new File(new File(new File(cache.getTilesDirectory(), service.getId()), (collectionId == null ? "__all__" : collectionId)), tilingScheme.getId());

        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        return subDir;
    }

    /**
     * generate the Mapbox Vector Tile file in the cache
     *
     * @param tileFileMvt       the file object of the tile in the cache
     * @param layers            map of the layers names and the file objects of the existing GeoJSON tiles in the cache; these files must exist
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return true, if the file was generated successfully, false, if an error occurred
     */
    boolean generateTileMvt(File tileFileMvt, Map<String, File> layers, Set<String> propertyNames, CrsTransformation crsTransformation) {

        // Prepare MVT output
        TilingScheme tilingScheme = getTilingScheme();
        VectorTileEncoder encoder = new VectorTileEncoder(tilingScheme.getTileExtent());
        AffineTransformation transform;
        try {
            transform = createTransformLonLatToTile(crsTransformation);
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
            try {
                if (new BufferedReader(new FileReader(tileFileJson)).readLine() != null) {
                    jsonFeatureCollection = mapper.readValue(tileFileJson, new TypeReference<LinkedHashMap>() {
                    });
                }
            } catch (IOException e) {
                String msg = "Internal server error: exception reading the GeoJSON file of tile {}/{}/{} in dataset '{}', layer {}.";
                LOGGER.error(msg, Integer.toString(level), Integer.toString(row), Integer.toString(col), service.getId(), layerName);
                e.printStackTrace();
                return false;
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
                        if (jsonGeometry.get("type").equals("MultiLineString") && !(jsonGeometry.get("coordinates").toString().contains("],")))
                            continue; // TODO: skip MultiLineStrings with a single LineString for now because of an issue with the JTS code
                        jtsGeom = reader.read(mapper.writeValueAsString(jsonGeometry));

                    } catch (ParseException e) {
                        String msg = "Internal server error: exception parsing the GeoJSON file of tile {}/{}/{} in dataset '{}', layer {}.";
                        LOGGER.error(msg, Integer.toString(level), Integer.toString(row), Integer.toString(col), service.getId(), layerName);
                        e.printStackTrace();
                        return false;
                    } catch (JsonProcessingException e) {
                        String msg = "Internal server error: exception processing the GeoJSON file of tile {}/{}/{} in dataset '{}', layer {}.";
                        LOGGER.error(msg, Integer.toString(level), Integer.toString(row), Integer.toString(col), service.getId(), layerName);
                        e.printStackTrace();
                        return false;
                    }
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

                    // remove properties that have not been requested
                    if (propertyNames != null) {
                        jsonProperties.entrySet().removeIf(property -> !propertyNames.contains(property.getKey()));
                    }

                    // remove null values
                    jsonProperties.entrySet().removeIf(property -> property.getValue() == null);
                    // TODO: these are temporary fixes for TDS data
                    jsonProperties.entrySet().removeIf(property -> property.getValue() instanceof String && ((String) property.getValue()).toLowerCase().matches("^(no[ ]?information|\\-999999)$"));
                    jsonProperties.entrySet().removeIf(property -> property.getValue() instanceof Number && ((Number) property.getValue()).intValue() == -999999);

                    // Add the feature with the layer name, a Map with attributes and the JTS Geometry.
                    encoder.addFeature(layerName, jsonProperties, jtsGeom);
                }

                if (srfCount > srfLimit || crvCount > crvLimit || pntCount > pntLimit) {
                    LOGGER.info("Feature counts above limits for tile {}/{}/{} in dataset '{}', layer {}: {} points, {} curves, {} surfaces.", Integer.toString(level), Integer.toString(row), Integer.toString(col), service.getId(), layerName, Integer.toString(pntCount), Integer.toString(crvCount), Integer.toString(srfCount));
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
     * generate the GeoJSON tile file in the cache
     *
     * @param tileFile          the file object of the tile in the cache
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return true, if the file was generated successfully, false, if an error occurred
     */
    boolean generateTileJson(File tileFile, CrsTransformation crsTransformation, @Context UriInfo uriInfo, Map<String, String> filters, Map<String, String> filterableFields) {

        // TODO add support for multi-collection GeoJSON output
        if (collectionId == null)
            return false;

        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(tileFile);
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        String geometryField = service.getData()
                .getFilterableFieldsForFeatureType(collectionId)
                .get("bbox");

        String filter = null;
        try {
            filter = getSpatialFilter(geometryField, crsTransformation);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // calculate maxAllowableOffset
        double maxAllowableOffsetTilingScheme = tilingScheme.getMaxAllowableOffset(level, row, col);
        double maxAllowableOffsetNative = maxAllowableOffsetTilingScheme; // TODO convert to native CRS units
        double maxAllowableOffsetCrs84 = 0;
        try {
            maxAllowableOffsetCrs84 = tilingScheme.getMaxAllowableOffset(level, row, col, DEFAULT_CRS, crsTransformation);
        } catch (CrsTransformationException e) {
            LOGGER.error("CRS transformation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();

        List<String> propertiesList = Wfs3EndpointCore.getPropertiesList(queryParameters);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                .type(collectionId)
                .filter(filter)
                .maxAllowableOffset(maxAllowableOffsetNative)
                .fields(propertiesList);


        if (filters!= null &&filterableFields!=null) {
            if (!filters.isEmpty()) {
                String cql = getCQLFromFilters(service, filters, filterableFields);
                LOGGER.debug("CQL {}", cql);
                queryBuilder
                        .filter(cql + " AND " + filter)
                        .type(collectionId)
                        .maxAllowableOffset(maxAllowableOffsetNative)
                        .fields(propertiesList);
            }
        }

        queryBuilder.build();


        TransformingFeatureProvider provider = service.getFeatureProvider();
        FeatureStream<FeatureTransformer> featureTransformStream = provider.getFeatureTransformStream(queryBuilder.build());

        FeatureTransformerGeoJson featureTransformer = new FeatureTransformerGeoJson(createJsonGenerator(outputStream), true, service.getCrsTransformer(null), ImmutableList
                .of(), 0, "", maxAllowableOffsetCrs84, FeatureTransformerGeoJson.NESTED_OBJECTS.NEST, FeatureTransformerGeoJson.MULTIPLICITY.ARRAY);

        try {
            featureTransformStream.apply(featureTransformer)
                    .toCompletableFuture()
                    .join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) e.getCause();
            }
            throw new IllegalStateException("Feature stream error", e.getCause());
        }

        return true;
    }

    /**
     * generate the spatial filter for a tile in the native coordinate reference system of the dataset in CQL
     *
     * @param geometryField     the name of the geometry field to use for the filter
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the spatial filter in CQL
     * @throws CrsTransformationException if the bounding box could not be converted
     */
    private String getSpatialFilter(String geometryField, CrsTransformation crsTransformation) throws CrsTransformationException {

        // calculate bbox in the native CRS of the dataset
        BoundingBox bboxNativeCrs = getBoundingBoxNativeCrs(crsTransformation);

        return String.format(Locale.US, "BBOX(%s, %.3f, %.3f, %.3f, %.3f, '%s')", geometryField, bboxNativeCrs.getXmin(), bboxNativeCrs.getYmin(), bboxNativeCrs.getXmax(), bboxNativeCrs.getYmax(), bboxNativeCrs.getEpsgCrs().getAsSimple());
    }

    /**
     * Creates an affine transformation for converting geometries in lon/lat to tile coordinates.
     *
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the transform
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    private AffineTransformation createTransformLonLatToTile(CrsTransformation crsTransformation) throws CrsTransformationException {

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
     * @return the tiling scheme object
     */
    private TilingScheme getTilingScheme() {
        return tilingScheme;
    }

    /**
     * @return the bounding box of the tiling scheme object
     */
    private BoundingBox getBoundingBox() {
        return tilingScheme.getBoundingBox(level, row, col);
    }

    /**
     * @param crsTransformation the coordinate reference system transformation object to transform coordinates
     * @return the bounding box of the tiling scheme in the form of the native crs
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     */
    private BoundingBox getBoundingBoxNativeCrs(CrsTransformation crsTransformation) throws CrsTransformationException {
        EpsgCrs crs = service.getData().getFeatureProvider().getNativeCrs();
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



    private String getCQLFromFilters(Wfs3Service service, Map<String, String> filters, Map<String, String> filterableFields) {
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


}