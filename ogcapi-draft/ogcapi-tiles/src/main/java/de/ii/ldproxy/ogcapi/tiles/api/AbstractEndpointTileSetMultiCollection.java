/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.api;

import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
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
import de.ii.xtraplatform.features.domain.FeatureProvider2;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

public abstract class AbstractEndpointTileSetMultiCollection extends Endpoint {

    private final TilesQueriesHandler queryHandler;
    private final FeaturesCoreProviders providers;

    public AbstractEndpointTileSetMultiCollection(ExtensionRegistry extensionRegistry,
                                                  TilesQueriesHandler queryHandler,
                                                  FeaturesCoreProviders providers) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.providers = providers;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats == null) {
            formats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class);
        }
        return formats;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
        if (config.map(cfg -> !cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
            // Tiles are pre-generated as a static tile set
            return config.filter(ExtensionConfiguration::isEnabled)
                .isPresent();
        } else {
            // Tiles are generated on-demand from a data source
            if (config.filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::isMultiCollectionEnabled)
                .isEmpty()) return false;
            // currently no vector tiles support for WFS backends
            return providers.getFeatureProvider(apiData)
                .map(FeatureProvider2::supportsHighLoad)
                .orElse(false);
        }
    }

    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData,
                                                      String apiEntrypoint,
                                                      int sortPriority,
                                                      String path,
                                                      List<String> tags) {
        ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint(apiEntrypoint)
            .sortPriority(sortPriority);
        HttpMethods method = HttpMethods.GET;
        List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
        List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
        String operationSummary = "retrieve information about a tile set";
        Optional<String> operationDescription = Optional.of("This operation fetches information about a tile set.");
        ImmutableOgcApiResourceAuxiliary.Builder resourceBuilderSet = new ImmutableOgcApiResourceAuxiliary.Builder()
            .path(path)
            .pathParameters(pathParameters);
        ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, tags);
        if (operation!=null)
            resourceBuilderSet.putOperations(method.name(), operation);
        definitionBuilder.putResources(path, resourceBuilderSet.build());

        return definitionBuilder.build();
    }

    protected Response getTileSet(OgcApiDataV2 apiData, ApiRequestContext requestContext, String definitionPath, String tileMatrixSetId) {

        String path = "/tiles/{tileMatrixSetId}";
        checkPathParameter(extensionRegistry, apiData, path, "tileMatrixSetId", tileMatrixSetId);
        TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileSet queryInput = new ImmutableQueryInputTileSet.Builder()
            .from(getGenericQueryInput(apiData))
            .tileMatrixSetId(tileMatrixSetId)
            .center(tilesConfiguration.getCenterDerived())
            .zoomLevels(tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId))
            .path(definitionPath)
            .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
    }

}
