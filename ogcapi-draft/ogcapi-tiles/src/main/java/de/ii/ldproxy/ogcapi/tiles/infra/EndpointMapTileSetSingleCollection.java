/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * Handle responses under '/collections/{collectionId}/map/tiles/{tileMatrixSetId}'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTileSetSingleCollection extends AbstractEndpointTileSetSingleCollection {

    private final FeaturesCoreProviders providers;

    EndpointMapTileSetSingleCollection(@Requires ExtensionRegistry extensionRegistry,
                                       @Requires TilesQueriesHandler queryHandler,
                                       @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry, queryHandler);
        this.providers = providers;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        Optional<TilesConfiguration> config = apiData.getCollections()
                                                        .get(collectionId)
                                                        .getExtension(TilesConfiguration.class);
        if (config.map(cfg -> !cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
            // Tiles are pre-generated as a static tile set
            return config.filter(ExtensionConfiguration::isEnabled)
                         .isPresent();
        } else {
            // Tiles are generated on-demand from a data source
            if (config.filter(TilesConfiguration::isEnabled)
                      .filter(TilesConfiguration::isSingleCollectionEnabled)
                      .isEmpty()) return false;
            // currently no vector tiles support for WFS backends
            return providers.getFeatureProvider(apiData)
                            .map(FeatureProvider2::supportsHighLoad)
                            .orElse(false);
        }
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        return null;
    }

    /**
     * retrieve tilejson for the MVT tile sets
     *
     * @return a tilejson file
     */
    @Path("/{collectionId}/map/tiles/{tileMatrixSetId}")
    @GET
    public Response getTileSet(@Context OgcApi api,
                                       @Context ApiRequestContext requestContext,
                                       @PathParam("collectionId") String collectionId,
                                       @PathParam("tileMatrixSetId") String tileMatrixSetId) {

        return super.getTileSet(api, requestContext, collectionId, tileMatrixSetId);
    }
}
