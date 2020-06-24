/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableList;
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
 * Handle responses under '/tiles/{tileMatrixSetId}'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileSetMultiCollection extends OgcApiEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileSetMultiCollection.class);

    private static final OgcApiContext API_CONTEXT = new ImmutableOgcApiContext.Builder()
            .apiEntrypoint("tiles")
            .addMethods(OgcApiContext.HttpMethods.GET, OgcApiContext.HttpMethods.HEAD)
            .subPathPattern("^/\\w+/?$")
            .build();
    private static final List<String> TAGS = ImmutableList.of("Access multi-layer tiles");

    private final TilesQueriesHandler queryHandler;

    EndpointTileSetMultiCollection(@Requires OgcApiExtensionRegistry extensionRegistry,
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
        Optional<TilesConfiguration> extension = getExtensionConfiguration(apiData, TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::getEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .isPresent();
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
                    .apiEntrypoint("tiles")
                    .sortPriority(OgcApiEndpointDefinition.SORT_PRIORITY_TILE_SET);
            String path = "/tiles/{tileMatrixSetId}";
            OgcApiContext.HttpMethods method = OgcApiContext.HttpMethods.GET;
            Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            String operationSummary = "retrieve information about a tile set";
            Optional<String> operationDescription = Optional.of("This operation fetches information about a tile set.");
            ImmutableOgcApiResourceAuxiliary.Builder resourceBuilderSet = new ImmutableOgcApiResourceAuxiliary.Builder()
                    .path(path)
                    .pathParameters(pathParameters);
            OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilderSet.putOperations(method.name(), operation);
            definitionBuilder.putResources(path, resourceBuilderSet.build());

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    /**
     * retrieve tilejson for the MVT tile sets
     *
     * @return a tilejson file
     */
    @Path("/{tileMatrixSetId}")
    @GET
    public Response getTileSet(@Context OgcApiApi api,
                                       @Context OgcApiRequestContext requestContext,
                                       @PathParam("tileMatrixSetId") String tileMatrixSetId) {

        OgcApiApiDataV2 apiData = api.getData();
        String path = "/tiles/{tileMatrixSetId}";
        checkPathParameter(extensionRegistry, apiData, path, "tileMatrixSetId", tileMatrixSetId);

        TilesConfiguration tilesConfiguration = getExtensionConfiguration(apiData, TilesConfiguration.class).get();

        TilesQueriesHandler.OgcApiQueryInputTileSet queryInput = new ImmutableOgcApiQueryInputTileSet.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tileMatrixSetId(tileMatrixSetId)
                .center(tilesConfiguration.getCenter())
                .zoomLevels(tilesConfiguration.getZoomLevels().get(tileMatrixSetId))
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
    }
}
