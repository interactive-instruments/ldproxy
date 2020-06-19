/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
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
import javax.ws.rs.core.Response;
import java.util.*;

import static de.ii.xtraplatform.runtime.FelixRuntime.DATA_DIR_KEY;

/**
 * fetch tiling schemes / tile matrix sets that have been configured for an API
 */
@Component
@Provides
@Instantiate
public class EndpointTileMatrixSets extends OgcApiEndpoint implements ConformanceClass {

    @Requires
    I18n i18n;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileMatrixSets.class);
    private static final List<String> TAGS = ImmutableList.of("Discover and fetch tiling schemes");

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("tileMatrixSets")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/?(?:\\w+)?$")
            .build();

    private final VectorTilesCache cache;
    private final VectorTileMapGenerator vectorTileMapGenerator = new VectorTileMapGenerator();
    private final TilesQueriesHandler queryHandler;

    EndpointTileMatrixSets(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                           @Requires OgcApiExtensionRegistry extensionRegistry,
                           @Requires TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        String dataDirectory = bundleContext.getProperty(DATA_DIR_KEY);
        cache = new VectorTilesCache(dataDirectory);
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    /*
    @Override
    public ImmutableSet<OgcApiMediaType> getMediaTypes(OgcApiApiDataV2 apiData, String subPath) {
        return ImmutableSet.of(
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build(),
                new ImmutableOgcApiMediaType.Builder()
                        .type(MediaType.TEXT_HTML_TYPE)
                        .build());
    }
     */

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tmxs");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileMatrixSetsFormatExtension.class);
        return formats;
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("tileMatrixSets")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_TILE_MATRIX_SETS);
            String path = "/tileMatrixSets";
            Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            String operationSummary = "lists the available tiling schemes";
            Optional<String> operationDescription = Optional.of("This operation fetches the set of tiling schemes supported by this API. " +
                    "For each tiling scheme the id, a title and the link to the tiling scheme object is provided.");
            ImmutableOgcApiResourceSet.Builder resourceBuilderSet = new ImmutableOgcApiResourceSet.Builder()
                    .path(path)
                    .subResourceType("TileMatrixSet");
            OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilderSet.putOperations("GET", operation);
            definitionBuilder.putResources(path, resourceBuilderSet.build());

            path = "/tileMatrixSets/{tileMatrixSetId}";
            queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            if (!pathParameters.stream().filter(param -> param.getName().equals("tileMatrixSetId")).findAny().isPresent()) {
                LOGGER.error("Path parameter 'tileMatrixSetId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                operationSummary = "fetch information about the tiling scheme `{tileMatrixSetId}`";
                operationDescription = Optional.of("The definition of the tiling scheme according to the [OGC Two Dimensional Tile Matrix Set standard](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html).");
                ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                        .path(path)
                        .pathParameters(pathParameters);
                operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
                if (operation!=null)
                    resourceBuilder.putOperations("GET", operation);
                definitionBuilder.putResources(path, resourceBuilder.build());
            }

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    /**
     * retrieve all available tile matrix sets
     *
     * @return all tile matrix sets in a json array or an HTML view
     */
    @Path("")
    @GET
    public Response getTileMatrixSets(@Context OgcApiApi api, @Context OgcApiRequestContext requestContext) {

        checkTilesParameterDataset(getEnabledMap(api.getData()));

        TilesQueriesHandler.OgcApiQueryInputTileMatrixSets queryInput = new ImmutableOgcApiQueryInputTileMatrixSets.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tileMatrixSets(extensionRegistry.getExtensionsForType(TileMatrixSet.class))
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_MATRIX_SETS, queryInput, requestContext);
    }

    /**
     * retrieve one specific tile matrix set by id
     *
     * @param tileMatrixSetId   the local identifier of a specific tile matrix set
     * @return the tiling scheme in a json file
     */
    @Path("/{tileMatrixSetId}")
    @GET
    public Response getTileMatrixSet(@PathParam("tileMatrixSetId") String tileMatrixSetId,
                                     @Context OgcApiApi api,
                                     @Context OgcApiRequestContext requestContext) {

        checkTilesParameterDataset(getEnabledMap(api.getData()));
        checkPathParameter(extensionRegistry, api.getData(), "/tileMatrixSets/{tileMatrixSetId}", "tileMatrixSetId", tileMatrixSetId);

        TilesQueriesHandler.OgcApiQueryInputTileMatrixSet queryInput = new ImmutableOgcApiQueryInputTileMatrixSet.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tileMatrixSetId(tileMatrixSetId)
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_MATRIX_SET, queryInput, requestContext);
    }

    /**
     * checks if the tiles parameter is enabled in the dataset. If the tiles parameter is disabled in all collections, it throws a 404.
     *
     * @param enabledMap    a map with all collections and the boolean if the tiles support is enabled or not
     */
    private void checkTilesParameterDataset(Map<String, Boolean> enabledMap) {

        if (!Objects.isNull(enabledMap)) {
            for (String collectionId : enabledMap.keySet()) {
                if (enabledMap.get(collectionId))
                    return;
            }
        }
        throw new NotFoundException();
    }

    private Map<String, MinMax> getTileMatrixSetZoomLevels(OgcApiApiDataV2 data) {
        TilesConfiguration tilesConfiguration = getExtensionConfiguration(data, TilesConfiguration.class).get();
        return tilesConfiguration.getZoomLevels();
    }

    private void checkTileMatrixSet(OgcApiApiDataV2 data, String tileMatrixSetId) {
        Set<String> tileMatrixSets = getTileMatrixSetZoomLevels(data).keySet();
        if (!tileMatrixSets.contains(tileMatrixSetId)) {
            throw new NotFoundException("Unknown tile matrix set: " + tileMatrixSetId);
        }
    }

    /**
     * checks if the tiles extension is available and returns a Map with all available collections and a boolean value if the tiles
     * support is currently enabled
     * @param datasetData       the service data of the Wfs3 Service
     * @return a map with all CollectionIds, which have the tiles Extension and the value of the tiles Parameter  "enabled"
     */
    private Map<String, Boolean> getEnabledMap(OgcApiApiDataV2 datasetData) {
        Map<String, Boolean> enabledMap = new HashMap<>();
        for (String collectionId : datasetData.getCollections()
                .keySet()) {
            if (isExtensionEnabled(datasetData, datasetData.getCollections()
                    .get(collectionId), TilesConfiguration.class)) {
                final TilesConfiguration tilesConfiguration = getExtensionConfiguration(datasetData, datasetData.getCollections()
                        .get(collectionId), TilesConfiguration.class).get();

                boolean tilesEnabled = tilesConfiguration.getEnabled();

                enabledMap.put(collectionId, tilesEnabled);
            }
        }
        return enabledMap;
    }
}
