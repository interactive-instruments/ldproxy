/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ldproxy.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ldproxy.ogcapi.tiles.api.AbstractEndpointTileSetMultiCollection;
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
import java.util.List;
import java.util.Optional;

/**
 * Handle responses under '/map/tiles/{tileMatrixSetId}'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTileSetMultiCollection extends AbstractEndpointTileSetMultiCollection {

    private static final List<String> TAGS = ImmutableList.of("Access multi-layer map tiles");

    EndpointMapTileSetMultiCollection(@Requires ExtensionRegistry extensionRegistry,
                                      @Requires TilesQueriesHandler queryHandler,
                                      @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry, queryHandler, providers);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        if (apiData.getExtension(MapTilesConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(MapTilesConfiguration::isMultiCollectionEnabled)
            .isPresent())
            return super.isEnabledForApi(apiData);
        return false;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        return computeDefinition(apiData,
                                 "map",
                                 ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE_SET,
                                 "/map/tiles/{tileMatrixSetId}",
                                 TAGS);
    }

    /**
     * retrieve tilejson for the MVT tile sets
     *
     * @return a tilejson file
     */
    @Path("/tiles/{tileMatrixSetId}")
    @GET
    public Response getTileSet(@Context OgcApi api,
                               @Context ApiRequestContext requestContext,
                               @PathParam("tileMatrixSetId") String tileMatrixSetId) {

        return super.getTileSet(api.getData(), requestContext, "/map/tiles/{tileMatrixSetId}", tileMatrixSetId);
    }
}
