/**
 * Copyright 2019 interactive instruments GmbH
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
import de.ii.ldproxy.target.geojson.Wfs3OutputFormatGeoJson;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.Wfs3Service;
import de.ii.ldproxy.wfs3.api.*;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static de.ii.ldproxy.wfs3.vt.TilesConfiguration.EXTENSION_KEY;
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

    @Requires
    private Wfs3ExtensionRegistry wfs3ExtensionRegistry;

    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();


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
    @Override
    public boolean isEnabledForService(Wfs3ServiceData serviceData){
        if(!isExtensionEnabled(serviceData,EXTENSION_KEY)){
            throw new NotFoundException();
        }
        return true;
    }
    /**
     * retrieve all available tiling schemes
     *
     * @return all tiling schemes in a json array
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingSchemes(@Context Service service, @Context Wfs3RequestContext wfs3Request) {

        Wfs3Service wfsService = Wfs3EndpointTiles.wfs3ServiceCheck(service);
        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(wfsService.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator= new VectorTilesLinkGenerator();
        List<Map<String, Object>> wfs3LinksList = new ArrayList<>();

        for (Object tilingSchemeId : cache.getTilingSchemeIds()
                .toArray()) {
            Map<String, Object> wfs3LinksMap = new HashMap<>();
            wfs3LinksMap.put("identifier", tilingSchemeId);
            wfs3LinksMap.put("links", vectorTilesLinkGenerator.generateTilesLinks(wfs3Request.getUriCustomizer(), tilingSchemeId.toString()));
            wfs3LinksList.add(wfs3LinksMap);
        }

        return Response.ok(ImmutableMap.of("tilingSchemes", wfs3LinksList))
                .build();
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
    public Response getTilingScheme(@Auth Optional<User> optionalUser, @PathParam("tilingSchemeId") String tilingSchemeId, @Context Service service, @Context Wfs3RequestContext wfs3Request) throws IOException {
        Wfs3Service wfsService = wfs3ServiceCheck(service);
        checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(wfsService.getData()));
        File file = cache.getTilingScheme(tilingSchemeId);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator= new VectorTilesLinkGenerator();
        List<Wfs3Link> wfs3Link = vectorTilesLinkGenerator.generateTilingSchemeLinks(wfs3Request.getUriCustomizer(), tilingSchemeId, true, false);


        /*read the json file to add links*/
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonTilingScheme;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (br.readLine() == null) {
            jsonTilingScheme = null;
        } else {
            jsonTilingScheme = mapper.readValue(file, new TypeReference<LinkedHashMap>() {
            });
        }

        jsonTilingScheme.put("links", wfs3Link);

        return Response.ok(jsonTilingScheme)
                .build();
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
    public Response xgetTileMVT(@Auth Optional<User> optionalUser, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @QueryParam("collections") String collections, @QueryParam("properties") String properties, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        // TODO support time
        // TODO support other filter parameters
        Wfs3Service wfsService = wfs3ServiceCheck(service);
        checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(wfsService.getData()));

        LOGGER.debug("GET TILE MVT {} {} {} {} {} {}", service.getId(), "all", tilingSchemeId, level, row, col);
        cache.cleanup(); // TODO centralize this

        // check and process parameters
        Set<String> requestedProperties = null;
        if (properties != null && !properties.trim()
                .isEmpty()) {
            String[] sa = properties.trim()
                    .split(",");
            requestedProperties = new HashSet<>();
            for (String s : sa) {
                requestedProperties.add(s.trim());
            }
        }

        Set<String> requestedCollections = null;
        if (collections != null && !collections.trim()
                .isEmpty()) {
            String[] sa = collections.trim()
                    .split(",");
            requestedCollections = new HashSet<>();
            for (String s : sa) {
                requestedCollections.add(s.trim());
            }
        }

        boolean doNotCache = (requestedProperties != null || requestedCollections != null);

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        doNotCache = false;
        if (queryParameters.containsKey("properties"))
            doNotCache = true;

        Wfs3OutputFormatExtension wfs3OutputFormatGeoJson = wfs3ExtensionRegistry.getOutputFormats().get(Wfs3OutputFormatGeoJson.MEDIA_TYPE);


        VectorTile tile = new VectorTile(null, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);
        // generate tile
        File tileFileMvt = tile.getFile(cache, "pbf");

        Map<String, File> layers = new HashMap<String, File>();
        Set<String> collectionIds = getCollectionIdsDataset(vectorTileMapGenerator.getAllCollectionIdsWithTileExtension(wfsService.getData()),vectorTileMapGenerator.getEnabledMap(wfsService.getData()),
                vectorTileMapGenerator.getFormatsMap(wfsService.getData()),vectorTileMapGenerator.getMinMaxMap(wfsService.getData(),true), false, false, false);
        if (!tileFileMvt.exists()) {
            generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, wfsService, level, row, col, tilingSchemeId, doNotCache, cache, wfs3Request, crsTransformation, uriInfo, false);
        } else {
            boolean invalid = false;

            for (String collectionId : collectionIds) {
                VectorTile layerTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);
                File tileFileJson = layerTile.getFile(cache, "json");
                if (tileFileJson.exists()) {
                    if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                        tileFileJson.delete();
                        layerTile.getFile(cache, "pbf")
                                .delete();
                        invalid = true;
                    }
                } else {
                    invalid = true;
                }
            }

            if (invalid) {
                generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, wfsService, level, row, col, tilingSchemeId, doNotCache, cache, wfs3Request, crsTransformation, uriInfo, true);
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
/*
    @Path("/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({Wfs3MediaTypes.GEO_JSON, MediaType.APPLICATION_JSON})
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException {

        // TODO support time
        // TODO support other filter parameters
        // TODO reduce content based on zoom level and feature counts

        // check and process parameters
        Wfs3Service wfsService = wfs3ServiceCheck(service);
        checkTilesParameterDataset(VectorTileMapGenerator.getEnabledMap(wfsService.getData()));
        Set<String> collectionIds = getCollectionIdsDataset(VectorTileMapGenerator.getAllCollectionIdsWithTileExtension(wfsService.getData()),VectorTileMapGenerator.getEnabledMap(wfsService.getData()),
                VectorTileMapGenerator.getFormatsMap(wfsService.getData()),VectorTileMapGenerator.getMinMaxMap(wfsService.getData(),true), false, false, false);

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        boolean doNotCache = false;
        if (queryParameters.containsKey("properties"))
            doNotCache = true;

        Wfs3OutputFormatExtension wfs3OutputFormatGeoJson = wfs3ExtensionRegistry.getOutputFormats().get(Wfs3OutputFormatGeoJson.MEDIA_TYPE);

        for (String collectionId : collectionIds) {
            VectorTile.checkZoomLevel(Integer.parseInt(level), VectorTileMapGenerator.getMinMaxMap(wfsService.getData(),false),wfsService, wfs3OutputFormatGeoJson, collectionId, tilingSchemeId, MediaType.APPLICATION_JSON, row, col, doNotCache, cache, false, wfs3Request, crsTransformation);
            VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, Wfs3MediaTypes.JSON, false);
        }
        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), "all", tilingSchemeId, level, row, col);


        VectorTile tile = new VectorTile(null, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

        File tileFileJson = tile.getFile(cache, "json");

        if (!tileFileJson.exists()) {
            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, null, null, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), false,tile);
        } else {
            if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, null, null, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true,tile);
            }
        }

        File finalTileFileJson = tileFileJson;
        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(finalTileFileJson), outputStream);
        };

        return Response.ok(streamingOutput, Wfs3MediaTypes.GEO_JSON)
                       .build();
    }
*/
    /**
     * checks if the tiles parameter is enabled in the dataset. If the tiles parameter is disabled in all collections, it throws a 404.
     *
     * @param enabledMap    a map with all collections and the boolean if the tiles support is enabled or not
     */
    static boolean checkTilesParameterDataset(Map<String,Boolean> enabledMap) {

        if(!Objects.isNull(enabledMap)){
            for(String collectionId: enabledMap.keySet()){
                if(enabledMap.get(collectionId))
                    return true;
            }
        }
        throw new NotFoundException();
    }

    /**
     * checks if the service is a wfs3Service
     *
     * @param service   normal service
     * @return wfs3Service
     */
    static Wfs3Service wfs3ServiceCheck(Service service) {
        if (!(service instanceof Wfs3Service)) {
            String msg = "Internal server error: vector tiles require a WFS 3 service.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }
        return (Wfs3Service) service;
    }


    /**
     * checks the whole dataset for collections which have the tiles extension and the seeding enabled
     *
     * @param allCollectionIds  set of all collectionIds with the tiles Extension
     * @param enabledMap        map of all collectionIds with tile Extension and tiles parameter enabled/disabled
     * @param formatsMap        map of all collectionIds with tile Extension and the supported formats
     * @param seedingMap        map of all collectionIds with tile Extension and the seeding levels
     * @param mvtEnabled        true if mvt format is enabled
     * @param onlyJSONenabled   true if only Json format is enabled
     * @param startSeeding           true if request came from the beginning of seeding
     * @return a set of all collection ids, which support tiles, seeding and the right format
     */
    static Set<String> getCollectionIdsDataset(Set<String>allCollectionIds,Map<String,Boolean> enabledMap,Map<String,List<String>> formatsMap,Map<String, Map<String, TilesConfiguration.MinMax>> seedingMap, boolean mvtEnabled, boolean onlyJSONenabled, boolean startSeeding) {

        Set<String> collectionIdsFilter = new HashSet<>();

        if(!Objects.isNull(allCollectionIds)) {
            for (String collectionId : allCollectionIds) {
                if (!Objects.isNull(enabledMap) && enabledMap.containsKey(collectionId) && !Objects.isNull(formatsMap)
                        && formatsMap.containsKey(collectionId) && !Objects.isNull(seedingMap) && seedingMap.containsKey(collectionId)) {
                    Boolean tilesCollectionEnabled = enabledMap.get(collectionId);
                    List<String> formatsCollection = formatsMap.get(collectionId);
                    Map<String, TilesConfiguration.MinMax> seedingCollection = seedingMap.get(collectionId);

                    if (mvtEnabled) {
                        if (tilesCollectionEnabled && seedingCollection != null && formatsCollection.contains(Wfs3MediaTypes.MVT)) {
                            collectionIdsFilter.add(collectionId);
                        }
                    } else if (onlyJSONenabled) {
                        if (tilesCollectionEnabled && formatsCollection.contains(Wfs3MediaTypes.JSON) && !formatsCollection.contains(Wfs3MediaTypes.MVT)) {
                            collectionIdsFilter.add(collectionId);
                        }
                    } else if (startSeeding) {
                        if (tilesCollectionEnabled && seedingCollection != null)
                            collectionIdsFilter.add(collectionId);
                    } else {
                        if (tilesCollectionEnabled) {
                            collectionIdsFilter.add(collectionId);
                        }
                    }
                }
            }
        }
        return collectionIdsFilter;
    }

    private void generateTileDataset(VectorTile tile, File tileFileMvt, Map<String, File> layers, Set<String> collectionIds, Set<String> requestedCollections, Set<String> requestedProperties, Wfs3Service wfsService, String level, String row, String col, String tilingSchemeId, boolean doNotCache, VectorTilesCache cache, Wfs3RequestContext wfs3Request, CrsTransformation crsTransformation, UriInfo uriInfo, boolean invalid) throws FileNotFoundException {

        Wfs3OutputFormatExtension wfs3OutputFormatGeoJson = wfs3ExtensionRegistry.getOutputFormats().get(Wfs3OutputFormatGeoJson.MEDIA_TYPE);

        for (String collectionId : collectionIds) {
            // include only the requested layers / collections

            if (requestedCollections != null && !requestedCollections.contains(collectionId))
                continue;
            if (VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, Wfs3MediaTypes.MVT, true)) {
                VectorTile.checkZoomLevel(Integer.parseInt(level), vectorTileMapGenerator.getMinMaxMap(wfsService.getData(),false), wfsService, wfs3OutputFormatGeoJson, collectionId, tilingSchemeId, Wfs3MediaTypes.MVT, row, col, doNotCache, cache, false, wfs3Request, crsTransformation);

                Map<String, File> layerCollection = new HashMap<String, File>();

                VectorTile tileCollection = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

                File tileFileMvtCollection = tileCollection.getFile(cache, "pbf");
                if (!tileFileMvtCollection.exists() || invalid) {
                    if (invalid)
                        tileFileMvtCollection.delete();

                    VectorTile layerTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

                    File tileFileJson = layerTile.getFile(cache, "json");
                    if (!tileFileJson.exists()) {
                        Wfs3MediaType geojsonMediaType;
                        geojsonMediaType = ImmutableWfs3MediaType.builder()
                                .main(new MediaType("application", "geo+json"))
                                .metadata(new MediaType("application", "json"))
                                .label("GeoJSON")
                                .build();
                        boolean success = TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, null, null, wfs3Request.getUriCustomizer(), geojsonMediaType, false, layerTile);
                        if (!success) {
                            String msg = "Internal server error: could not generate GeoJSON for a tile.";
                            LOGGER.error(msg);
                            throw new InternalServerErrorException(msg);
                        }
                    } else {
                        if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, null, null, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true,layerTile);
                        }
                    }
                    layers.put(collectionId, tileFileJson);
                    layerCollection.put(collectionId, tileFileJson);

                }
                boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvtCollection, layerCollection, requestedProperties, crsTransformation,tile);
                if (!success) {
                    String msg = "Internal server error: could not generate protocol buffers for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            }
        }
        boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformation,tile);
        if (!success) {
            String msg = "Internal server error: could not generate protocol buffers for a tile.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }

    }




}
