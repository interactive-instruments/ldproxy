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
import de.ii.ldproxy.ogcapi.application.DefaultLinksGenerator;
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
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.hsqldb.Server;
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
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs3EndpointTiles.class);
    private static final String TMS_REGEX = "(?:WebMercatorQuad|WorldCRS84Quad|WorldMercatorWGS84Quad)";

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("tiles")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?(?:"+TMS_REGEX+"(?:/(?:metadata|(?:\\w+/\\w+/\\w+)?))?)?$")
            .build();

    private final I18n i18n;
    //TODO: OgcApiTilesProviders (use features core featureProvider id as fallback)
    private final OgcApiFeatureCoreProviders providers;
    private final CrsTransformerFactory crsTransformerFactory;
    private final OgcApiExtensionRegistry extensionRegistry;
    private final OgcApiFeaturesQuery queryParser;
    private final VectorTileMapGenerator vectorTileMapGenerator;
    private final TileMatrixSetLimitsGenerator limitsGenerator;
    private final CollectionsMultitilesGenerator collectionsMultitilesGenerator;
    private final VectorTilesCache cache;

    Wfs3EndpointTiles(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                      @Requires I18n i18n,
                      @Requires OgcApiFeatureCoreProviders providers,
                      @Requires CrsTransformerFactory crsTransformerFactory,
                      @Requires OgcApiExtensionRegistry extensionRegistry,
                      @Requires OgcApiFeaturesQuery queryParser) {
        this.i18n = i18n;
        this.providers = providers;
        this.crsTransformerFactory = crsTransformerFactory;
        this.extensionRegistry = extensionRegistry;
        this.queryParser = queryParser;
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        this.cache = new VectorTilesCache(dataDirectory);
        this.vectorTileMapGenerator = new VectorTileMapGenerator();
        this.limitsGenerator = new TileMatrixSetLimitsGenerator();
        this.collectionsMultitilesGenerator = new CollectionsMultitilesGenerator(i18n, providers, this);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 dataset, String subPath) {
        if (subPath.matches("^/?$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build(),
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.TEXT_HTML_TYPE)
                            .build());
        else if (subPath.matches("^/?"+TMS_REGEX+"/metadata/?$")) {
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build());
        } else if (subPath.matches("^/?"+TMS_REGEX+"/?$")) {
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
    public ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath) {
        if (subPath.matches("^/?$")) {
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .build();
        } else if (subPath.matches("^/?"+TMS_REGEX+"/metadata/?$")) {
            return new ImmutableSet.Builder<String>()
                    .addAll(OgcApiEndpointExtension.super.getParameters(apiData, subPath))
                    .build();
        } else if (subPath.matches("^/?"+TMS_REGEX+"/?$")) {
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
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .isPresent();
    }

    private boolean isMultiTilesEnabledForApi(OgcApiApiDataV2 apiData) {
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
    public Response getTileMatrixSets(@Context OgcApiApi service, @Context OgcApiRequestContext requestContext) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        Map<String, MinMax> tileMatrixSetZoomLevels = getTileMatrixSetZoomLevels(service.getData());

        TileCollections tiles = ImmutableTileCollections.builder()
                                                        .title(requestContext.getApi()
                                                                             .getData()
                                                                             .getLabel())
                                                        .description(requestContext.getApi()
                                                                                   .getData()
                                                                                   .getDescription()
                                                                                   .orElse(""))
                .tileMatrixSetLinks(
                        tileMatrixSetZoomLevels
                                .keySet()
                                .stream()
                                .map(tileMatrixSetId -> ImmutableTileCollection.builder()
                                    .tileMatrixSet(tileMatrixSetId)
                                    .tileMatrixSetURI(requestContext.getUriCustomizer()
                                            .copy()
                                            .removeLastPathSegments(1)
                                            .clearParameters()
                                            .ensureLastPathSegments("tileMatrixSets", tileMatrixSetId)
                                            .toString())
                                    .addAllTileMatrixSetLimits(limitsGenerator.getTileMatrixSetLimits(service.getData(),
                                            tileMatrixSetId, tileMatrixSetZoomLevels.get(tileMatrixSetId), crsTransformerFactory))
                                    .build())
                                .collect(Collectors.toList()))
                .links(vectorTilesLinkGenerator.generateTilesLinks(
                        requestContext.getUriCustomizer(),
                        requestContext.getMediaType(),
                        requestContext.getAlternateMediaTypes(),
                        false, // TODO
                        false,
                        false,
                        true,
                        false,
                        isMultiTilesEnabledForApi(service.getData()),
                        i18n,
                        requestContext.getLanguage()))
                .build();

        if (requestContext.getMediaType()
                          .matches(MediaType.TEXT_HTML_TYPE)) {
            Optional<TileCollectionsFormatExtension> outputFormatHtml = requestContext.getApi()
                                                                                      .getOutputFormat(TileCollectionsFormatExtension.class, requestContext.getMediaType(), "/tiles");
            if (outputFormatHtml.isPresent())
                return outputFormatHtml.get()
                                       .getTileCollectionsResponse(tiles, Optional.empty(), requestContext.getApi(), requestContext);

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
    @Path("/{tileMatrixSetId : "+TMS_REGEX+"}/metadata")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTileSetMetadata(@Context OgcApiApi service,
                                       @Context OgcApiRequestContext requestContext,
                                       @PathParam("tileMatrixSetId") String tileMatrixSetId) {

        Wfs3EndpointTiles.checkTilesParameterDataset(vectorTileMapGenerator.getEnabledMap(service.getData()));

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();
        List<OgcApiLink> links = vectorTilesLinkGenerator.generateTilesLinks(
                requestContext.getUriCustomizer(),
                requestContext.getMediaType(),
                requestContext.getAlternateMediaTypes(),
                false, // TODO
                false,
                true,
                true,
                false,
                false,
                i18n,
                requestContext.getLanguage());
        String tilesUriTemplate = links.stream()
                .filter(link -> link.getRel().equalsIgnoreCase("item") && link.getType().equalsIgnoreCase("application/vnd.mapbox-vector-tile"))
                .findFirst()
                .map(link -> link.getHref())
                .orElseThrow(() -> new ServerErrorException(500));

        ImmutableMap.Builder<String,Object> tilejson = ImmutableMap.<String,Object>builder()
                .put("tilejson", "3.0.0")
                .put("name", service.getData().getLabel())
                .put("description", service.getData().getDescription().orElse(""))
                .put("tiles", ImmutableList.of(tilesUriTemplate));

        // TODO: add support for attribution and version (manage revisions to the data)

        BoundingBox bbox = service.getData().getSpatialExtent();
        if (Objects.nonNull(bbox))
            tilejson.put("bounds", ImmutableList.of(bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax()) );

        Map<String, MinMax> tileMatrixSetZoomLevels = getTileMatrixSetZoomLevels(service.getData());

        MinMax minmax = tileMatrixSetZoomLevels.get(tileMatrixSetId);
        if (Objects.nonNull(minmax))
            tilejson.put("minzoom", minmax.getMin() )
                    .put("maxzoom", minmax.getMax() );

        ValueBuilderMap<FeatureTypeConfigurationOgcApi, ImmutableFeatureTypeConfigurationOgcApi.Builder> featureTypesApi = service.getData().getCollections();

        List<ImmutableMap<String, Object>> layers = featureTypesApi.values().stream()
                .map(featureTypeApi -> {
                    FeatureProvider2 featureProvider = providers.getFeatureProvider(service.getData(), featureTypeApi);
                    FeatureType featureType = featureProvider.getData()
                            .getTypes()
                            .get(featureTypeApi.getId());
                    Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = featureTypeApi.getExtension(OgcApiFeaturesCoreConfiguration.class);

                    SchemaObject schema = service.getData().getSchema(featureType);
                    ImmutableMap.Builder<String, Object> fieldsBuilder = ImmutableMap.<String, Object>builder();
                    schema.properties.stream()
                            .forEach(prop -> {
                                boolean isArray = prop.maxItems > 1;
                                if (prop.literalType.isPresent()) {
                                    fieldsBuilder.put(prop.id, prop.literalType.get().concat(isArray ? " (0..*)" : " (0..1)"));
                                } else if (prop.wellknownType.isPresent()) {
                                    switch (prop.wellknownType.get()) {
                                        case "Link":
                                            fieldsBuilder.put(prop.id, "Link".concat(isArray ? " (0..*)" : " (0..1)"));
                                            break;
                                        case "Point":
                                        case "MultiPoint":
                                        case "LineString":
                                        case "MultiLineString":
                                        case "Polygon":
                                        case "MultiPolygon":
                                        case "Geometry":
                                        default:
                                            break;
                                    }
                                } else if (prop.objectType.isPresent()) {
                                    fieldsBuilder.put(prop.id, "Object".concat(isArray ? " (0..*)" : " (0..1)"));
                                }
                            });

                    return ImmutableMap.<String, Object>builder()
                            .put("id", featureTypeApi.getId())
                            .put("description", featureTypeApi.getDescription().orElse(""))
                            .put("fields", fieldsBuilder.build())
                            .build();
                })
                .collect(Collectors.toList());
        tilejson.put("vector_layers", layers);

        return Response.ok(tilejson.build())
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
    @Path("/{tileMatrixSetId : "+TMS_REGEX+"}")
    @GET
    public Response getCollectionsMultitiles(@Auth Optional<User> optionalUser,
                                             @Context OgcApiRequestContext wfs3Request,
                                             @Context OgcApiApi service,
                                             @PathParam("tileMatrixSetId") String tileMatrixSetId,
                                             @QueryParam("bbox") String bboxParam,
                                             @QueryParam("scaleDenominator") String scaleDenominatorParam,
                                             @QueryParam("multiTileType") String multiTileType,
                                             @QueryParam("collections") String collectionsParam,
                                             @Context UriInfo uriInfo) throws UnsupportedEncodingException {

        if (!isMultiTilesEnabledForApi(service.getData())) {
            throw new NotFoundException();
        }
        checkTileMatrixSet(service.getData(), tileMatrixSetId);
        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE)
                .orElseThrow(NotAcceptableException::new);
        Map<String, Boolean> enabledMap = vectorTileMapGenerator.getEnabledMap(service.getData());
        Set<String> requestedCollections = parseCsv(collectionsParam);
        Set<String> collections = getEnabledCollections(enabledMap, requestedCollections);
        return collectionsMultitilesGenerator.getCollectionsMultitiles(tileMatrixSetId, bboxParam, scaleDenominatorParam,
                multiTileType, wfs3Request.getUriCustomizer(), collections, crsTransformerFactory, uriInfo, service,
                wfs3Request, cache, wfs3OutputFormatGeoJson);
    }

    /**
     * Parse the list of comma separated values into a Set of Strings
     * @param csv string with comma separated parameter values
     * @return Set of parameter values
     */
    private Set<String> parseCsv(String csv) {
        Set<String> result = null;
        if (csv != null && !csv.trim()
                               .isEmpty()) {
            String[] sa = csv.trim()
                             .split(",");
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
                                @QueryParam("properties") String properties, @Context OgcApiApi service,
                                @Context UriInfo uriInfo, @Context OgcApiRequestContext wfs3Request)
            throws CrsTransformationException, FileNotFoundException, NotFoundException {

        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE).orElseThrow(NotAcceptableException::new);

        // TODO support datetime
        // TODO support other filter parameters
        Map<String, Boolean> enabledMap = vectorTileMapGenerator.getEnabledMap(service.getData());
        checkTilesParameterDataset(enabledMap);
        checkTileMatrixSet(service.getData(), tileMatrixSetId);

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
                                                                                              .getCollections()
                                                                                              .get(collection)
                                                                                              .getExtension(OgcApiFeaturesCoreConfiguration.class)
                                                                                              .map(OgcApiFeaturesCoreConfiguration::getAllFilterParameters)
                                                                                              .orElse(ImmutableMap.of())
                        .entrySet()
                        .stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x1, x2) -> x1));

        final Map<String, String> filters = Wfs3EndpointTilesSingleCollection.getFiltersFromQuery(
                OgcApiFeaturesEndpoint.toFlatMap(queryParameters), filterableFields, filterParameters);

        final ImmutableFeatureQuery.Builder queryBuilder = ImmutableFeatureQuery.builder();
        for (String collection : collections) {
            for (OgcApiParameterExtension parameterExtension : extensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)) {
                parameterExtension.transformQuery(service.getData()
                        .getCollections()
                        .get(collection), queryBuilder, OgcApiFeaturesEndpoint.toFlatMap(queryParameters), service.getData());
            }
        }

        if (!filters.isEmpty() || queryParameters.containsKey("properties")) {
            doNotCache = true;
        }

        FeatureProvider2 featureProvider = providers.getFeatureProvider(service.getData());
        
        VectorTile tile = new VectorTile(null, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, featureProvider, wfs3OutputFormatGeoJson);
        
        VectorTile.checkZoomLevel(Integer.parseInt(tileMatrix), vectorTileMapGenerator.getMinMaxMap(service.getData(), false), service, wfs3OutputFormatGeoJson, null, tileMatrixSetId, MediaType.APPLICATION_JSON, tileRow, tileCol, doNotCache, cache, true, wfs3Request, crsTransformerFactory, i18n);
        checkTileValidity(tileMatrixSetId, Integer.parseInt(tileMatrix), Integer.parseInt(tileRow), Integer.parseInt(tileCol), service.getData());

        // TODO check tileMatrix, tileRow, tileCol

        // generate tile
        File tileFileMvt = tile.getFile(cache, "pbf");

        Map<String, File> layers = new HashMap<String, File>();
        Set<String> collectionIds = getCollectionIdsDataset(vectorTileMapGenerator.getAllCollectionIdsWithTileExtension(service.getData()), vectorTileMapGenerator.getEnabledMap(service.getData()),
                vectorTileMapGenerator.getFormatsMap(service.getData()), vectorTileMapGenerator.getMinMaxMap(service.getData(), true), false, false, false);
        if (!tileFileMvt.exists()) {
            generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, service, featureProvider, tileMatrix, tileRow, tileCol, tileMatrixSetId, doNotCache, cache, wfs3Request, crsTransformerFactory, uriInfo, false, wfs3OutputFormatGeoJson, i18n, vectorTileMapGenerator, filters);
        } else {
            boolean invalid = false;

            for (String collectionId : collectionIds) {
                VectorTile layerTile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, featureProvider, wfs3OutputFormatGeoJson);
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
                generateTileDataset(tile, tileFileMvt, layers, collectionIds, requestedCollections, requestedProperties, service, featureProvider, tileMatrix, tileRow, tileCol, tileMatrixSetId, doNotCache, cache, wfs3Request, crsTransformerFactory, uriInfo, true, wfs3OutputFormatGeoJson, i18n, vectorTileMapGenerator, filters);
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
            VectorTile.checkFormat(VectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, "application/geo+json", false);
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
                                               Map<String, Map<String, MinMax>> seedingMap,
                                               boolean mvtEnabled, boolean onlyJSONenabled, boolean startSeeding) {

        Set<String> collectionIdsFilter = new HashSet<>();

        if (!Objects.isNull(allCollectionIds)) {
            for (String collectionId : allCollectionIds) {
                if (!Objects.isNull(enabledMap) && enabledMap.containsKey(collectionId) && !Objects.isNull(formatsMap)
                        && formatsMap.containsKey(collectionId) && !Objects.isNull(seedingMap) && seedingMap.containsKey(collectionId)) {
                    Boolean tilesCollectionEnabled = enabledMap.get(collectionId);
                    List<String> formatsCollection = formatsMap.get(collectionId);
                    Map<String, MinMax> seedingCollection = seedingMap.get(collectionId);

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

    protected void generateTileDataset(VectorTile tile, File tileFileMvt, Map<String, File> layers,
                                              Set<String> collectionIds, Set<String> requestedCollections,
                                              Set<String> requestedProperties, OgcApiApi wfsService, FeatureProvider2 featureProvider, String level,
                                              String row, String col, String tileMatrixSetId, boolean doNotCache,
                                              VectorTilesCache cache, OgcApiRequestContext wfs3Request,
                                              CrsTransformerFactory crsTransformerFactory, UriInfo uriInfo, boolean invalid,
                                              OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson, I18n i18n,
                                              VectorTileMapGenerator vectorTileMapGenerator, Map<String, String> filters)
            throws FileNotFoundException {

        for (String collectionId : collectionIds) {
            // include only the requested layers / collections

            if (requestedCollections != null && !requestedCollections.contains(collectionId))
                continue;
            if (VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(wfsService.getData()), collectionId, "application/vnd.mapbox-vector-tile", true)) {
                VectorTile.checkZoomLevel(Integer.parseInt(level), vectorTileMapGenerator.getMinMaxMap(wfsService.getData(), false), wfsService, wfs3OutputFormatGeoJson, collectionId, tileMatrixSetId, "application/vnd.mapbox-vector-tile", row, col, doNotCache, cache, false, wfs3Request, crsTransformerFactory, i18n);

                Map<String, File> layerCollection = new HashMap<String, File>();

                VectorTile tileCollection = new VectorTile(collectionId, tileMatrixSetId, level, row, col, wfsService, doNotCache, cache, featureProvider, wfs3OutputFormatGeoJson);

                File tileFileJson = tileCollection.getFile(cache, "json");
                layers.put(collectionId, tileFileJson);

                File tileFileMvtCollection = tileCollection.getFile(cache, "pbf");
                if (!tileFileMvtCollection.exists() || invalid) {
                    if (invalid)
                        tileFileMvtCollection.delete();

                    Map<String, String> filterableFields = wfsService.getData()
                                                                     .getCollections()
                                                                     .get(collectionId)
                                                                     .getExtension(OgcApiFeaturesCoreConfiguration.class)
                                                                     .map(OgcApiFeaturesCoreConfiguration::getAllFilterParameters)
                                                                     .orElse(ImmutableMap.of());

                    if (!tileFileJson.exists()) {
                        OgcApiMediaType geojsonMediaType;
                        geojsonMediaType = new ImmutableOgcApiMediaType.Builder()
                                .type(new MediaType("application", "geo+json"))
                                .label("GeoJSON")
                                .build();
                        boolean success = TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), geojsonMediaType, false, tileCollection, i18n, wfs3Request.getLanguage(), queryParser);
                        if (!success) {
                            String msg = "Internal server error: could not generate GeoJSON for a tile.";
                            LOGGER.error(msg);
                            throw new InternalServerErrorException(msg);
                        }
                    } else {
                        if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformerFactory, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tileCollection, i18n, wfs3Request.getLanguage(), queryParser);
                        }
                    }
                    layerCollection.put(collectionId, tileFileJson);

                boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvtCollection, layerCollection, requestedProperties, crsTransformerFactory, tile, false);
                if (!success) {
                    String msg = "Internal server error: could not generate protocol buffers for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                    }
                }
            }
        }
        boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformerFactory, tile, false);
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

    public static Map<String, MinMax> getTileMatrixSetZoomLevels(OgcApiApiDataV2 data) {
        return data.getExtensions()
                .stream()
                .filter(extensionConfiguration -> extensionConfiguration instanceof TilesConfiguration)
                .map(tilesConfiguration -> ((TilesConfiguration) tilesConfiguration).getZoomLevels())
                .findAny()
                .orElse(null);
    }

    public static void checkTileMatrixSet(OgcApiApiDataV2 data, String tileMatrixSetId) {
        Set<String> tileMatrixSets = getTileMatrixSetZoomLevels(data).keySet();
        if (!tileMatrixSets.contains(tileMatrixSetId)) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }
    }

    private void checkTileValidity(String tileMatrixSetId, int tileMatrix, int tileRow, int tileCol, OgcApiApiDataV2 data) {
        // tileMatrixSetId and tileMatrix have been checked already
        TileMatrixSet tileMatrixSet = null;
        try {
            tileMatrixSet = TileMatrixSetCache.getTileMatrixSet(tileMatrixSetId);
        }
        catch (InternalServerErrorException e) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }

        try {
            BoundingBox bbox = data.getSpatialExtent(crsTransformerFactory, tileMatrixSet.getCrs());
            TileMatrixSetLimits limits = tileMatrixSet.getLimits(tileMatrix, bbox);
            if (tileRow > limits.getMaxTileRow() || tileRow < limits.getMinTileRow() || tileCol > limits.getMaxTileCol() || tileCol < limits.getMinTileCol())
                throw new NotFoundException("Tile outside of the area of the dataset");
        } catch (CrsTransformationException e) {
            String msg = String.format("Internal coordinate transformation error while checking the validity of requested tile %s/%d/%d/%d", tileMatrixSetId, tileMatrix, tileRow, tileCol);
            LOGGER.error(msg);
            throw new InternalServerErrorException(msg);
        }
    }
}
