/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.api;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileSet.Builder;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;

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
        ApiOperation.getResource(apiData, path, false, queryParameters, ImmutableList.of(),
                                 getContent(apiData, path), operationSummary, operationDescription, Optional.empty(), tags
            )
            .ifPresent(operation -> resourceBuilderSet.putOperations(method.name(), operation));
        definitionBuilder.putResources(path, resourceBuilderSet.build());

        return definitionBuilder.build();
    }

    protected Response getTileSet(OgcApiDataV2 apiData, ApiRequestContext requestContext, String definitionPath, String tileMatrixSetId) {

        checkPathParameter(extensionRegistry, apiData, definitionPath, "tileMatrixSetId", tileMatrixSetId);
        TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileSet queryInput = new Builder()
            .from(getGenericQueryInput(apiData))
            .tileMatrixSetId(tileMatrixSetId)
            .center(tilesConfiguration.getCenterDerived())
            .zoomLevels(tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId))
            .path(definitionPath)
            .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
    }

}
