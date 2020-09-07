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
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * Handle responses under '/tiles'.
 */
@Component
@Provides
@Instantiate
public class EndpointTileSetsMultiCollection extends Endpoint implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileSetsMultiCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access multi-layer tiles");

    private final TilesQueriesHandler queryHandler;
    private final FeaturesCoreProviders providers;

    EndpointTileSetsMultiCollection(@Requires ExtensionRegistry extensionRegistry,
                                    @Requires TilesQueriesHandler queryHandler,
                                    @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
        this.providers = providers;
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/dataset-tilesets",
                                "http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/geodata-selection");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        // currently no vector tiles support for WFS backends
        if (providers.getFeatureProvider(apiData).getData().getFeatureProviderType().equals("WFS"))
            return false;

        Optional<TilesConfiguration> extension = apiData.getExtension(TilesConfiguration.class);

        return extension
                .filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::getMultiCollectionEnabled)
                .isPresent();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileSetsFormatExtension.class);
        return formats;
    }

    @Override
    public ApiEndpointDefinition getDefinition(OgcApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return super.getDefinition(apiData);

        String apiId = apiData.getId();
        if (!apiDefinitions.containsKey(apiId)) {
            ImmutableApiEndpointDefinition.Builder definitionBuilder = new ImmutableApiEndpointDefinition.Builder()
                    .apiEntrypoint("tiles")
                    .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_TILE_SETS);
            String path = "/tiles";
            HttpMethods method = HttpMethods.GET;
            List<OgcApiQueryParameter> queryParameters = getQueryParameters(extensionRegistry, apiData, path);
            String operationSummary = "retrieve a list of the available tile sets";
            Optional<String> operationDescription = Optional.of("This operation fetches the list of multi-layer tile sets supported by this API.");
            ImmutableOgcApiResourceSet.Builder resourceBuilderSet = new ImmutableOgcApiResourceSet.Builder()
                    .path(path)
                    .subResourceType("Tile Set");
            ApiOperation operation = addOperation(apiData, queryParameters, path, operationSummary, operationDescription, TAGS);
            if (operation!=null)
                resourceBuilderSet.putOperations(method.name(), operation);
            definitionBuilder.putResources(path, resourceBuilderSet.build());

            apiDefinitions.put(apiId, definitionBuilder.build());
        }

        return apiDefinitions.get(apiId);
    }

    @Path("")
    @GET
    public Response getTileSets(@Context OgcApi api, @Context ApiRequestContext requestContext) {

        OgcApiDataV2 apiData = api.getData();
        if (!isEnabledForApi(apiData))
            throw new NotFoundException("Multi-collection tiles are not available in this API.");

        TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileSets queryInput = new ImmutableQueryInputTileSets.Builder()
                .from(getGenericQueryInput(api.getData()))
                .center(tilesConfiguration.getCenter())
                .tileMatrixSetZoomLevels(tilesConfiguration.getZoomLevels())
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SETS, queryInput, requestContext);
    }
}
