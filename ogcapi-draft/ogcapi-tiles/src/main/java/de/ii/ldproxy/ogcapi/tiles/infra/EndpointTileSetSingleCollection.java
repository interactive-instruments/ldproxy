/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
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

/**
 * Handle responses under '/collections/{collectionId}/tiles/{tileMatrixSetId}'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileSetSingleCollection extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileSetSingleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access single-layer tiles");

    private final TilesQueriesHandler queryHandler;
    private final FeaturesCoreProviders providers;

    EndpointTileSetSingleCollection(@Requires ExtensionRegistry extensionRegistry,
                                    @Requires TilesQueriesHandler queryHandler,
                                    @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.providers = providers;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class);
        return formats;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        Optional<TilesConfiguration> config = apiData.getCollections()
                                                        .get(collectionId)
                                                        .getExtension(TilesConfiguration.class);
        if (config.map(cfg -> cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
            // Tiles are pre-generated as a static tile set
            return config.map(ExtensionConfiguration::isEnabled).orElse(false);
        } else {
            // Tiles are generated on-demand from a data source
            if (config.filter(TilesConfiguration::isEnabled)
                      .filter(TilesConfiguration::getSingleCollectionEnabledDerived)
                      .isEmpty()) return false;
            // currently no vector tiles support for WFS backends
            return providers.getFeatureProvider(apiData).supportsHighLoad();
        }
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                .apiEntrypoint("collections")
                .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_TILE_SET_COLLECTION);
        final String subSubPath = "/tiles/{tileMatrixSetId}";
        final String path = "/collections/{collectionId}" + subSubPath;
        final HttpMethods method = HttpMethods.GET;
        final List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        final Optional<OgcApiPathParameter> optCollectionIdParam = pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
        if (optCollectionIdParam.isEmpty()) {
            LOGGER.error("Path parameter 'collectionId' missing for resource at path '" + path + "'. The GET method will not be available.");
        } else {
            final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
            boolean explode = collectionIdParam.getExplodeInOpenApi(apiData);
            final List<String> collectionIds = (explode) ?
                    collectionIdParam.getValues(apiData) :
                    ImmutableList.of("{collectionId}");
            for (String collectionId : collectionIds) {
                List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path, collectionId);
                String operationSummary = "retrieve information about a tile set";
                Optional<String> operationDescription = Optional.of("This operation fetches information about a tile set.");
                String resourcePath = path.replace("{collectionId}", collectionId);
                ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder = new ImmutableOgcApiResourceAuxiliary.Builder()
                        .path(resourcePath)
                        .pathParameters(pathParameters);
                ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, TAGS);
                if (operation != null)
                    resourceBuilder.putOperations(method.name(), operation);
                definitionBuilder.putResources(resourcePath, resourceBuilder.build());
            }
        }

        return definitionBuilder.build();
    }

    /**
     * retrieve tilejson for the MVT tile sets
     *
     * @return a tilejson file
     */
    @Path("/{collectionId}/tiles/{tileMatrixSetId}")
    @GET
    public Response getTileSet(@Context OgcApi api,
                                       @Context ApiRequestContext requestContext,
                                       @PathParam("collectionId") String collectionId,
                                       @PathParam("tileMatrixSetId") String tileMatrixSetId) {

        OgcApiDataV2 apiData = api.getData();
        String path = "/collections/{collectionId}/tiles/{tileMatrixSetId}";
        checkPathParameter(extensionRegistry, apiData, path, "collectionId", collectionId);
        checkPathParameter(extensionRegistry, apiData, path, "tileMatrixSetId", tileMatrixSetId);

        FeatureTypeConfigurationOgcApi featureType = requestContext.getApi().getData().getCollections().get(collectionId);
        TilesConfiguration tilesConfiguration = featureType.getExtension(TilesConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileSet queryInput = new ImmutableQueryInputTileSet.Builder()
                .from(getGenericQueryInput(api.getData()))
                .collectionId(collectionId)
                .tileMatrixSetId(tileMatrixSetId)
                .center(tilesConfiguration.getCenterDerived())
                .zoomLevels(tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId))
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
    }
}
