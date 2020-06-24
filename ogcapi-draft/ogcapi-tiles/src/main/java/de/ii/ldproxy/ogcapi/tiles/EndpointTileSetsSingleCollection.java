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
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.PathParameterTileMatrixSetId.TMS_REGEX;

/**
 * Handle responses under '/collection/{collectionId}/tiles'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileSetsSingleCollection extends OgcApiEndpointSubCollection implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileSetsSingleCollection.class);

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .subPathPattern("^/?[[\\w\\-]\\-]+/tiles(?:/"+TMS_REGEX+"/\\w+/\\w+/\\w+)?$")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .build();

    private static final List<String> TAGS = ImmutableList.of("Access single-layer tiles");

    private final CrsTransformerFactory crsTransformerFactory;
    private final TilesQueriesHandler queryHandler;

    EndpointTileSetsSingleCollection(@Requires CrsTransformerFactory crsTransformerFactory,
                                     @Requires OgcApiExtensionRegistry extensionRegistry,
                                     @Requires TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.crsTransformerFactory = crsTransformerFactory;
        this.queryHandler = queryHandler;
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/core");
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), TilesConfiguration.class);
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileSetsFormatExtension.class);
        return formats;
    }

    @Override
    public OgcApiEndpointDefinition getDefinition(OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableOgcApiEndpointDefinition.Builder definitionBuilder = new ImmutableOgcApiEndpointDefinition.Builder()
                    .apiEntrypoint("collections")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_TILE_SETS_COLLECTION);
            final String subSubPath = "/tiles";
            final String path = "/collections/{collectionId}" + subSubPath;
            final OgcApiContext.HttpMethods method = OgcApiContext.HttpMethods.GET;
            final Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
            if (!optCollectionIdParam.isPresent()) {
                LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                final  OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
                boolean explode = collectionIdParam.getExplodeInOpenApi();
                final Set<String> collectionIds = (explode) ?
                        collectionIdParam.getValues(apiData) :
                        ImmutableSet.of("{collectionId}");
                for (String collectionId : collectionIds) {
                    if (explode)
                        queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                    String operationSummary = "retrieve a list of the available tile sets";
                    Optional<String> operationDescription = Optional.of("This operation fetches the list of tile sets available for this collection.");
                    String resourcePath = path.replace("{collectionId}",collectionId);
                    ImmutableOgcApiResourceSet.Builder resourceBuilder = new ImmutableOgcApiResourceSet.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters)
                            .subResourceType("Tile Set");
                    OgcApiOperation operation = addOperation(apiData, OgcApiContext.HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                    if (operation != null)
                        resourceBuilder.putOperations(method.name(), operation);
                    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
                }
            }

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    /**
     * retrieve all available tile matrix sets from the collection
     *
     * @return all tile matrix sets from the collection in a json array
     */
    @Path("/{collectionId}/tiles")
    @GET
    public Response getTileSets(@Context OgcApiApi api, @Context OgcApiRequestContext requestContext,
                                @PathParam("collectionId") String collectionId) {

        OgcApiApiDataV2 apiData = api.getData();
        checkPathParameter(extensionRegistry, apiData, "/collections/{collectionId}/tiles", "collectionId", collectionId);

        TilesQueriesHandler.OgcApiQueryInputTileSets queryInput = new ImmutableOgcApiQueryInputTileSets.Builder()
                .from(getGenericQueryInput(api.getData()))
                .collectionId(collectionId)
                .center(getCenter(apiData))
                .tileMatrixSetZoomLevels(getTileMatrixSetZoomLevels(apiData, collectionId))
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SETS, queryInput, requestContext);
    }

    private double[] getCenter(OgcApiApiDataV2 data) {
        TilesConfiguration tilesConfiguration = getExtensionConfiguration(data, TilesConfiguration.class).get();
        return tilesConfiguration.getCenter();
    }

    private Map<String, MinMax> getTileMatrixSetZoomLevels(OgcApiApiDataV2 data, String collectionId) {
        TilesConfiguration tilesConfiguration = getExtensionConfiguration(data, data.getCollections().get(collectionId), TilesConfiguration.class).get();
        return tilesConfiguration.getZoomLevels();
    }
}
