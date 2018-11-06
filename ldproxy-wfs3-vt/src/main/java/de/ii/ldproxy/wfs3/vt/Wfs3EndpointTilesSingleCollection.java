/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.*;
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
import java.io.*;
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
     * retrieve all available tiling schemes from the collection
     *
     * @return all tiling schemes from the collection in a json array
     */
    @Path("/{collectionId}/tiles")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingSchemes(@Context Service service, @Context Wfs3RequestContext wfs3Request, @PathParam("collectionId") String collectionId) {

        Wfs3Service wfsService=Wfs3EndpointTiles.wfs3ServiceCheck(service);
        Wfs3EndpointTilesSingleCollection.checkTilesParameterCollection(wfsService,collectionId);

        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        List<Map<String,Object>> wfs3LinksList = new ArrayList<>();

        for(Object tilingSchemeId : cache.getTilingSchemeIds().toArray()){
            Map<String,Object> wfs3LinksMap = new HashMap<>();
            wfs3LinksMap.put("identifier",tilingSchemeId);
            wfs3LinksMap.put("links",wfs3LinksGenerator.generateTilesLinks(wfs3Request.getUriCustomizer(),tilingSchemeId.toString()));
            wfs3LinksList.add(wfs3LinksMap);
        }

        return Response.ok(ImmutableMap.of("tilingSchemes", wfs3LinksList)).build();
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
    public Response getTilingScheme(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @Context Service service, @Context Wfs3RequestContext wfs3Request) throws IOException {
        Wfs3Service wfsService=Wfs3EndpointTiles.wfs3ServiceCheck(service);
        checkTilesParameterCollection(wfsService,collectionId);

        File file = cache.getTilingScheme(tilingSchemeId);

        final Wfs3LinksGenerator wfs3LinksGenerator = new Wfs3LinksGenerator();
        List<Wfs3Link> wfs3Link = wfs3LinksGenerator.generateTilingSchemeLinks(wfs3Request.getUriCustomizer(),tilingSchemeId,VectorTile.checkFormats(wfsService.getData(),collectionId,Wfs3MediaTypes.MVT,true),VectorTile.checkFormats(wfsService.getData(),collectionId,Wfs3MediaTypes.JSON,true));


        /*read the json file to add links*/
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonTilingScheme;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (br.readLine() == null) {
            jsonTilingScheme = null;
        } else {
            jsonTilingScheme =  mapper.readValue(file, new TypeReference<LinkedHashMap>() {});
        }

        jsonTilingScheme.put("links",wfs3Link);

        return Response.ok(jsonTilingScheme).build();


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
    public Response getTileMVT(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @QueryParam("properties") String properties, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        Wfs3Service wfsService = Wfs3EndpointTiles.wfs3ServiceCheck(service);
        checkTilesParameterCollection(wfsService, collectionId);
        VectorTile.checkFormats(wfsService.getData(), collectionId, Wfs3MediaTypes.MVT,false);


        boolean doNotCache = false;

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = wfsService.getData().getFilterableFieldsForFeatureType(collectionId);
        final Map<String, String> filters = Wfs3EndpointCore.getFiltersFromQuery(queryParameters, filterableFields);
        if (!filters.isEmpty() || queryParameters.containsKey("properties"))
            doNotCache = true;


        VectorTile.checkZoomLevels(Integer.parseInt(level),wfsService,collectionId, tilingSchemeId,Wfs3MediaTypes.MVT,row,col,doNotCache,cache,true, wfs3Request,crsTransformation);


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



        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache,wfsService.getFeatureProvider());

        File tileFileMvt = tile.getFile(cache, "pbf");
        if (!tileFileMvt.exists()) {

            VectorTile jsonTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache,wfsService.getFeatureProvider());
            File tileFileJson = jsonTile.getFile(cache, "json");
            if (!tileFileJson.exists()) {
                Wfs3MediaType geojsonMediaType;
                geojsonMediaType = ImmutableWfs3MediaType.builder()
                                                             .main(new MediaType("application", "geo+json"))
                                                             .metadata(new MediaType("application", "json"))
                                                             .label("GeoJSON")
                                                             .build();
                boolean success = jsonTile.generateTileJson(tileFileJson, crsTransformation,uriInfo,filters, filterableFields, wfs3Request.getUriCustomizer(), geojsonMediaType,true);
                if (!success) {
                    String msg = "Internal server error: could not generate GeoJSON for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            }else{
                if(VectorTile.deleteJSON(tileFileJson,tile,crsTransformation,uriInfo,filters,filterableFields,wfs3Request.getUriCustomizer(),wfs3Request.getMediaType(),cache)){
                    tile.generateTileJson(tileFileJson, crsTransformation,uriInfo, filters,filterableFields,wfs3Request.getUriCustomizer(),wfs3Request.getMediaType(),true);
                }
            }

            generateTileCollection(collectionId,tileFileJson,tileFileMvt,tile,requestedProperties,crsTransformation);
        }else{
            VectorTile jsonTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache,wfsService.getFeatureProvider());
            File tileFileJson = jsonTile.getFile(cache, "json");

            if(VectorTile.deleteJSON(tileFileJson, tile, crsTransformation, uriInfo, null, null, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(),cache)){
                tile.generateTileJson(tileFileJson, crsTransformation,uriInfo, filters,filterableFields,wfs3Request.getUriCustomizer(),wfs3Request.getMediaType(),true);
                tileFileMvt.delete();
                generateTileCollection(collectionId,tileFileJson,tileFileMvt,tile,requestedProperties,crsTransformation);
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
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException {

        Wfs3Service wfsService=Wfs3EndpointTiles.wfs3ServiceCheck(service);
        checkTilesParameterCollection(wfsService,collectionId);
        VectorTile.checkFormats(wfsService.getData(),collectionId, Wfs3MediaTypes.JSON,false);
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = wfsService.getData().getFilterableFieldsForFeatureType(collectionId);
        final Map<String, String> filters = Wfs3EndpointCore.getFiltersFromQuery(queryParameters, filterableFields);

        boolean doNotCache=false;
        if(!filters.isEmpty()||queryParameters.containsKey("properties"))
            doNotCache=true;

        VectorTile.checkZoomLevels(Integer.parseInt(level),wfsService,collectionId, tilingSchemeId,MediaType.APPLICATION_JSON,row,col,doNotCache,cache, true,wfs3Request,crsTransformation);

        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), collectionId, tilingSchemeId, level, row, col);





        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache,wfsService.getFeatureProvider());

        File tileFileJson = tile.getFile(cache, "json");

        //TODO parse file (check if valid) if not valid delete it and generate new one

        if (!tileFileJson.exists()) {
            tile.generateTileJson(tileFileJson, crsTransformation,uriInfo, filters,filterableFields,wfs3Request.getUriCustomizer(),wfs3Request.getMediaType(),true);
        } else{
           if(VectorTile.deleteJSON(tileFileJson,tile,crsTransformation,uriInfo,filters,filterableFields,wfs3Request.getUriCustomizer(),wfs3Request.getMediaType(),cache)){
               tile.generateTileJson(tileFileJson, crsTransformation,uriInfo, filters,filterableFields,wfs3Request.getUriCustomizer(),wfs3Request.getMediaType(),true);
           }

        }

        File finalTileFileJson = tileFileJson;
        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(finalTileFileJson), outputStream);
        };

        return Response.ok(streamingOutput, Wfs3MediaTypes.GEO_JSON)
                .build();
    }

    public static void checkTilesParameterCollection(Wfs3Service wfsService, String collectionId){
        FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3=wfsService.getData().getFeatureTypes().get(collectionId);

        try{
            featureTypeConfigurationWfs3.getTiles();
        }catch (NullPointerException e){
            throw new NotFoundException();
        }

        if(!featureTypeConfigurationWfs3.getTiles().getEnabled()){
            throw new NotFoundException();
        }
    }

    public static void generateTileCollection(String collectionId, File tileFileJson, File tileFileMvt, VectorTile tile, Set<String> requestedProperties, CrsTransformation crsTransformation){

        Map<String, File> layers = new HashMap<>();
        layers.put(collectionId, tileFileJson);
        boolean success = tile.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformation);
        if (!success) {
            String msg = "Internal server error: could not generate protocol buffers for a tile.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }
    }
}
