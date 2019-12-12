/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesEndpoint;
import de.ii.ldproxy.target.geojson.OgcApiFeaturesOutputFormatGeoJson;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformationException;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;
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
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * Handle responses under '/tiles'.
 *
 * TODO: Make support for the path configurable. Include in the configuration: min/max zoom level, automated seeding (based on the spatial extent) for specified zoom levels
 *
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTiles implements OgcApiEndpointExtension, ConformanceClass {

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("tiles")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?(?:\\w+(?:/(?:\\w+/\\w+/\\w+)?)?)?$")
            .build();

    @Requires
    I18n i18n;

    @Requires
    private CrsTransformation crsTransformation;

    @Requires
    private OgcApiExtensionRegistry extensionRegistry;

    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();

    private final TileMatrixSetLimitsGenerator limitsGenerator = new TileMatrixSetLimitsGenerator();

    private final CollectionsMultitilesGenerator collectionsMultitilesGenerator = new CollectionsMultitilesGenerator();

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
        if (subPath.matches("^/?$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build(),
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.TEXT_HTML_TYPE)
                            .build());
        else if (subPath.matches("^/?\\w+/?$")) {
            if (!isMultiTilesEnabledForApi(dataset))
                return ImmutableSet.of();

            final Map<String, String> parameterMapMvt = ImmutableMap.of(
                    "container", "application/zip",
                    "tiles", "application/vnd.mapbox-vector-tile");
            final Map<String, String> parameterMapGeoJson = ImmutableMap.of(
                    "container", "application/zip",
                    "tiles", "application/geo+json");
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(new MediaType("application", "vnd.ogc.multipart", parameterMapMvt))
                            .parameter("zipmvt")
                            .build(),
                    new ImmutableOgcApiMediaType.Builder()
                            .type(new MediaType("application", "vnd.ogc.multipart", parameterMapGeoJson))
                            .parameter("zipjson")
                            .build()
            );
        } else if (subPath.matches("^/?\\w+/\\w+/\\w+/\\w+$"))
            // TODO: from tile format configuration
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
    public ImmutableSet<String> getParameters(OgcApiDatasetData apiData, String subPath) {
        if (subPath.matches("^/?$")) {
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .build();
        } else if (subPath.matches("^/?\\w+/?$")) {
            if (!isMultiTilesEnabledForApi(apiData))
                return ImmutableSet.of();

            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .add("bbox", "scaleDenominator", "multiTileType", "f-tile")
                    .build();
        } else if (subPath.matches("^/?(?:\\w+/\\w+/\\w+/\\w+)$"))
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .addAll(extensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)
                            .stream()
                            .map(ext -> ext.getParameters(apiData, subPath))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet()))
                    .add("properties", "collections")
                    .build();

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/collections";
    }

    @Override
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .isPresent();
    }

    private boolean isMultiTilesEnabledForApi(OgcApiDatasetData apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .filter(TilesConfiguration::getMultiTilesEnabled)
                .isPresent();

    }

    /**
     * retrieve all available tiling schemes
     *
     * @return all tiling schemes in a json array
     */
    @Path("/")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getTileMatrixSets(@Context OgcApiDataset service, @Context OgcApiRequestContext requestContext) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        TileCollections tiles = ImmutableTileCollections.builder()
                .title(requestContext.getApi().getData().getLabel())
                .description(requestContext.getApi().getData().getDescription().orElse(""))
                .tileMatrixSetLinks(
                            cache.getTileMatrixSetIds()
                                .stream()
                                .map(tileMatrixSetId -> ImmutableTileCollection.builder()
                                    .tileMatrixSet(tileMatrixSetId)
                                    .addAllTileMatrixSetLimits(limitsGenerator.getTileMatrixSetLimits(service.getData(), tileMatrixSetId, crsTransformation))
                                    .build())
                                .collect(Collectors.toList()))
                .links(vectorTilesLinkGenerator.generateTilesLinks(
                        requestContext.getUriCustomizer(),
                        requestContext.getMediaType(),
                        requestContext.getAlternateMediaTypes(),
                        false, // TODO
                        false,
                        true,
                        false,
                        i18n,
                        requestContext.getLanguage()))
                .build();

        if (requestContext.getMediaType().matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<TileCollectionsFormatExtension> outputFormatHtml = requestContext.getApi().getOutputFormat(TileCollectionsFormatExtension.class, requestContext.getMediaType(), "/tiles");
            if (outputFormatHtml.isPresent())
                return outputFormatHtml.get().getTileCollectionsResponse(tiles, Optional.empty(), requestContext.getApi(), requestContext);

            throw new NotAcceptableException();
        }

        return Response.ok(tiles)
                       .build();
    }

    /**
     * Retrieve multiple tiles from multiple collections
     * @param optionalUser          the user
     * @param wfs3Request           the request
     * @param service               the wfs3 service
     * @param tileMatrixSetId       the local identifier of a specific tile matrix set
     * @param bboxParam             bounding box specified in WGS 84 longitude/latitude format
     * @param scaleDenominatorParam value of the scaleDenominator request parameter
     * @param multiTileType         value of the multiTileType request parameter
     * @param collectionsParam      value of the collections request parameter
     * @return multiple tiles from multiple collections
     */
    @Path("/{tileMatrixSetId}")
    @GET
    public Response getCollectionsMultitiles(@Auth Optional<User> optionalUser, @Context OgcApiRequestContext wfs3Request,
                                             @Context OgcApiDataset service, @PathParam("tileMatrixSetId") String tileMatrixSetId,
                                             @QueryParam("bbox") String bboxParam, @QueryParam("scaleDenominator") String scaleDenominatorParam,
                                             @QueryParam("multiTileType") String multiTileType, @QueryParam("collections") String collectionsParam,
                                             @Context UriInfo uriInfo) throws UnsupportedEncodingException {

        if (!isMultiTilesEnabledForApi(service.getData()))
            throw new NotFoundException();
        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE)
                .orElseThrow(NotAcceptableException::new);
        Map<String, Boolean> enabledMap = vectorTileMapGenerator.getEnabledMap(service.getData());
        Set<String> requestedCollections = parseCsv(collectionsParam);
        Set<String> collections = getEnabledCollections(enabledMap, requestedCollections);
        return collectionsMultitilesGenerator.getCollectionsMultitiles(tileMatrixSetId, bboxParam, scaleDenominatorParam,
                multiTileType, wfs3Request.getUriCustomizer(), collections, crsTransformation, uriInfo, service,
                wfs3Request, cache, wfs3OutputFormatGeoJson);
    }

    /**
     * Parse the list of comma separated values into a Set of Strings
     * @param csv string with comma separated parameter values
     * @return Set of parameter values
     */
    private Set<String> parseCsv(String csv) {
        Set<String> result = null;
        if (csv != null && !csv.trim().isEmpty()) {
            String[] sa = csv.trim().split(",");
            result = new HashSet<>();
            for (String s : sa) {
                result.add(s.trim());
            }
        }
        return result;
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
     * @param collectionsParam  the collections that should be included in the tile. The parameter value is a list of collection identifiers.
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
    public Response getTileMVT(@Auth Optional<User> optionalUser, @PathParam("tileMatrixSetId") String tileMatrixSetId,
                                @PathParam("tileMatrix") String tileMatrix, @PathParam("tileRow") String tileRow,
                                @PathParam("tileCol") String tileCol, @QueryParam("collections") String collectionsParam,
                                @QueryParam("properties") String properties, @Context OgcApiDataset service,
                                @Context UriInfo uriInfo, @Context OgcApiRequestContext wfs3Request)
            throws CrsTransformationException, FileNotFoundException, NotFoundException {

        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE).orElseThrow(NotAcceptableException::new);

        // TODO support datetime
        // TODO support other filter parameters
        Map<String, Boolean> enabledMap = vectorTileMapGenerator.getEnabledMap(service.getData());
        checkTilesParameterDataset(enabledMap);

        LOGGER.debug("GET TILE MVT {} {} {} {} {} {}", service.getId(), "all", tileMatrixSetId, tileMatrix, tileRow, tileCol);
        cache.cleanup(); // TODO centralize this

        // check and process parameters
        Set<String> requestedProperties = parseCsv(properties);
        Set<String> requestedCollections = parseCsv(collectionsParam);
        Set<String> collections = getEnabledCollections(enabledMap, requestedCollections);

        boolean doNotCache = (requestedProperties != null || requestedCollections != null);

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        doNotCache = false;

        Set<String> filterParameters = ImmutableSet.of();
        for (OgcApiParameterExtension parameterExtension : extensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            filterParameters = parameterExtension.getFilterParameters(filterParameters, service.getData());
        }

        final Map<String, String> filterableFields = collections.stream()
                .flatMap(collection -> service.getData()
                        .getFilterableFieldsForFeatureType(collection)
                        .entrySet()
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x1, x2) -> x1));

        final Map<String, String> filters = Wfs3EndpointTilesSingleCollection.getFiltersFromQuery(
                OgcApiFeaturesEndpoint.toFlatMap(queryParameters), filterableFields, filterParameters);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder();
        for (String collection : collections) {
            for (OgcApiParameterExtension parameterExtension : extensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
                parameterExtension.transformQuery(service.getData()
                        .getFeatureTypes()
                        .get(collection), queryBuilder, OgcApiFeaturesEndpoint.toFlatMap(queryParameters), service.getData());
            }
        }

        if (!filters.isEmpty() || queryParameters.containsKey("properties")) {
            doNotCache = true;
        }


        VectorTile tile = new VectorTile(null, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);
        // generate tile
        File tileFileMvt = tile.getFile(cache, "pbf");

        Map<String, File> layers = new HashMap<String, File>();
        Set<String> collectionIds = getCollectionIdsDataset(vectorTileMapGenerator.getAllCollectionIdsWithTileExtension(service.getData()), vectorTileMapGenerator.getEnabledMap(service.getData()),
                vectorTileMapGenerator.getFormatsMap(service.getData()), vectorTileMapGenerator.getMinMaxMap(service.getData(), true), false, false, false);
        if (!tileFileMvt.exists()) {
            generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, service, tileMatrix, tileRow, tileCol, tileMatrixSetId, doNotCache, cache, wfs3Request, crsTransformation, uriInfo, false, wfs3OutputFormatGeoJson, i18n, vectorTileMapGenerator, filters);
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
                generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, service, tileMatrix, tileRow, tileCol, tileMatrixSetId, doNotCache, cache, wfs3Request, crsTransformation, uriInfo, true, wfs3OutputFormatGeoJson, i18n, vectorTileMapGenerator, filters);
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
     * @param tileMatrixSetId the local identifier of a specific tiling scheme
     * @param level the zoom level of the tile as a string
     * @param row the row index of the tile on the selected zoom level
     * @param col the column index of the tile on the selected zoom level
     * @param service the wfs3 service
     * @return all geoJson features in json format
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException an error occurred when searching for a file
     */
/*
    @Path("/{tileMatrixSetId}/{level}/{row}/{col}")
    @GET
    @Produces({"application/geo+json", MediaType.APPLICATION_JSON})
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("level") String level, @PathParam("row") String row, @PathParam("col") String col, @Context Service service, @Context UriInfo uriInfo, @Context Wfs3RequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException {

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

        CollectionsFormatExtension wfs3OutputFormatGeoJson = extensionRegistry.getOutputFormats().get(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE);

        for (String collectionId : collectionIds) {
            VectorTile.checkZoomLevel(Integer.parseInt(level), VectorTileMapGenerator.getMinMaxMap(wfsService.getData(),false),wfsService, wfs3OutputFormatGeoJson, collectionId, tileMatrixSetId, MediaType.APPLICATION_JSON, row, col, doNotCache, cache, false, wfs3Request, crsTransformation);
            VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, "application/json", false);
        }
        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), "all", tileMatrixSetId, level, row, col);


        VectorTile tile = new VectorTile(null, tileMatrixSetId, level, row, col, wfsService.getData(), doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

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

    protected static void generateTileDataset(VectorTile tile, File tileFileMvt, Map<String, File> layers,
                                     Set<String> collectionIds, Set<String> requestedCollections,
                                     Set<String> requestedProperties, OgcApiDataset wfsService, String level,
                                     String row, String col, String tileMatrixSetId, boolean doNotCache,
                                     VectorTilesCache cache, OgcApiRequestContext wfs3Request,
                                     CrsTransformation crsTransformation, UriInfo uriInfo, boolean invalid,
                                     OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson, I18n i18n,
                                     VectorTileMapGenerator vectorTileMapGenerator, Map<String, String> filters)
            throws FileNotFoundException {

        for (String collectionId : collectionIds) {
            // include only the requested layers / collections

            if (requestedCollections != null && !requestedCollections.contains(collectionId))
                continue;
            if (VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, "application/vnd.mapbox-vector-tile", true)) {
                VectorTile.checkZoomLevel(Integer.parseInt(level), vectorTileMapGenerator.getMinMaxMap(wfsService.getData(), false), wfsService, wfs3OutputFormatGeoJson, collectionId, tileMatrixSetId, "application/vnd.mapbox-vector-tile", row, col, doNotCache, cache, false, wfs3Request, crsTransformation, i18n);

                Map<String, File> layerCollection = new HashMap<String, File>();

                VectorTile tileCollection = new VectorTile(collectionId, tileMatrixSetId, level, row, col, wfsService, doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

                File tileFileMvtCollection = tileCollection.getFile(cache, "pbf");
                if (!tileFileMvtCollection.exists() || invalid) {
                    if (invalid)
                        tileFileMvtCollection.delete();

                    VectorTile layerTile = new VectorTile(collectionId, tileMatrixSetId, level, row, col, wfsService, doNotCache, cache, wfsService.getFeatureProvider(), wfs3OutputFormatGeoJson);

                    Map<String, String> filterableFields = wfsService.getData()
                                                                     .getFilterableFieldsForFeatureType(collectionId);

                    File tileFileJson = layerTile.getFile(cache, "json");
                    if (!tileFileJson.exists()) {
                        OgcApiMediaType geojsonMediaType;
                        geojsonMediaType = new ImmutableOgcApiMediaType.Builder()
                                .type(new MediaType("application", "geo+json"))
                                .label("GeoJSON")
                                .build();
                        boolean success = TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), geojsonMediaType, false, layerTile, i18n, wfs3Request.getLanguage());
                        if (!success) {
                            String msg = "Internal server error: could not generate GeoJSON for a tile.";
                            LOGGER.error(msg);
                            throw new InternalServerErrorException(msg);
                        }
                    } else {
                        if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, layerTile, i18n, wfs3Request.getLanguage());
                        }
                    }
                    layers.put(collectionId, tileFileJson);
                    layerCollection.put(collectionId, tileFileJson);

                }
                boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvtCollection, layerCollection, requestedProperties, crsTransformation, tile, false);
                if (!success) {
                    String msg = "Internal server error: could not generate protocol buffers for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            }
        }
        boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformation, tile, false);
        if (!success) {
            String msg = "Internal server error: could not generate protocol buffers for a tile.";
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }

    }

    private Optional<OgcApiFeatureFormatExtension> getOutputFormatForType(OgcApiMediaType mediaType) {
        return extensionRegistry.getExtensionsForType(OgcApiFeatureFormatExtension.class)
                                .stream()
                                .filter(wfs3OutputFormatExtension -> wfs3OutputFormatExtension.getMediaType()
                                                                                              .equals(mediaType))
                                .findFirst();
    }

    /**
     * Given a set of collections from the request URI, return collections for which Tiles extension is enabled.
     * If requested collections URI parameter is absent/empty, return all collections from the dataset with Tiles extension enabled.
     * @param enabledMap map with all collection IDs as keys and their enabled status as value
     * @param requestedCollections set of collections requested in the URI parameter
     * @return Set of collections with Tiles extension enabled
     */
    private Set<String> getEnabledCollections(Map<String, Boolean> enabledMap, Set<String> requestedCollections) {
        Set<String> collections = new HashSet<>();
        if (enabledMap != null && !enabledMap.isEmpty()) {
            if (requestedCollections == null || requestedCollections.isEmpty()) {
                collections = enabledMap.entrySet()
                        .stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
            } else {
                collections = requestedCollections.stream()
                        .filter(enabledMap::containsKey)
                        .filter(enabledMap::get)
                        .collect(Collectors.toSet());
            }
        }
        return collections;
    }
}
