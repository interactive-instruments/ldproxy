/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesEndpoint;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesQuery;
import de.ii.ldproxy.target.geojson.OgcApiFeaturesOutputFormatGeoJson;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * Handle responses under '/collection/{collectionId}/tiles'.
 * <p>
 * TODO: Make support for the path configurable. Include in the configuration for each collection: min/max zoom level, automated seeding (based on the spatial extent) for specified zoom levels
 *
 *
 */
@Component
@Provides
@Instantiate
public class Wfs3EndpointTilesSingleCollection implements OgcApiEndpointExtension, ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTilesSingleCollection.class);
    private static final String TMS_REGEX = "(?:WebMercatorQuad|WorldCRS84Quad|WorldMercatorWGS84Quad)";

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^/?[[\\w\\-]\\-]+/tiles(?:/"+TMS_REGEX+"(?:/(?:metadata|(?:\\w+/\\w+/\\w+)?))?)?$")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .build();

    private final I18n i18n;
    //TODO: OgcApiTilesProviders (use features core featureProvider id as fallback)
    private final OgcApiFeatureCoreProviders providers;
    private final CrsTransformerFactory crsTransformerFactory;
    private final OgcApiExtensionRegistry wfs3ExtensionRegistry;
    private final OgcApiFeaturesQuery queryParser;
    private final VectorTileMapGenerator vectorTileMapGenerator;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final VectorTilesCache cache;
    private final CollectionMultitilesGenerator multiTilesGenerator;

    Wfs3EndpointTilesSingleCollection(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                                      @Requires I18n i18n,
                                      @Requires OgcApiFeatureCoreProviders providers,
                                      @Requires CrsTransformerFactory crsTransformerFactory,
                                      @Requires OgcApiExtensionRegistry wfs3ExtensionRegistry,
                                      @Requires OgcApiFeaturesQuery queryParser) {
        this.i18n = i18n;
        this.providers = providers;
        this.crsTransformerFactory = crsTransformerFactory;
        this.wfs3ExtensionRegistry = wfs3ExtensionRegistry;
        this.queryParser = queryParser;
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        this.cache = new VectorTilesCache(dataDirectory);
        this.vectorTileMapGenerator = new VectorTileMapGenerator();
        this.limitsGenerator = new TileMatrixSetLimitsGenerator();
        this.multiTilesGenerator = new CollectionMultitilesGenerator(providers, this.queryParser);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/?[\\w\\-]+/tiles/?$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build(),
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.TEXT_HTML_TYPE)
                            .build());
        else if (subPath.matches("^/?[\\w\\-]+/tiles/"+TMS_REGEX+"/?$")) {
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
        } else if (subPath.matches("^/?(?:[\\w\\-]+)/tiles/"+TMS_REGEX+"/metadata/?$")) {
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build());
        } else if (subPath.matches("^/?(?:[\\w\\-]+)/tiles/"+TMS_REGEX+"/\\w+/\\w+/\\w+/?$")) {
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
        }

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath) {
        if (subPath.matches("^/?[\\w\\-]+/tiles/?$")) {
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .build();
        } else if (subPath.matches("^/?[\\w\\-]+/tiles/"+TMS_REGEX+"/?$")) {
            if (!isMultiTilesEnabledForApi(apiData))
                return ImmutableSet.of();

            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .add("bbox", "scaleDenominator", "multiTileType", "f-tile")
                    .build();
        } else if (subPath.matches("^/?(?:[\\w\\-]+)/tiles/"+TMS_REGEX+"/metadata/?$")) {
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .build();
        } else if (subPath.matches("^/?(?:[\\w\\-]+)/tiles/"+TMS_REGEX+"/\\w+/\\w+/\\w+/?$")) {
            ImmutableSet<String> parametersFromExtensions = new ImmutableSet.Builder<String>()
                    .addAll(wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)
                            .stream()
                            .map(ext -> ext.getParameters(apiData, subPath))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet()))
                    .build();

            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .add("properties", "datetime")
                    .addAll(parametersFromExtensions)
                    .build();
        }

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public String getConformanceClass() {
        return "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/core";
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    private boolean isMultiTilesEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiTilesEnabled)
                .isPresent();
    }

    /**
     * Retrieve more than one tile from a single collection in a single request
     * @param service               the wfs3 service
     * @param collectionId          the id of the collection to which the tiles belong
     * @param tileMatrixSetId       the local identifier of a specific tile matrix set
     * @param bboxParam             bounding box specified in WGS 84 longitude/latitude format
     * @param scaleDenominatorParam value of the scaleDenominator request parameter
     * @param multiTileType         value of the multiTileType request parameter
     * @return                      tileSet document
     */
    @Path("/{collectionId}/tiles/{tileMatrixSetId}")
    @GET
    public Response getMultitiles(@Context OgcApiRequestContext wfs3Request, @Context OgcApiApi service, @Context UriInfo uriInfo,
                                  @PathParam("collectionId") String collectionId, @PathParam("tileMatrixSetId") String tileMatrixSetId,
                                  @QueryParam("bbox") String bboxParam, @QueryParam("scaleDenominator") String scaleDenominatorParam,
                                  @QueryParam("multiTileType") String multiTileType, @QueryParam("f-tile") String tileFormat) {

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);
        if (!isMultiTilesEnabledForApi(service.getData()))
            throw new NotFoundException();
        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE)
                .orElseThrow(NotAcceptableException::new);

        return multiTilesGenerator.getMultitiles(tileMatrixSetId, bboxParam, scaleDenominatorParam, multiTileType,
                wfs3Request.getUriCustomizer(), tileFormat, collectionId, crsTransformerFactory, uriInfo, i18n,
                wfs3Request.getLanguage(), service, cache, wfs3OutputFormatGeoJson);
    }

    /**
     * retrieve all available tile matrix sets from the collection
     *
     * @return all tile matrix sets from the collection in a json array
     */
    @Path("/{collectionId}/tiles")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getTileMatrixSets(@Context OgcApiApi service, @Context OgcApiRequestContext requestContext,
                                      @PathParam("collectionId") String collectionId) {

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        FeatureTypeConfigurationOgcApi featureTypeConfiguration = requestContext.getApi().getData().getCollections().get(collectionId);
        Map<String, MinMax> tileMatrixSetZoomLevels = getTileMatrixSetZoomLevels(service.getData(), collectionId);
        TileCollections tiles = ImmutableTileCollections.builder()
                .title(featureTypeConfiguration.getLabel())
                .description(featureTypeConfiguration.getDescription().orElse(""))
                .tileMatrixSetLinks(
                        tileMatrixSetZoomLevels
                                .keySet()
                                .stream()
                                .map(tileMatrixSetId -> ImmutableTileCollection.builder()
                                        .tileMatrixSet(tileMatrixSetId)
                                        .tileMatrixSetURI(requestContext.getUriCustomizer()
                                                .copy()
                                                .removeLastPathSegments(3)
                                                .clearParameters()
                                                .ensureLastPathSegments("tileMatrixSets", tileMatrixSetId)
                                                .toString())
                                        .tileMatrixSetLimits(limitsGenerator.getCollectionTileMatrixSetLimits(
                                                service.getData(), collectionId, tileMatrixSetId,
                                                tileMatrixSetZoomLevels.get(tileMatrixSetId), crsTransformerFactory))
                                        .build())
                                .collect(Collectors.toList()))
                .links(vectorTilesLinkGenerator.generateTilesLinks(
                        requestContext.getUriCustomizer(),
                        requestContext.getMediaType(),
                        requestContext.getAlternateMediaTypes(),
                        false, // TODO
                        true,
                        false,
                        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/vnd.mapbox-vector-tile", true),
                        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/geo+json", true),
                        isMultiTilesEnabledForApi(service.getData()),
                        i18n,
                        requestContext.getLanguage()))
                .build();

        if (requestContext.getMediaType().matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<TileCollectionsFormatExtension> outputFormatHtml = requestContext.getApi().getOutputFormat(TileCollectionsFormatExtension.class, requestContext.getMediaType(), "/tiles");
            if (outputFormatHtml.isPresent())
                return outputFormatHtml.get().getTileCollectionsResponse(tiles, Optional.of(collectionId), requestContext.getApi(), requestContext);

            throw new NotAcceptableException();
        }

        return Response.ok(tiles)
                .build();

    }

    /**
     * retrieve tilejson for the MVT tile sets
     *
     * @return a tilejson file
     */
    @Path("/{collectionId}/tiles/{tileMatrixSetId : "+TMS_REGEX+"}/metadata")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTileSetMetadata(@Context OgcApiApi service,
                                       @Context OgcApiRequestContext requestContext,
                                       @PathParam("collectionId") String collectionId,
                                       @PathParam("tileMatrixSetId") String tileMatrixSetId) {

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<OgcApiLink> links = vectorTilesLinkGenerator.generateTilesLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                false, // TODO
                true,
                true,
                VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/vnd.mapbox-vector-tile", true),
                VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/geo+json", true),
                false,
                i18n,
                requestContext.getLanguage());

        MinMax zoomLevels = getTileMatrixSetZoomLevels(service.getData(),collectionId).get(tileMatrixSetId);

        Map<String, Object> tilejson = new VectorTilesMetadataGenerator().generateTilejson(providers, service.getData(), Optional.of(collectionId), tileMatrixSetId, zoomLevels, links, i18n, requestContext.getLanguage());

        return Response.ok(tilejson)
                .build();
    }

    /**
     * Retrieve a tile of the collection. The tile in the requested tiling scheme,
     * on the requested zoom level in the tiling scheme, with the
     * requested grid coordinates (row, column) is returned. The tile has a single
     * layer with all selected features in the bounding box of the tile. The feature properties to include in the tile
     * representation can be limited using a query parameter.
     *
     * @param optionalUser      the user
     * @param collectionId      the id of the collection in which the tiles belong
     * @param tileMatrixSetId   the local identifier of a specific tiling scheme
     * @param tileMatrix        the zoom level of the tile as a string
     * @param tileRow           the row index of the tile on the selected zoom level
     * @param tileCol           the column index of the tile on the selected zoom level
     * @param properties        the properties that should be included for each feature. The parameter value is a list of property names
     * @param service           the wfs3 service
     * @return a mvt file
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException      an error occurred when searching for a file
     * @throws NotFoundException          an error occurred when a resource is not found
     */
    @Path("/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    @Produces({"application/vnd.mapbox-vector-tile"})
    public Response getTileMVT(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                               @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                               @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                               @QueryParam("properties") String properties, @Context OgcApiApi service,
                               @Context UriInfo uriInfo,
                               @Context OgcApiRequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException, NotFoundException {

        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE).orElseThrow(NotAcceptableException::new);

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);
        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/vnd.mapbox-vector-tile", false);
        checkTileMatrixSetId(tileMatrixSetId, collectionId, service.getData());
        boolean doNotCache = false;

        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = service.getData()
                                                            .getCollections()
                                                            .get(collectionId)
                                                            .getExtension(OgcApiFeaturesCoreConfiguration.class)
                                                            .map(OgcApiFeaturesCoreConfiguration::getAllFilterParameters)
                                                            .orElse(ImmutableMap.of());

        Map<String, List<PredefinedFilter>> predefFilters = service.getData()
                .getCollections()
                .get(collectionId)
                .getExtension(TilesConfiguration.class)
                .orElse(null)
                .getFilters();

        Set<String> filterParameters = ImmutableSet.of();
        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            filterParameters = parameterExtension.getFilterParameters(filterParameters, service.getData());
        }

        final Map<String, String> filters = getFiltersFromQuery(OgcApiFeaturesEndpoint.toFlatMap(queryParameters), filterableFields, filterParameters);
        if (!filters.isEmpty() || queryParameters.containsKey("properties")) {
            doNotCache = true;
        }

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                                                                                .type(collectionId);

        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            parameterExtension.transformQuery(service.getData()
                    .getCollections()
                    .get(collectionId), queryBuilder, OgcApiFeaturesEndpoint.toFlatMap(queryParameters), service.getData());
        }

        FeatureProvider2 featureProvider = providers.getFeatureProvider(service.getData());

        VectorTile.checkZoomLevel(Integer.parseInt(tileMatrix), vectorTileMapGenerator.getMinMaxMap(service.getData(), false), service, wfs3OutputFormatGeoJson, collectionId, tileMatrixSetId, "application/vnd.mapbox-vector-tile", tileRow, tileCol, doNotCache, cache, true, wfs3Request, crsTransformerFactory, i18n);
        checkTileValidity(collectionId, tileMatrixSetId, Integer.parseInt(tileMatrix), Integer.parseInt(tileRow), Integer.parseInt(tileCol), service.getData());

        LOGGER.debug("GET TILE MVT {} {} {} {} {} {}", service.getId(), collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol);
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

        VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, featureProvider, wfs3OutputFormatGeoJson);

        File tileFileMvt = tile.getFile(cache, "pbf");
        if (!tileFileMvt.exists()) {

            VectorTile jsonTile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, featureProvider, wfs3OutputFormatGeoJson);
            File tileFileJson = jsonTile.getFile(cache, "json");
            if (!tileFileJson.exists()) {
                OgcApiMediaType geojsonMediaType;
                geojsonMediaType = new ImmutableOgcApiMediaType.Builder()
                        .type(new MediaType("application", "geo+json"))
                        .label("GeoJSON")
                        .build();
                boolean success = TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, uriInfo, predefFilters, filters, filterableFields, wfs3Request.getUriCustomizer(), geojsonMediaType, true, jsonTile, i18n, wfs3Request.getLanguage(), queryParser);
                if (!success) {
                    String msg = "Internal server error: could not generate GeoJSON for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            } else {
                if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                   TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, uriInfo, predefFilters, filters, filterableFields,
                           wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage(), queryParser);
                }
            }

            generateTileCollection(collectionId, tileFileJson, tileFileMvt, tile, requestedProperties, crsTransformerFactory);
        } else {
            VectorTile jsonTile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, featureProvider, wfs3OutputFormatGeoJson);
            File tileFileJson = jsonTile.getFile(cache, "json");

            if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, uriInfo, predefFilters, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage(), queryParser);
                tileFileMvt.delete();
                generateTileCollection(collectionId, tileFileJson, tileFileMvt, tile, requestedProperties, crsTransformerFactory);
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
     * @param optionalUser      the user
     * @param collectionId      the id of the collection in which the tiles belong
     * @param tileMatrixSetId   the local identifier of a specific tiling scheme
     * @param tileMatrix        the zoom level of the tile as a string
     * @param tileRow           the row index of the tile on the selected zoom level
     * @param tileCol           the column index of the tile on the selected zoom level
     * @param service           the wfs3 service
     * @return a geoJson feature in json format
     * @throws CrsTransformationException an error occurred when transforming the coordinates
     * @throws FileNotFoundException      an error occurred when searching for a file
     */

    @Path("/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    @Produces({"application/geo+json", MediaType.APPLICATION_JSON})
    public Response getTileJson(@Auth Optional<User> optionalUser, @PathParam("collectionId") String collectionId,
                                @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                                @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                                @Context OgcApiApi service, @Context UriInfo uriInfo,
                                @Context OgcApiRequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException {

        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE)
        															.orElseThrow(NotAcceptableException::new);

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);
        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/geo+json", false);
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        checkTileMatrixSetId(tileMatrixSetId, collectionId, service.getData());

        Set<String> filterParameters = ImmutableSet.of();
        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            filterParameters = parameterExtension.getFilterParameters(filterParameters, service.getData());
        }

        final Map<String, String> filterableFields = service.getData()
                                                            .getCollections()
                                                            .get(collectionId)
                                                            .getExtension(OgcApiFeaturesCoreConfiguration.class)
                                                            .map(OgcApiFeaturesCoreConfiguration::getAllFilterParameters)
                                                            .orElse(ImmutableMap.of());
        final Map<String, String> filters = getFiltersFromQuery(OgcApiFeaturesEndpoint.toFlatMap(queryParameters), filterableFields, filterParameters);

        Map<String, List<PredefinedFilter>> predefFilters = service.getData()
                .getCollections()
                .get(collectionId)
                .getExtension(TilesConfiguration.class)
                .orElse(null)
                .getFilters();

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder()
                .type(collectionId);

        for (OgcApiParameterExtension parameterExtension : wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
            parameterExtension.transformQuery(service.getData()
                    .getCollections()
                    .get(collectionId), queryBuilder, OgcApiFeaturesEndpoint.toFlatMap(queryParameters), service.getData());
        }


        boolean doNotCache = false;
        if (!filters.isEmpty() || queryParameters.containsKey("properties"))
            doNotCache = true;

        FeatureProvider2 featureProvider = providers.getFeatureProvider(service.getData());

        VectorTile.checkZoomLevel(Integer.parseInt(tileMatrix), vectorTileMapGenerator.getMinMaxMap(service.getData(), false), service, wfs3OutputFormatGeoJson, collectionId, tileMatrixSetId, MediaType.APPLICATION_JSON, tileRow, tileCol, doNotCache, cache, true, wfs3Request, crsTransformerFactory, i18n);
        checkTileValidity(collectionId, tileMatrixSetId, Integer.parseInt(tileMatrix), Integer.parseInt(tileRow), Integer.parseInt(tileCol), service.getData());

        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol);

        VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, featureProvider, wfs3OutputFormatGeoJson);

        File tileFileJson = tile.getFile(cache, "json");

        //TODO parse file (check if valid) if not valid delete it and generate new one

        if (!tileFileJson.exists()) {
            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, uriInfo, predefFilters, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage(), queryParser);
        } else {
            if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, uriInfo, predefFilters, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage(), queryParser);
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

    public static Map<String, String> getFiltersFromQuery(Map<String, String> query, Map<String, String> filterableFields,
                                                    Set<String> filterParameters) {


        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterParameters.contains(filterKey)) {
                String filterValue = query.get(filterKey);
                filters.put(filterKey, filterValue);
            } else if (filterableFields.containsKey(filterKey)) {
                String filterValue = query.get(filterKey);
                filters.put(filterKey, filterValue);
            }

        }
        return filters;
    }


    public static void generateTileCollection(String collectionId, File tileFileJson, File tileFileMvt, VectorTile tile,
                                              Set<String> requestedProperties, CrsTransformerFactory crsTransformerFactory) {

        Map<String, File> layers = new HashMap<>();
        layers.put(collectionId, tileFileJson);
        boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformerFactory, tile, false);
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

    private Map<String, MinMax> getTileMatrixSetZoomLevels(OgcApiApiDataV2 data, String collectionId) {
        TilesConfiguration tilesConfiguration = getExtensionConfiguration(data, data.getCollections().get(collectionId), TilesConfiguration.class).get();
        return tilesConfiguration.getZoomLevels();
    }

    private void checkTileMatrixSetId(String tileMatrixSetId, String collectionId, OgcApiApiDataV2 data) {
        Set<String> tileMatrixSets = getTileMatrixSetZoomLevels(data, collectionId).keySet();
        if (!tileMatrixSets.contains(tileMatrixSetId)) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }
    }

    private void checkTileValidity(String collectionId, String tileMatrixSetId, int tileMatrix, int tileRow, int tileCol, OgcApiApiDataV2 data) {
        // tileMatrixSetId and tileMatrix have been checked already
        TileMatrixSet tileMatrixSet = null;
        try {
            tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);
        }
        catch (InternalServerErrorException e) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }

        try {
            BoundingBox bbox = data.getSpatialExtent(collectionId, crsTransformerFactory, tileMatrixSet.getCrs());
            TileMatrixSetLimits limits = tileMatrixSet.getLimits(tileMatrix, bbox);
            if (tileRow > limits.getMaxTileRow() || tileRow < limits.getMinTileRow() || tileCol > limits.getMaxTileCol() || tileCol < limits.getMinTileCol())
                throw new NotFoundException("Tile outside of the area for collection "+collectionId);
        } catch (CrsTransformationException e) {
            String msg = String.format("Internal coordinate transformation error while checking the validity of requested tile %s/%d/%d/%d in collection ", tileMatrixSetId, tileMatrix, tileRow, tileCol, collectionId);
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }
    }

}
