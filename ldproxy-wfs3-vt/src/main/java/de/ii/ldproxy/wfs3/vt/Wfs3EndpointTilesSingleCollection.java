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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.target.geojson.OgcApiFeaturesOutputFormatGeoJson;
import de.ii.ldproxy.wfs3.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.wfs3.core.OgcApiFeaturesEndpoint;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
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
 * <p>
 * TODO: Make support for the path configurable. Include in the configuration for each collection: min/max zoom level, automated seeding (based on the spatial extent) for specified zoom levels
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTilesSingleCollection implements OgcApiEndpointExtension {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^/?(?:\\w+)/tiles/?(?:\\w+(?:/\\w+/\\w+/\\w+)?)?$")
            .addMethods(OgcApiContext.HttpMethods.GET)
            .build();

    @Requires
    private CrsTransformation crsTransformation;

    @Requires
    private OgcApiExtensionRegistry wfs3ExtensionRegistry;

    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTilesSingleCollection.class);

    private final VectorTilesCache cache;

    Wfs3EndpointTilesSingleCollection(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        if (subPath.matches("^/?(?:\\w+)/tiles/?\\w+?$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build());
        else if (subPath.matches("^/?(?:\\w+)/tiles/\\w+/\\w+/\\w+/\\w+?$"))
            // TODO: from tile format extensions
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(new MediaType("application", "geo+json"))
                            .parameter("json")
                            .build(),
                    new ImmutableOgcApiMediaType.Builder()
                            .type(new MediaType("application", "vnd.mapbox-vector-tile"))
                            .parameter("mvt")
                            .build()
            );

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * retrieve all available tiling schemes from the collection
     *
     * @return all tiling schemes from the collection in a json array
     */
    @Path("/{collectionId}/tiles")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingSchemes(@Context OgcApiDataset service, @Context OgcApiRequestContext wfs3Request,
                                     @PathParam("collectionId") String collectionId) {

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
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
     * retrieve a tiling scheme used to partition the collection into tiles
     *
     * @param optionalUser   the user
     * @param collectionId   the id of the collection in which the tiling scheme belongs
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @return the tiling scheme in a json file
     */

    @Path("/{collectionId}/tiles/{tilingSchemeId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTilingScheme(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                                    @PathParam("tilingSchemeId") String tilingSchemeId, @Context OgcApiDataset service,
                                    @Context OgcApiRequestContext wfs3Request) {
        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);

        File file = cache.getTilingScheme(tilingSchemeId);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<OgcApiLink> ogcApiLink = vectorTilesLinkGenerator.generateTilingSchemeLinks(wfs3Request.getUriCustomizer(), tilingSchemeId, VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/vnd.mapbox-vector-tile", true), VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/json", true));


        /*read the json file to add links*/
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonTilingScheme = null;
        try {
            jsonTilingScheme = mapper.readValue(new FileReader(file), new TypeReference<LinkedHashMap>() {
            });

            jsonTilingScheme.put("links", ogcApiLink);

            return Response.ok(jsonTilingScheme)
                           .build();
        } catch (IOException e) {
            throw new NotFoundException();
        }
    }

    /**
     * Retrieve a tile of the collection. The tile in the requested tiling scheme,
     * on the requested zoom level in the tiling scheme, with the
     * requested grid coordinates (row, column) is returned. The tile has a single
     * layer with all selected features in the bounding box of the tile. The feature properties to include in the tile
     * representation can be limited using a query parameter.
     *
     * @param optionalUser   the user
     * @param collectionId   the id of the collection in which the tiles belong
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @param level          the zoom level of the tile as a string
     * @param row            the row index of the tile on the selected zoom level
     * @param col            the column index of the tile on the selected zoom level
     * @param properties     the properties that should be included for each feature. The parameter value is a list of property names
     * @param service        the wfs3 service
     * @return a mvt file
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException      an error occurred when searching for a file
     * @throws NotFoundException          an error occurred when a resource is not found
     */
    @Path("/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({"application/vnd.mapbox-vector-tile"})
    public Response getTileMVT(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                               @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level,
                               @PathParam("row") String row, @PathParam("col") String col,
                               @QueryParam("properties") String properties, @Context OgcApiDataset service,
                               @Context UriInfo uriInfo,
                               @Context OgcApiRequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE).orElseThrow(NotAcceptableException::new);

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);
        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/vnd.mapbox-vector-tile", false);

        boolean doNotCache = false;

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = service.getData()
                                                            .getFilterableFieldsForFeatureType(collectionId);
        final Map<String, String> filters = OgcApiFeaturesEndpoint.getFiltersFromQuery(OgcApiFeaturesEndpoint.toFlatMap(queryParameters), filterableFields);
        if (!filters.isEmpty() || queryParameters.containsKey("properties"))
            doNotCache = true;


        VectorTile.checkZoomLevel(Integer.parseInt(level), vectorTileMapGenerator.getMinMaxMap(service.getData(), false), service, wfs3OutputFormatGeoJson, collectionId, tilingSchemeId, "application/vnd.mapbox-vector-tile", row, col, doNotCache, cache, true, wfs3Request, crsTransformation);


        LOGGER.debug("GET TILE MVT {} {} {} {} {} {}", service.getId(), collectionId, tilingSchemeId, level, row, col);
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


        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, level, row, col, service.getData(), doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);

        File tileFileMvt = tile.getFile(cache, "pbf");
        if (!tileFileMvt.exists()) {

            VectorTile jsonTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, service.getData(), doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);
            File tileFileJson = jsonTile.getFile(cache, "json");
            if (!tileFileJson.exists()) {
                OgcApiMediaType geojsonMediaType;
                geojsonMediaType = new ImmutableOgcApiMediaType.Builder()
                        .type(new MediaType("application", "geo+json"))
                        .label("GeoJSON")
                        .build();
                boolean success = TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), geojsonMediaType, true, jsonTile);
                if (!success) {
                    String msg = "Internal server error: could not generate GeoJSON for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            } else {
                if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                    TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile);
                }
            }

            generateTileCollection(collectionId, tileFileJson, tileFileMvt, tile, requestedProperties, crsTransformation);
        } else {
            VectorTile jsonTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, service.getData(), doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);
            File tileFileJson = jsonTile.getFile(cache, "json");

            if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile);
                tileFileMvt.delete();
                generateTileCollection(collectionId, tileFileJson, tileFileMvt, tile, requestedProperties, crsTransformation);
            }
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFileMvt), outputStream);
        };

        return Response.ok(streamingOutput, "application/vnd.mapbox-vector-tile")
                       .build();

    }

    /**
     * Retrieve a tile of the collection. The tile in the requested tiling scheme,
     * on the requested zoom level in the tiling scheme, with the
     * requested grid coordinates (row, column) is returned. The tile has a single
     * layer with all selected features in the bounding box of the tile.
     *
     * @param optionalUser   the user
     * @param collectionId   the id of the collection in which the tiles belong
     * @param tilingSchemeId the local identifier of a specific tiling scheme
     * @param level          the zoom level of the tile as a string
     * @param row            the row index of the tile on the selected zoom level
     * @param col            the column index of the tile on the selected zoom level
     * @param service        the wfs3 service
     * @return a geoJson feature in json format
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException      an error occurred when searching for a file
     */

    @Path("/{collectionId}/tiles/{tilingSchemeId}/{level}/{row}/{col}")
    @GET
    @Produces({"application/geo+json", MediaType.APPLICATION_JSON})
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                                @PathParam("tilingSchemeId") String tilingSchemeId, @PathParam("level") String level,
                                @PathParam("row") String row, @PathParam("col") String col,
                                @Context OgcApiDataset service,
                                @Context UriInfo uriInfo,
                                @Context OgcApiRequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException {

        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE).orElseThrow(NotAcceptableException::new);

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);
        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/json", false);
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = service.getData()
                                                            .getFilterableFieldsForFeatureType(collectionId);
        final Map<String, String> filters = OgcApiFeaturesEndpoint.getFiltersFromQuery(OgcApiFeaturesEndpoint.toFlatMap(queryParameters), filterableFields);

        boolean doNotCache = false;
        if (!filters.isEmpty() || queryParameters.containsKey("properties"))
            doNotCache = true;

        VectorTile.checkZoomLevel(Integer.parseInt(level), vectorTileMapGenerator.getMinMaxMap(service.getData(), false), service, wfs3OutputFormatGeoJson, collectionId, tilingSchemeId, MediaType.APPLICATION_JSON, row, col, doNotCache, cache, true, wfs3Request, crsTransformation);

        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), collectionId, tilingSchemeId, level, row, col);


        VectorTile tile = new VectorTile(collectionId, tilingSchemeId, level, row, col, service.getData(), doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);

        File tileFileJson = tile.getFile(cache, "json");

        //TODO parse file (check if valid) if not valid delete it and generate new one

        if (!tileFileJson.exists()) {
            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile);
        } else {
            if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile);
            }

        }

        File finalTileFileJson = tileFileJson;
        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(finalTileFileJson), outputStream);
        };

        return Response.ok(streamingOutput, "application/geo+json")
                       .build();
    }


    /**
     * checks if the tiles parameter is enabled in the collection
     *
     * @param enabledMap   a map with all collections and the boolean if the tiles support is enabled or not
     * @param collectionId the id of the collection, which should be verified
     */
    public static boolean checkTilesParameterCollection(Map<String, Boolean> enabledMap, String collectionId) {


        if (!Objects.isNull(enabledMap) && enabledMap.containsKey(collectionId)) {
            boolean tilesEnabledCollection = enabledMap.get(collectionId);
            if (tilesEnabledCollection) {
                return true;
            }
        }
        throw new NotFoundException();

    }


    public static void generateTileCollection(String collectionId, File tileFileJson, File tileFileMvt, VectorTile tile,
                                              Set<String> requestedProperties, CrsTransformation crsTransformation) {

        Map<String, File> layers = new HashMap<>();
        layers.put(collectionId, tileFileJson);
        boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformation, tile);
        if (!success) {
            String msg = "Internal server error: could not generate protocol buffers for a tile.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }
    }

    private Optional<OgcApiFeatureFormatExtension> getOutputFormatForType(OgcApiMediaType mediaType) {
        return wfs3ExtensionRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                                    .stream()
                                    .filter(wfs3OutputFormatExtension -> wfs3OutputFormatExtension.getMediaType()
                                                                                                  .equals(mediaType))
                                    .findFirst();
    }
}
