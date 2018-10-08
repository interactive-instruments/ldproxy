package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.core.Wfs3EndpointCore;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.service.api.Service;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * Handle responses under '/collection/{collectionId}/tiles'.
 *
 * TODO: Make support for the path configurable. Include in the configuration for each collection: min/max zoom level, automated seeding (based on the spatial extent) for specified zoom levels
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTilesSingleCollection implements Wfs3EndpointExtension {

    @Requires
    private CrsTransformation crsTransformation;

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTilesSingleCollection.class);

    private final VectorTilesCache cache;

    Wfs3EndpointTilesSingleCollection(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
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

    /**
     * retrieve a tiling scheme used to partition the collection into tiles
     *
     * @param optionalUser the user
     * @param collectionId the id of the collection in which the tiling scheme belongs
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @return the tiling scheme in a json file
     */

    @Path("/{collectionId}/tiles/{tilingSchemeId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingScheme(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId) {

        File file = cache.getTilingScheme(tilingSchemeId);

        return Response.ok(file, MediaType.APPLICATION_JSON).build();
    }

    /**
     * Retrieve a tile of the collection. The tile in the requested tiling scheme,
     * on the requested zoom level in the tiling scheme, with the
     * requested grid coordinates (row, column) is returned. The tile has a single
     * layer with all selected features in the bounding box of the tile. The feature properties to include in the tile
     * representation can be limited using a query parameter.
     *
     * @param optionalUser the user
     * @param collectionId the id of the collection in which the tiles belong
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @param level the zoom level of the tile as a string
     * @param row the row index of the tile on the selected zoom level
     * @param col the column index of the tile on the selected zoom level
     * @param properties the properties that should be included for each feature. The parameter value is a list of property names
     * @param service the wfs3 service
     * @return a mvt file
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException an error occurred when searching for a file
     * @throws NotFoundException an error occurred when a resource is not found
     */
    @Path("/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({Wfs3MediaTypes.MVT})
    public Response getTileMVT(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @QueryParam("properties") String properties, @Context Service service, @Context UriInfo uriInfo) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        LOGGER.debug("GET TILE MVT {} {} {} {} {} {}", service.getId(), collectionId, tilingSchemeId, level, row, col);
        cache.cleanup(); // TODO centralize this

        // check and process parameters
        Set<String> requestedProperties = null;
        if (properties!=null && !properties.trim().isEmpty()) {
            String[] sa = properties.trim().split(",");
            requestedProperties = new HashSet<>();
            for (String s: sa) {
                requestedProperties.add(s.trim());
            }
        }

        boolean doNotCache = false;

        if (!(service instanceof Wfs3Service)) {
            String msg = "Internal server error: vector tiles require a WFS 3 service.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }
        Wfs3Service wfsService = (Wfs3Service) service;

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = wfsService.getData().getFilterableFieldsForFeatureType(collectionId);
        final Map<String, String> filters = Wfs3EndpointCore.getFiltersFromQuery(queryParameters, filterableFields);
        if(!filters.isEmpty()||queryParameters.containsKey("properties"))
            doNotCache=true;

        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService, doNotCache, cache);

        File tileFileMvt = tile.getFile(cache, "pbf");
        if (!tileFileMvt.exists()) {

            VectorTile jsonTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService, doNotCache, cache);
            File tileFileJson = jsonTile.getFile(cache, "json");
            if (!tileFileJson.exists()) {
                boolean success = jsonTile.generateTileJson(tileFileJson, crsTransformation,uriInfo,filters, filterableFields);
                if (!success) {
                    String msg = "Internal server error: could not generate GeoJSON for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            }

            Map<String, File> layers = new HashMap<>();
            layers.put(collectionId, tileFileJson);
            boolean success = tile.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformation);
            if (!success) {
                String msg = "Internal server error: could not generate protocol buffers for a tile.";
                LOGGER.error(msg);
                throw new InternalServerErrorException(msg);
            }
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFileMvt), outputStream);
        };

        return Response.ok(streamingOutput, Wfs3MediaTypes.MVT)
                .build();

    }

    /**
     * Retrieve a tile of the collection. The tile in the requested tiling scheme,
     * on the requested zoom level in the tiling scheme, with the
     * requested grid coordinates (row, column) is returned. The tile has a single
     * layer with all selected features in the bounding box of the tile.
     *
     * @param optionalUser the user
     * @param collectionId the id of the collection in which the tiles belong
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @param level the zoom level of the tile as a string
     * @param row the row index of the tile on the selected zoom level
     * @param col the column index of the tile on the selected zoom level
     * @param service the wfs3 service
     * @return a geoJson feature in json format
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException an error occurred when searching for a file
     */

    @Path("/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({Wfs3MediaTypes.GEO_JSON, MediaType.APPLICATION_JSON})
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service, @Context UriInfo uriInfo) throws CrsTransformationException, FileNotFoundException {


        // TODO reduce content based on zoom level and feature counts

        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), collectionId, tilingSchemeId, level, row, col);

        // check and process parameters
        if (!(service instanceof Wfs3Service)) {
            String msg = "Internal server error: vector tiles require a WFS 3 service.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }

        Wfs3Service wfsService = (Wfs3Service) service;

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = wfsService.getData().getFilterableFieldsForFeatureType(collectionId);
        final Map<String, String> filters = Wfs3EndpointCore.getFiltersFromQuery(queryParameters, filterableFields);

        boolean doNotCache=false;
        if(!filters.isEmpty()||queryParameters.containsKey("properties"))
            doNotCache=true;

        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService, doNotCache, cache);

        File tileFileJson = tile.getFile(cache, "json");

        if (!tileFileJson.exists()) {
            tile.generateTileJson(tileFileJson, crsTransformation,uriInfo, filters,filterableFields);
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFileJson), outputStream);
        };

        return Response.ok(streamingOutput, Wfs3MediaTypes.GEO_JSON)
                .build();
    }
}
