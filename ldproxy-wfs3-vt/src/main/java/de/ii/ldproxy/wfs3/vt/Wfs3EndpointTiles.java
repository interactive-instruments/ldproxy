package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.Wfs3EndpointExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ExtensionRegistry;
import de.ii.ldproxy.wfs3.api.Wfs3RequestContext;
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
 * Handle responses under '/tiles'.
 *
 * TODO: Make support for the path configurable. Include in the configuration: min/max zoom level, automated seeding (based on the spatial extent) for specified zoom levels
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

    private final VectorTilesCache cache;

    Wfs3EndpointTiles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }

    @Override
    public String getPath() {
        return "tiles";
    }

    @Override
    public String getSubPathRegex() {
        return "^\\/?.*$";
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
     * retrieve a tiling scheme to partition the dataset into tiles
     *
     * @param optionalUser the user
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @return the tiling scheme in a json file
     */
    @Path("/{tilingSchemeId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingScheme(@Auth Optional<User> optionalUser, @PathParam("tilingSchemeId") String tilingSchemeId) {

        File file = cache.getTilingScheme(tilingSchemeId);

        return Response.ok(file, MediaType.APPLICATION_JSON).build();
    }

    /**
     * The tile in the requested tiling scheme, on the requested zoom level in the tiling scheme, with the requested grid coordinates (row, column) is returned.
     * Each collection of the dataset is returned as a separate layer.
     * The collections and the feature properties to include in the tile representation can be limited using query parameters.
     *
     * @param optionalUser the user
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @param level the zoom level of the tile as a string
     * @param row the row index of the tile on the selected zoom level
     * @param col the column index of the tile on the selected zoom level
     * @param collections the collections that should be included in the tile. The parameter value is a list of collection identifiers.
     * @param properties the properties that should be included for each feature. The parameter value is a list of property names.
     * @param service the wfs3 service
     * @return all mvt's that match the criteria
     * @throws CrsTransformationException
     * @throws FileNotFoundException
     * @throws NotFoundException
     */
    @Path("/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({Wfs3MediaTypes.MVT})
    public Response getTileMVT(@Auth Optional<User> optionalUser, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @QueryParam("collections") String collections, @QueryParam("properties") String properties, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        // TODO support time
        // TODO support other filter parameters

        LOGGER.debug("GET TILE MVT {} {} {} {} {} {}", service.getId(), "all", tilingSchemeId, level, row, col);
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

        Set<String> requestedCollections = null;
        if (collections!=null && !collections.trim().isEmpty()) {
            String[] sa = collections.trim().split(",");
            requestedCollections = new HashSet<>();
            for (String s: sa) {
                requestedCollections.add(s.trim());
            }
        }

        boolean doNotCache = (requestedProperties!=null || requestedCollections!=null);

        if (!(service instanceof Wfs3Service)) {
            String msg = "Internal server error: vector tiles require a WFS 3 service.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }

        Wfs3Service wfsService = (Wfs3Service) service;

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        doNotCache=false;
        if(queryParameters.containsKey("properties"))
            doNotCache=true;

        VectorTile tile = new VectorTile(null, tilingSchemeId, level, row, col, wfsService, doNotCache, cache);

        // generate tile
        File tileFileMvt = tile.getFile(cache, "pbf");
        if (!tileFileMvt.exists()) {

            Map<String, File> layers = new HashMap<String, File>();
            Set<String> collectionIds = wfsService.getData().getFeatureTypes().keySet();

            for (String collectionId : collectionIds) {
                // include only the requested layers / collections
                if (requestedCollections!=null && !requestedCollections.contains(collectionId))
                    continue;

                VectorTile layerTile = new VectorTile( collectionId, tilingSchemeId, level, row, col, wfsService, doNotCache, cache);

                File tileFileJson = layerTile.getFile(cache, "json");
                if (!tileFileJson.exists()) {
                    boolean success = layerTile.generateTileJson(tileFileJson, crsTransformation,uriInfo,null,null,wfs3Request,false);
                    if (!success) {
                        String msg = "Internal server error: could not generate GeoJSON for a tile.";
                        LOGGER.error(msg);
                        throw new InternalServerErrorException(msg);
                    }
                }
                layers.put(collectionId, tileFileJson);
            }

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
     * The tile in the requested tiling scheme, on the requested zoom level in the tiling scheme, with the requested grid coordinates (row, column)
     * is returned. Each collection of the dataset is returned as a separate layer.
     *
     * @param optionalUser the user
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @param level the zoom level of the tile as a string
     * @param row the row index of the tile on the selected zoom level
     * @param col the column index of the tile on the selected zoom level
     * @param service the wfs3 service
     * @return all geoJson features in json format
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException an error occurred when searching for a file
     */

    @Path("/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({Wfs3MediaTypes.GEO_JSON, MediaType.APPLICATION_JSON})
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service, @Context UriInfo uriInfo,@Context Wfs3RequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException {

        // TODO support time
        // TODO support other filter parameters
        // TODO reduce content based on zoom level and feature counts

        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), "all", tilingSchemeId, level, row, col);

        // check and process parameters
        Wfs3Service wfsService = (Wfs3Service) service; // TODO: protect cast, if this is not a WFS 3 service

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        boolean doNotCache=false;
        if(queryParameters.containsKey("properties"))
            doNotCache=true;

        VectorTile tile = new VectorTile(null, tilingSchemeId, level, row, col, wfsService, doNotCache, cache);

        File tileFileJson = tile.getFile(cache, "json");

        if (!tileFileJson.exists()) {
            tile.generateTileJson(tileFileJson, crsTransformation,uriInfo,null,null,wfs3Request,false);
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFileJson), outputStream);
        };

        return Response.ok(streamingOutput, Wfs3MediaTypes.GEO_JSON)
                .build();
    }

}
