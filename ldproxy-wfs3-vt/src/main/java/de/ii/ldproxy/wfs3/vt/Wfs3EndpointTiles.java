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
import de.ii.ldproxy.target.geojson.Wfs3OutputFormatGeoJson;
import de.ii.ldproxy.wfs3.api.Wfs3FeatureFormatExtension;
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
 * Handle responses under '/tiles'.
 *
 * TODO: Make support for the path configurable. Include in the configuration: min/max zoom level, automated seeding (based on the spatial extent) for specified zoom levels
 *
 * @author portele
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTiles implements OgcApiEndpointExtension, ConformanceClass {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("tiles")
            .addMethods(OgcApiContext.HttpMethods.GET)
            .subPathPattern("^/?(?:\\w+(?:/\\w+/\\w+/\\w+)?)?$")
            .build();

    @Requires
    private CrsTransformation crsTransformation;

    @Requires
    private OgcApiExtensionRegistry extensionRegistry;

    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();


    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);

    private final VectorTilesCache cache;

    Wfs3EndpointTiles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext) {
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiDatasetData dataset, String subPath) {
        if (subPath.matches("^/?(?:\\w+)?$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build());
        else if (subPath.matches("^/?(?:\\w+/\\w+/\\w+/\\w+)$"))
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
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/req/collections";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * retrieve all available tiling schemes
     *
     * @return all tiling schemes in a json array
     */
    @Path("/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTileMatrixSets(@Context OgcApiDataset service, @Context OgcApiRequestContext wfs3Request) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<Map<String, Object>> wfs3LinksList = new ArrayList<>();

        for (Object tileMatrixSetId : cache.getTileMatrixSetIds()
                                          .toArray()) {
            Map<String, Object> wfs3LinksMap = new HashMap<>();
            wfs3LinksMap.put("identifier", tileMatrixSetId);
            // TODO: json support
            wfs3LinksMap.put("links", vectorTilesLinkGenerator.generateTileMatrixSetLinks(wfs3Request.getUriCustomizer(), tileMatrixSetId.toString(), true, false));
            wfs3LinksList.add(wfs3LinksMap);
        }

        return Response.ok(ImmutableMap.of("tileMatrixSetLinks", wfs3LinksList))
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
    public Response getTilingScheme(@Auth Optional<User> optionalUser,
                                    @PathParam("tilingSchemeId") String tilingSchemeId, @Context OgcApiDataset service,
                                    @Context OgcApiRequestContext wfs3Request) throws IOException {
        checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));
        File file = cache.getTileMatrixSet(tilingSchemeId);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<OgcApiLink> ogcApiLink = vectorTilesLinkGenerator.generateTileMatrixSetLinks(wfs3Request.getUriCustomizer(), tilingSchemeId, true, false);


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

        jsonTilingScheme.put("links", ogcApiLink);

        return Response.ok(jsonTilingScheme)
                       .build();
    }

    /**
     * The tile in the requested tiling scheme, on the requested zoom level in the tiling scheme, with the requested grid coordinates (row, column) is returned.
     * Each collection of the dataset is returned as a separate layer.
     * The collections and the feature properties to include in the tile representation can be limited using query parameters.
     *
     * @param optionalUser      the user
     * @param tileMatrixSetId   the local identifier of a specific tiling scheme
     * @param tileMatrix        the zoom level of the tile as a string
     * @param tileRow           the row index of the tile on the selected zoom level
     * @param tileCol           the column index of the tile on the selected zoom level
     * @param collections       the collections that should be included in the tile. The parameter value is a list of collection identifiers.
     * @param properties        the properties that should be included for each feature. The parameter value is a list of property names.
     * @param service           the wfs3 service
     * @return all mvt's that match the criteria
     * @throws CrsTransformationException
     * @throws FileNotFoundException
     * @throws NotFoundException
     */
    @Path("/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    @Produces({"application/vnd.mapbox-vector-tile"})
    public Response xgetTileMVT(@Auth Optional<User> optionalUser, @PathParam("tileMatrixSetId") String tileMatrixSetId,
                                @PathParam("tileMatrix") String tileMatrix, @PathParam("tileRow") String tileRow,
                                @PathParam("tileCol") String tileCol, @QueryParam("collections") String collections,
                                @QueryParam("properties") String properties, @Context OgcApiDataset service,
                                @Context UriInfo uriInfo,
                                @Context OgcApiRequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        Wfs3FeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(Wfs3OutputFormatGeoJson.MEDIA_TYPE).orElseThrow(NotAcceptableException::new);

        // TODO support time
        // TODO support other filter parameters
        checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));

        LOGGER.debug("GET TILE MVT {} {} {} {} {} {}", service.getId(), "all", tileMatrixSetId, tileMatrix, tileRow, tileCol);
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


        VectorTile tile = new VectorTile(null, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);
        // generate tile
        File tileFileMvt = tile.getFile(cache, "pbf");

        Map<String, File> layers = new HashMap<String, File>();
        Set<String> collectionIds = getCollectionIdsDataset(vectorTileMapGenerator.getAllCollectionIdsWithTileExtension(service.getData()), vectorTileMapGenerator.getEnabledMap(service.getData()),
                vectorTileMapGenerator.getFormatsMap(service.getData()), vectorTileMapGenerator.getMinMaxMap(service.getData(), true), false, false, false);
        if (!tileFileMvt.exists()) {
            generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, service, tileMatrix, tileRow, tileCol, tileMatrixSetId, doNotCache, cache, wfs3Request, crsTransformation, uriInfo, false, wfs3OutputFormatGeoJson);
        } else {
            boolean invalid = false;

            for (String collectionId : collectionIds) {
                VectorTile layerTile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);
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
                generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, service, tileMatrix, tileRow, tileCol, tileMatrixSetId, doNotCache, cache, wfs3Request, crsTransformation, uriInfo, true, wfs3OutputFormatGeoJson);
            }
        }

        StreamingOutput streamingOutput = outputStream -> {
            ByteStreams.copy(new FileInputStream(tileFileMvt), outputStream);
        };

        return Response.ok(streamingOutput, "application/vnd.mapbox-vector-tile")
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
    @Produces({"application/geo+json", MediaType.APPLICATION_JSON})
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

        Wfs3CollectionFormatExtension wfs3OutputFormatGeoJson = extensionRegistry.getOutputFormats().get(Wfs3OutputFormatGeoJson.MEDIA_TYPE);

        for (String collectionId : collectionIds) {
            VectorTile.checkZoomLevel(Integer.parseInt(level), VectorTileMapGenerator.getMinMaxMap(wfsService.getData(),false),wfsService, wfs3OutputFormatGeoJson, collectionId, tilingSchemeId, MediaType.APPLICATION_JSON, row, col, doNotCache, cache, false, wfs3Request, crsTransformation);
            VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, "application/json", false);
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

        return Response.ok(streamingOutput, "application/geo+json")
                       .build();
    }
*/

    /**
     * checks if the tiles parameter is enabled in the dataset. If the tiles parameter is disabled in all collections, it throws a 404.
     *
     * @param enabledMap    a map with all collections and the boolean if the tiles support is enabled or not
     */
    static boolean checkTilesParameterDataset(Map<String, Boolean> enabledMap) {

        if (!Objects.isNull(enabledMap)) {
            for (String collectionId : enabledMap.keySet()) {
                if (enabledMap.get(collectionId))
                    return true;
            }
        }
        throw new NotFoundException();
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
    static Set<String> getCollectionIdsDataset(Set<String> allCollectionIds, Map<String, Boolean> enabledMap,
                                               Map<String, List<String>> formatsMap,
                                               Map<String, Map<String, TilesConfiguration.MinMax>> seedingMap,
                                               boolean mvtEnabled, boolean onlyJSONenabled, boolean startSeeding) {

        Set<String> collectionIdsFilter = new HashSet<>();

        if (!Objects.isNull(allCollectionIds)) {
            for (String collectionId : allCollectionIds) {
                if (!Objects.isNull(enabledMap) && enabledMap.containsKey(collectionId) && !Objects.isNull(formatsMap)
                        && formatsMap.containsKey(collectionId) && !Objects.isNull(seedingMap) && seedingMap.containsKey(collectionId)) {
                    Boolean tilesCollectionEnabled = enabledMap.get(collectionId);
                    List<String> formatsCollection = formatsMap.get(collectionId);
                    Map<String, TilesConfiguration.MinMax> seedingCollection = seedingMap.get(collectionId);

                    if (mvtEnabled) {
                        if (tilesCollectionEnabled && seedingCollection != null && formatsCollection.contains("application/vnd.mapbox-vector-tile")) {
                            collectionIdsFilter.add(collectionId);
                        }
                    } else if (onlyJSONenabled) {
                        if (tilesCollectionEnabled && formatsCollection.contains("application/json") && !formatsCollection.contains("application/vnd.mapbox-vector-tile")) {
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

    private void generateTileDataset(VectorTile tile, File tileFileMvt, Map<String, File> layers,
                                     Set<String> collectionIds, Set<String> requestedCollections,
                                     Set<String> requestedProperties, OgcApiDataset wfsService, String level,
                                     String row, String col, String tilingSchemeId, boolean doNotCache,
                                     VectorTilesCache cache, OgcApiRequestContext wfs3Request,
                                     CrsTransformation crsTransformation, UriInfo uriInfo, boolean invalid,
                                     Wfs3FeatureFormatExtension wfs3OutputFormatGeoJson) throws FileNotFoundException {

        for (String collectionId : collectionIds) {
            // include only the requested layers / collections

            if (requestedCollections != null && !requestedCollections.contains(collectionId))
                continue;
            if (VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, "application/vnd.mapbox-vector-tile", true)) {
                VectorTile.checkZoomLevel(Integer.parseInt(level), vectorTileMapGenerator.getMinMaxMap(wfsService.getData(), false), wfsService, wfs3OutputFormatGeoJson, collectionId, tilingSchemeId, "application/vnd.mapbox-vector-tile", row, col, doNotCache, cache, false, wfs3Request, crsTransformation);

                Map<String, File> layerCollection = new HashMap<String, File>();

                VectorTile tileCollection = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService, doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

                File tileFileMvtCollection = tileCollection.getFile(cache, "pbf");
                if (!tileFileMvtCollection.exists() || invalid) {
                    if (invalid)
                        tileFileMvtCollection.delete();

                    VectorTile layerTile = new VectorTile(collectionId, tilingSchemeId, level, row, col, wfsService, doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

                    File tileFileJson = layerTile.getFile(cache, "json");
                    if (!tileFileJson.exists()) {
                        OgcApiMediaType geojsonMediaType;
                        geojsonMediaType = new ImmutableOgcApiMediaType.Builder()
                                .type(new MediaType("application", "geo+json"))
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
                            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, null, null, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, layerTile);
                        }
                    }
                    layers.put(collectionId, tileFileJson);
                    layerCollection.put(collectionId, tileFileJson);

                }
                boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvtCollection, layerCollection, requestedProperties, crsTransformation, tile);
                if (!success) {
                    String msg = "Internal server error: could not generate protocol buffers for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            }
        }
        boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformation, tile);
        if (!success) {
            String msg = "Internal server error: could not generate protocol buffers for a tile.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }

    }

    private Optional<Wfs3FeatureFormatExtension> getOutputFormatForType(OgcApiMediaType mediaType) {
        return extensionRegistry.getExtensionsForType(Wfs3FeatureFormatExtension.class)
                                .stream()
                                .filter(wfs3OutputFormatExtension -> wfs3OutputFormatExtension.getMediaType()
                                                                                              .equals(mediaType))
                                .findFirst();
    }


}
