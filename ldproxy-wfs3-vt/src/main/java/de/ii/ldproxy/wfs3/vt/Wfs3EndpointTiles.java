package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import no.ecc.vectortile.VectorTileEncoder;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.attribute.standard.Media;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletionException;

import static de.ii.ldproxy.target.geojson.Wfs3OutputFormatGeoJson.createJsonGenerator;
import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;
import static de.ii.ldproxy.wfs3.vt.Wfs3EndpointTilingSchemes.TILING_SCHEMES_DIR_NAME;
import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * Handle responses under '/collection/{collectionId}/tiles'.
 *
 * TODO: this is in an early development state.
 *
 * TODO: Add /tiles endpoint in addition to /collections/{collectionId}/tiles.
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTiles implements Wfs3EndpointExtension {

    @Requires
    private CrsTransformation crsTransformation;

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);

    private static final String TILES_DIR_NAME = "tiles";

    private final JSONParser parser = new JSONParser();
    private final GeoJsonReader reader = new GeoJsonReader();


    // in dev env this would be build/data/tiles
    private final File tilesDirectory;

    // in dev env this would be build/data/tilingSchemes
    private final File tilingSchemesDirectory;

    Wfs3EndpointTiles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        this.tilesDirectory = new File(new File(bundleContext.getProperty(DATA_DIR_KEY)), TILES_DIR_NAME);
        this.tilingSchemesDirectory = new File(new File(bundleContext.getProperty(DATA_DIR_KEY)), TILING_SCHEMES_DIR_NAME);

        if (!tilesDirectory.exists()) {
            tilesDirectory.mkdirs();
        }
        if (!tilingSchemesDirectory.exists()) {
            tilingSchemesDirectory.mkdirs();
        }
    }

    @Override
    public String getPath() {
        return "collections";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/(?:\\w+)\\/tiles\\/?.*$";
    }

    @Override
    public List<String> getMethods() {
        return ImmutableList.of("GET");
    }

    @Override
    public boolean matches(String firstPathSegment, String method, String subPath) {
        return Wfs3EndpointExtension.super.matches(firstPathSegment, method, subPath);
    }

    /* TODO add conformance class
    @Override
    public String getConformanceClass() {
        return "http://def.ldproxy.net/wfs/3.0/ext/vt";
    }
    */

    @Path("/{collectionId}/tiles/{tilingSchemeId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingScheme(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId) {

        //TODO read from tilingSchemesDirectory or 301, wait for specs

        return Response.ok(ImmutableMap.of())
                .build();
    }

    @Path("/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({Wfs3MediaTypes.MVT})
    public Response getTileMVT(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        // TODO support f
        // TODO support time
        // TODO support other filter parameters

        // check and process parameters

        // we just pass the collection to the feature query, nothing to do for collectionId

        // get the TilingScheme


        //check if collectionId is valid
        Wfs3Service wfsService = (Wfs3Service) service;
        Set<String> featureTypeNames= wfsService.getData().getFeatureTypes().keySet();
        if(collectionId.isEmpty() || !featureTypeNames.contains(collectionId)){
            throw new NotFoundException();
        }


        TilingScheme tilingScheme = new DefaultTilingScheme(); // TODO, currently just support "default"

        int l = checkZoomLevel(tilingScheme, level);
        if (l == -1)
            throw new FileNotFoundException();

        int r = checkRow(tilingScheme, l, row);
        if (r == -1)
            throw new FileNotFoundException();

        int c = checkColumn(tilingScheme, l, col);
        if (c == -1)
            throw new FileNotFoundException();

        LOGGER.debug("GET TILE MVT {} {} {} {}", tilingScheme.getId(), level, row, col);

        File tileFilePbf = getTileFile(collectionId, tilingScheme, l, r, c, "pbf");
        if (!tileFilePbf.exists()) {

            File tileFileJson = getTileFile(collectionId, tilingScheme, l, r, c, "json");
            if (!tileFileJson.exists()) {
                generateTileJson(tileFileJson, collectionId, tilingScheme, l, r, c, service);
            }

            try {
                // TODO protect against GeoJSON syntax errors

                /*JacksonParser*/
                ObjectMapper mapper = new ObjectMapper();


                Map<String,Object> jsonFeatureCollection;
                //If file is empty, don't read from file
                BufferedReader br = new BufferedReader(new FileReader(tileFileJson));

                if (br.readLine() == null) {
                    jsonFeatureCollection = null;
                } else {
                    jsonFeatureCollection =  mapper.readValue(tileFileJson, new TypeReference<LinkedHashMap>() {});
                }


                // Prepare MVT output
                VectorTileEncoder encoder = new VectorTileEncoder(tilingScheme.getTileExtent());
                BoundingBox bboxTilingSchemeCrs = tilingScheme.getBoundingBox(l, r, c);
                CrsTransformer transformer = crsTransformation.getTransformer(tilingScheme.getCrs(), DEFAULT_CRS);
                BoundingBox bboxCrs84 = transformer.transformBoundingBox(bboxTilingSchemeCrs);
                AffineTransformation transform = createTransformLonLatToTile(tilingScheme, bboxCrs84);

                //empty Collection or no features in the collection
                if (jsonFeatureCollection != null) {

                    Geometry jtsGeom;
                    List<Object> jsonFeatures = (List<Object>) jsonFeatureCollection.get("features");

                    for (Object object : jsonFeatures) {
                        Map<String,Object> jsonFeature = (Map<String,Object>) object;
                        Map<String,Object> jsonGeometry = (Map<String,Object>) jsonFeature.get("geometry");

                        // read JTS geometry in WGS 84 lon/lat
                        jtsGeom = reader.read(mapper.writeValueAsString(jsonGeometry));
                        jtsGeom.apply(transform);

                        // TODO select properties
                        Map<String,Object> jsonProperties = (Map<String,Object>) jsonFeature.get("properties");

                        // Add the feature with the layer name, a Map with attributes and the JTS Geometry.
                        encoder.addFeature(collectionId, jsonProperties, jtsGeom);
                    }
                }

                // Finally, get the byte array and write it to the cache
                byte[] encoded = encoder.encode();
                Files.write(encoded, tileFilePbf);
            } catch (Exception e) {
                e.printStackTrace();
                // TODO handle exceptions properly
                return Response.ok(null)
                        .build();
            }
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFilePbf), outputStream);
        };

        return Response.ok(streamingOutput, Wfs3MediaTypes.MVT)
                .build();

    }

    @Path("/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({Wfs3MediaTypes.GEO_JSON, MediaType.APPLICATION_JSON})
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service) throws CrsTransformationException, FileNotFoundException {

        // TODO support f
        // TODO support time
        // TODO support other filter parameters
        // TODO reduce content based on zoom level and feature counts

        // check and process parameters

        // we just pass the collection to the feature query, nothing to do for collectionId

        // get the TilingScheme
        TilingScheme tilingScheme = new DefaultTilingScheme(); // TODO, currently just support "default"

        int l = checkZoomLevel(tilingScheme, level);
        if (l == -1)
            throw new FileNotFoundException();

        int r = checkRow(tilingScheme, l, row);
        if (r == -1)
            throw new FileNotFoundException();

        int c = checkColumn(tilingScheme, l, col);
        if (c == -1)
            throw new FileNotFoundException();

        LOGGER.debug("GET TILE GeoJSON {} {} {} {}", tilingScheme.getId(), level, row, col);

        File tileFileJson = getTileFile(collectionId, tilingScheme, l, r, c, "json");

        if (!tileFileJson.exists()) {
            generateTileJson(tileFileJson, collectionId, tilingScheme, l, r, c, service);
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFileJson), outputStream);
        };

        return Response.ok(streamingOutput, Wfs3MediaTypes.GEO_JSON)
                .build();
    }

    /**
     * Verify that the zoom level is an integer value in the valid range for the tiling scheme
     *
     * @param tilingScheme the tiling scheme used in the request
     * @param level the zoom level as a string
     * @return the zoom level as an integer, or -1 in case of an invalid zoom level
     */
    private int checkZoomLevel( TilingScheme tilingScheme, String level ) {
        int l;
        try {
            l = Integer.parseInt(level);
            if (l > tilingScheme.getMaxLevel() || l < tilingScheme.getMinLevel()) {
                l = -1;
            }
        } catch (NumberFormatException e) {
            l = -1;
        };
        return l;
    }

    /**
     * Verify that the row number is an integer value
     *
     * @param tilingScheme the tiling scheme used in the request
     * @param level the zoom level
     * @param row the row number as a string
     * @return the row number as an integer, or -1 in case of an invalid value
     */
    private int checkRow( TilingScheme tilingScheme, int level, String row ) {
        int r;
        try {
            r = Integer.parseInt(row);
        } catch (NumberFormatException e) {
            r = -1;
        };
        return r;
    }

    /**
     * Verify that the column number is an integer value
     *
     * @param tilingScheme the tiling scheme used in the request
     * @param level the zoom level
     * @param col the column number as a string
     * @return the column number as an integer, or -1 in case of an invalid value
     */
    private int checkColumn( TilingScheme tilingScheme, int level, String col ) {
        int c;
        try {
            c = Integer.parseInt(col);
        } catch (NumberFormatException e) {
            c = -1;
        };
        return c;
    }

    /**
     * Creates an affine transformation for converting geometries in lon/lat to tile coordinates.
     *
     * @param bbox the bounding box in CRS84
     * @return the transform
     */
    private AffineTransformation createTransformLonLatToTile( TilingScheme tilingScheme, BoundingBox bbox )
    {
        double lonMin = bbox.getXmin();
        double lonMax = bbox.getXmax();
        double latMin = bbox.getYmin();
        double latMax = bbox.getYmax();

        double tileSize = tilingScheme.getTileSize();
        double xScale = tileSize / (lonMax-lonMin);
        double yScale = tileSize / (latMax-latMin);

        double xOffset = - lonMin * xScale;
        double yOffset = latMin * yScale + tileSize;

        return new AffineTransformation( xScale, 0.0d, xOffset, 0.0d, -yScale, yOffset );
    }

    /**
     * retrieve the subdirectory in the tiles directory for the selected tiling scheme
     * @param tilingScheme the tiling scheme object
     * @return the directory with the cached tile files
     */
    private File getTileSubDirectory(String collectionId, TilingScheme tilingScheme) {
        File subDir = new File(new File(tilesDirectory, collectionId), tilingScheme.getId());
        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        return subDir;
    }

    private File getTileFile(String collectionId, TilingScheme tilingScheme, int level, int row, int col, String extension) {
        return new File(getTileSubDirectory(collectionId, tilingScheme), String.format("%s_%s_%s.%s", Integer.toString(level), Integer.toString(row), Integer.toString(col), extension));
    }

    private String getFilterForTile(String geometryField, TilingScheme tilingScheme, int level, int row, int col, Wfs3Service service) throws CrsTransformationException {

        // calculate bbox in the CRS of the tiling Scheme
        BoundingBox bboxTilingSchemeCrs = tilingScheme.getBoundingBox(level, row, col);

        LOGGER.debug("TILE {}", String.format(Locale.US, "BBOX(%.3f, %.3f, %.3f, %.3f, '%s')", bboxTilingSchemeCrs.getXmin(), bboxTilingSchemeCrs.getYmin(), bboxTilingSchemeCrs.getXmax(), bboxTilingSchemeCrs.getYmax(), bboxTilingSchemeCrs.getEpsgCrs().getAsSimple()));

        // transform to native crs via the default crs
        CrsTransformer transformer = crsTransformation.getTransformer(tilingScheme.getCrs(), service.getData().getFeatureProvider().getNativeCrs());
        BoundingBox bboxNativeCrs = transformer.transformBoundingBox(bboxTilingSchemeCrs);
        //BoundingBox bboxNativeCrs = service.transformBoundingBox(bboxCrs84);

        return String.format(Locale.US, "BBOX(%s, %.3f, %.3f, %.3f, %.3f, '%s')", geometryField, bboxNativeCrs.getXmin(), bboxNativeCrs.getYmin(), bboxNativeCrs.getXmax(), bboxNativeCrs.getYmax(), bboxNativeCrs.getEpsgCrs().getAsSimple());
    }

    private void generateTileJson(File tileFile, String collectionId, TilingScheme tilingScheme, int level, int row, int col, Service service) throws FileNotFoundException, CrsTransformationException {
        Wfs3Service wfs3Service = (Wfs3Service) service;

        OutputStream outputStream = new FileOutputStream(tileFile);

        String geometryField = wfs3Service.getData()
                .getFilterableFieldsForFeatureType(collectionId)
                .get("bbox");

        String filter = getFilterForTile(geometryField, tilingScheme, level, row, col, wfs3Service);

        // calculate maxAllowableOffset
        double maxAllowableOffsetTilingScheme = tilingScheme.getMaxAllowableOffset(level, row, col);
        double maxAllowableOffsetNative = maxAllowableOffsetTilingScheme; // TODO convert to native CRS units
        double maxAllowableOffsetCrs84 = tilingScheme.getMaxAllowableOffset(level, row, col, DEFAULT_CRS, crsTransformation);

        ImmutableFeatureQuery query = ImmutableFeatureQuery.builder()
                .type(collectionId)
                .filter(filter)
                .maxAllowableOffset(maxAllowableOffsetNative)
                .build();

        TransformingFeatureProvider provider = wfs3Service.getFeatureProvider();
        FeatureStream<FeatureTransformer> featureTransformStream = provider.getFeatureTransformStream(query);

        FeatureTransformerGeoJson featureTransformer = new FeatureTransformerGeoJson(createJsonGenerator(outputStream), true, wfs3Service.getCrsTransformer(null), ImmutableList.of(), 0, "", maxAllowableOffsetCrs84, FeatureTransformerGeoJson.NESTED_OBJECTS.NEST, FeatureTransformerGeoJson.MULTIPLICITY.ARRAY);

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
    }


}
