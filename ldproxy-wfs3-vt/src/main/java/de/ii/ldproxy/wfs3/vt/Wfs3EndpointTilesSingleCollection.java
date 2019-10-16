/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureFormatExtension;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesEndpoint;
import de.ii.ldproxy.target.geojson.OgcApiFeaturesOutputFormatGeoJson;
import de.ii.ldproxy.wfs3.filtertransformer.OgcApiParameterFilterTransformer;
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

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^/?[[\\w\\-]\\-]+/tiles(?:/\\w+(?:/\\w+/\\w+/\\w+)?)?/?$")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .build();

    @Requires
    I18n i18n;

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
        if (subPath.matches("^/?[\\w\\-]+/tiles(?:/\\w+)?/?$"))
            return ImmutableSet.of(
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.APPLICATION_JSON_TYPE)
                            .build(),
                    new ImmutableOgcApiMediaType.Builder()
                            .type(MediaType.TEXT_HTML_TYPE)
                            .build());
        else if (subPath.matches("^/?(?:[\\w\\-]+)/tiles/(?:\\w+/\\w+/\\w+/\\w+)$"))
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
        if (subPath.matches("^/?[\\w\\-]+/tiles(?:/\\w+)?/?$")) {
            return OgcApiEndpointExtension.super.getParameters(apiData, subPath);
        } else if (subPath.matches("^/?(?:[\\w\\-]+)/tiles/(?:\\w+/\\w+/\\w+/\\w+)$")) {
            ImmutableSet<String> parametersFromExtensions = new ImmutableSet.Builder<String>()
                    .addAll(wfs3ExtensionRegistry.getExtensionsForType(OgcApiParameterExtension.class)
                            .stream()
                            // TODO: this is a hack, we need a more flexible mechanism to determine where the parameters apply
                            .filter(ext -> ext.getClass()==OgcApiParameterFilterTransformer.class)
                            .map(ext -> ext.getParameters(apiData, subPath.split("/tiles/",2)[0] + "/items"))
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
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    /**
     * retrieve all available tile matrix sets from the collection
     *
     * @return all tile matrix sets from the collection in a json array
     */
    @Path("/{collectionId}/tiles")
    @GET
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getTileMatrixSets(@Context OgcApiDataset service, @Context OgcApiRequestContext requestContext,
                                      @PathParam("collectionId") String collectionId) {

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);

        final VectorTilesLinkGenerator vectorTilesLinkGenerator = new VectorTilesLinkGenerator();

        FeatureTypeConfigurationOgcApi featureTypeConfiguration = requestContext.getApi().getData().getFeatureTypes().get(collectionId);

        TileCollections tiles = ImmutableTileCollections.builder()
                .title(featureTypeConfiguration.getLabel())
                .description(featureTypeConfiguration.getDescription().orElse(""))
                .tileMatrixSetLinks(
                        cache.getTileMatrixSetIds()
                                .stream()
                                .map(tileMatrixSetId -> ImmutableTileCollection.builder()
                                        .tileMatrixSet(tileMatrixSetId)
                                        .build())
                                .collect(Collectors.toList()))
                .links(vectorTilesLinkGenerator.generateTilesLinks(
                        requestContext.getUriCustomizer(),
                        requestContext.getMediaType(),
                        requestContext.getAlternateMediaTypes(),
                        false, // TODO
                        true,
                        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/vnd.mapbox-vector-tile", true),
                        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/geo+json", true),
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


        VectorTile.checkZoomLevel(Integer.parseInt(tileMatrix), vectorTileMapGenerator.getMinMaxMap(service.getData(), false), service, wfs3OutputFormatGeoJson, collectionId, tileMatrixSetId, "application/vnd.mapbox-vector-tile", tileRow, tileCol, doNotCache, cache, true, wfs3Request, crsTransformation, i18n);


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


        VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);

        File tileFileMvt = tile.getFile(cache, "pbf");
        if (!tileFileMvt.exists()) {

            VectorTile jsonTile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);
            File tileFileJson = jsonTile.getFile(cache, "json");
            if (!tileFileJson.exists()) {
                OgcApiMediaType geojsonMediaType;
                geojsonMediaType = new ImmutableOgcApiMediaType.Builder()
                        .type(new MediaType("application", "geo+json"))
                        .label("GeoJSON")
                        .build();
                boolean success = TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), geojsonMediaType, true, jsonTile, i18n, wfs3Request.getLanguage());
                if (!success) {
                    String msg = "Internal server error: could not generate GeoJSON for a tile.";
                    LOGGER.error(msg);
                    throw new InternalServerErrorException(msg);
                }
            } else {
                if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                   TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage());
                }
            }

            generateTileCollection(collectionId, tileFileJson, tileFileMvt, tile, requestedProperties, crsTransformation);
        } else {
            VectorTile jsonTile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);
            File tileFileJson = jsonTile.getFile(cache, "json");

            if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage());
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
                                @Context OgcApiDataset service, @Context UriInfo uriInfo,
                                @Context OgcApiRequestContext wfs3Request) throws CrsTransformationException, FileNotFoundException {

        OgcApiFeatureFormatExtension wfs3OutputFormatGeoJson = getOutputFormatForType(OgcApiFeaturesOutputFormatGeoJson.MEDIA_TYPE)
        															.orElseThrow(NotAcceptableException::new);

        checkTilesParameterCollection(vectorTileMapGenerator.getEnabledMap(service.getData()), collectionId);
        VectorTile.checkFormat(vectorTileMapGenerator.getFormatsMap(service.getData()), collectionId, "application/geo+json", false);
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        final Map<String, String> filterableFields = service.getData()
                                                            .getFilterableFieldsForFeatureType(collectionId);
        final Map<String, String> filters = OgcApiFeaturesEndpoint.getFiltersFromQuery(OgcApiFeaturesEndpoint.toFlatMap(queryParameters), filterableFields);

        boolean doNotCache = false;
        if (!filters.isEmpty() || queryParameters.containsKey("properties"))
            doNotCache = true;

        VectorTile.checkZoomLevel(Integer.parseInt(tileMatrix), vectorTileMapGenerator.getMinMaxMap(service.getData(), false), service, wfs3OutputFormatGeoJson, collectionId, tileMatrixSetId, MediaType.APPLICATION_JSON, tileRow, tileCol, doNotCache, cache, true, wfs3Request, crsTransformation, i18n);

        LOGGER.debug("GET TILE GeoJSON {} {} {} {} {} {}", service.getId(), collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol);


        VectorTile tile = new VectorTile(collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, service, doNotCache, cache, service.getFeatureProvider(), wfs3OutputFormatGeoJson);

        File tileFileJson = tile.getFile(cache, "json");

        //TODO parse file (check if valid) if not valid delete it and generate new one

        if (!tileFileJson.exists()) {
            TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage());
        } else {
            if (TileGeneratorJson.deleteJSON(tileFileJson)) {
                TileGeneratorJson.generateTileJson(tileFileJson, crsTransformation, uriInfo, filters, filterableFields, wfs3Request.getUriCustomizer(), wfs3Request.getMediaType(), true, tile, i18n, wfs3Request.getLanguage());
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
        boolean success = TileGeneratorMvt.generateTileMvt(tileFileMvt, layers, requestedProperties, crsTransformation, tile, false);
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
