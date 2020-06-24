/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.tiles.*;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

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

    private final TileMatrixSetsQueriesHandler queryHandler;

    EndpointTileMatrixSets(@org.apache.felix.ipojo.annotations.Context BundleContext bundleContext,
                           @Requires OgcApiExtensionRegistry extensionRegistry,
                           @Requires TileMatrixSetsQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

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
            OgcApiContext.HttpMethods method = OgcApiContext.HttpMethods.GET;
            Set<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            String operationSummary = "retrieve a list of the available tiling schemes";
            Optional<String> operationDescription = Optional.of("This operation fetches the set of tiling schemes supported by this API. " +
                    "For each tiling scheme the id, a title and the link to the tiling scheme object is provided.");
            ImmutableOgcApiResourceSet.Builder resourceBuilderSet = new ImmutableOgcApiResourceSet.Builder()
                    .path(path)
                    .subResourceType("Tile Matrix Set");
            OgcApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilderSet.putOperations(method.name(), operation);
            definitionBuilder.putResources(path, resourceBuilderSet.build());

            path = "/tileMatrixSets/{tileMatrixSetId}";
            queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            Set<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
            if (!pathParameters.stream().filter(param -> param.getName().equals("tileMatrixSetId")).findAny().isPresent()) {
                LOGGER.error("Path parameter 'tileMatrixSetId' missing for resource at path '" + path + "'. The GET method will not be available.");
            } else {
                operationSummary = "fetch information about the tiling scheme `{tileMatrixSetId}`";
                operationDescription = Optional.of("Returns the definition of the tiling scheme according to the [OGC Two Dimensional Tile Matrix Set standard](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html).");
                ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                        .path(path)
                        .pathParameters(pathParameters);
                operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
                if (operation!=null)
                    resourceBuilder.putOperations(method.name(), operation);
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

        if (!isEnabledForApi(api.getData()))
            throw new NotFoundException();

        TileMatrixSetsQueriesHandler.OgcApiQueryInputTileMatrixSets queryInput = new ImmutableOgcApiQueryInputTileMatrixSets.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tileMatrixSets(extensionRegistry.getExtensionsForType(TileMatrixSet.class))
                .build();

        return queryHandler.handle(TileMatrixSetsQueriesHandler.Query.TILE_MATRIX_SETS, queryInput, requestContext);
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

        checkPathParameter(extensionRegistry, api.getData(), "/tileMatrixSets/{tileMatrixSetId}", "tileMatrixSetId", tileMatrixSetId);

        TileMatrixSetsQueriesHandler.OgcApiQueryInputTileMatrixSet queryInput = new ImmutableOgcApiQueryInputTileMatrixSet.Builder()
                .from(getGenericQueryInput(api.getData()))
                .tileMatrixSetId(tileMatrixSetId)
                .build();

        return queryHandler.handle(TileMatrixSetsQueriesHandler.Query.TILE_MATRIX_SET, queryInput, requestContext);
    }
}
