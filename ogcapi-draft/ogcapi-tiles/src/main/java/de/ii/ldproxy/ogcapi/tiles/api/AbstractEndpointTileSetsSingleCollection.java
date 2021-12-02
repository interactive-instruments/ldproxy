/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.api;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiResourceSet;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractEndpointTileSetsSingleCollection extends EndpointSubCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEndpointTileSetsSingleCollection.class);

    private final TilesQueriesHandler queryHandler;
    private final FeaturesCoreProviders providers;

    public AbstractEndpointTileSetsSingleCollection(ExtensionRegistry extensionRegistry,
                                                    TilesQueriesHandler queryHandler,
                                                    FeaturesCoreProviders providers) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.providers = providers;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        Optional<TilesConfiguration> config = apiData.getCollections()
            .get(collectionId)
            .getExtension(TilesConfiguration.class);
        if (config.filter(TilesConfiguration::isEnabled)
            .isEmpty())
            return false;
        if (config.map(cfg -> !cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
            // Tiles are pre-generated as a static tile set
            return config.filter(ExtensionConfiguration::isEnabled)
                .isPresent();
        } else {
            if (config.filter(TilesConfiguration::isSingleCollectionEnabled)
                .isEmpty())
                return false;
            // Tiles are generated on-demand from a data source;
            // currently no vector tiles support for WFS backends
            return providers.getFeatureProvider(apiData)
                .map(FeatureProvider2::supportsHighLoad)
                .orElse(false);
        }
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileSetsFormatExtension.class);
        return formats;
    }

    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData,
                                                      String apiEntrypoint,
                                                      int sortPriority,
                                                      String basePath,
                                                      String subSubPath,
                                                      List<String> tags) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint(apiEntrypoint)
            .sortPriority(sortPriority);
        final String path = basePath + subSubPath;
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
                String operationSummary = "retrieve a list of the available tile sets";
                Optional<String> operationDescription = Optional.of("This operation fetches the list of tile sets available for this collection.");
                String resourcePath = path.replace("{collectionId}",collectionId);
                ImmutableOgcApiResourceSet.Builder resourceBuilder = new ImmutableOgcApiResourceSet.Builder()
                    .path(resourcePath)
                    .pathParameters(pathParameters)
                    .subResourceType("Tile Set");
                ApiOperation operation = addOperation(apiData, HttpMethods.GET, queryParameters, collectionId, subSubPath, operationSummary, operationDescription, tags);
                if (operation != null)
                    resourceBuilder.putOperations(method.name(), operation);
                definitionBuilder.putResources(resourcePath, resourceBuilder.build());
            }
        }

        return definitionBuilder.build();
    }

    protected Response getTileSets(OgcApiDataV2 apiData, ApiRequestContext requestContext,
                                   String definitionPath, String collectionId, boolean onlyWebMercatorQuad) {

        checkPathParameter(extensionRegistry, apiData, "/collections/{collectionId}/tiles", "collectionId", collectionId);

        TilesQueriesHandler.QueryInputTileSets queryInput = new ImmutableQueryInputTileSets.Builder()
            .from(getGenericQueryInput(apiData))
            .collectionId(collectionId)
            .center(getCenter(apiData))
            .tileMatrixSetZoomLevels(getTileMatrixSetZoomLevels(apiData, collectionId))
            .path(definitionPath)
            .onlyWebMercatorQuad(onlyWebMercatorQuad)
            .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SETS, queryInput, requestContext);
    }

    private List<Double> getCenter(OgcApiDataV2 data) {
        TilesConfiguration tilesConfiguration = data.getExtension(TilesConfiguration.class).get();
        return tilesConfiguration.getCenterDerived();
    }

    private Map<String, MinMax> getTileMatrixSetZoomLevels(OgcApiDataV2 data, String collectionId) {
        TilesConfiguration tilesConfiguration = data.getCollections().get(collectionId).getExtension(TilesConfiguration.class).get();
        return tilesConfiguration.getZoomLevelsDerived();
    }
}
