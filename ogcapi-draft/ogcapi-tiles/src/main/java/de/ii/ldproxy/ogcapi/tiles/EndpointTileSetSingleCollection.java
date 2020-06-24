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
import java.util.Optional;
import java.util.Set;

/**
 * Handle responses under '/collections/{collectionId}/tiles/{tileMatrixSetId}'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileSetSingleCollection extends OgcApiEndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileSetSingleCollection.class);

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("collections")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/[\\w\\-]+/tiles/\\w+/?$")
            .build();
    private static final List<String> TAGS = ImmutableList.of("Access single-layer tiles");

    private final TilesQueriesHandler queryHandler;

    EndpointTileSetSingleCollection(@Requires OgcApiExtensionRegistry extensionRegistry,
                                    @Requires TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public OgcApiContext getApiContext() {
        return API_CONTEXT;
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
            formats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class);
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
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_TILE_SET_COLLECTION);
            final String subSubPath = "/tiles/{tileMatrixSetId}";
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
                    String operationSummary = "retrieve information about a tile set";
                    Optional<String> operationDescription = Optional.of("This operation fetches information about a tile set.");
                    String resourcePath = path.replace("{collectionId}", collectionId);
                    ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                            .path(resourcePath)
                            .pathParameters(pathParameters);
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
     * retrieve tilejson for the MVT tile sets
     *
     * @return a tilejson file
     */
    @Path("/{collectionId}/tiles/{tileMatrixSetId}")
    @GET
    public Response getTileSet(@Context OgcApiApi api,
                                       @Context OgcApiRequestContext requestContext,
                                       @PathParam("collectionId") String collectionId,
                                       @PathParam("tileMatrixSetId") String tileMatrixSetId) {

        OgcApiApiDataV2 apiData = api.getData();
        String path = "/collections/{collectionId}/tiles/{tileMatrixSetId}";
        checkPathParameter(extensionRegistry, apiData, path, "collectionId", collectionId);
        checkPathParameter(extensionRegistry, apiData, path, "tileMatrixSetId", tileMatrixSetId);

        FeatureTypeConfigurationOgcApi featureType = requestContext.getApi().getData().getCollections().get(collectionId);
        TilesConfiguration tilesConfiguration = getExtensionConfiguration(apiData, featureType, TilesConfiguration.class).get();

        TilesQueriesHandler.OgcApiQueryInputTileSet queryInput = new ImmutableOgcApiQueryInputTileSet.Builder()
                .from(getGenericQueryInput(api.getData()))
                .collectionId(collectionId)
                .tileMatrixSetId(tileMatrixSetId)
                .center(tilesConfiguration.getCenter())
                .zoomLevels(tilesConfiguration.getZoomLevels().get(tileMatrixSetId))
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
    }
}
