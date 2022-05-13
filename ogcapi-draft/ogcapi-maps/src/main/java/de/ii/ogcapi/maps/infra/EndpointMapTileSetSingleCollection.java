/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tiles.api.AbstractEndpointTileSetSingleCollection;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @langEn Access single-layer map tiles
 * @langDe TODO
 * @name Tileset
 * @path /{apiId}/collections/{collectionId}/map/tiles/{tileMatrixSetId}
 */

/**
 * Handle responses under '/collections/{collectionId}/map/tiles/{tileMatrixSetId}'.
 */
@Singleton
@AutoBind
public class EndpointMapTileSetSingleCollection extends AbstractEndpointTileSetSingleCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointMapTileSetSingleCollection.class);
    private static final List<String> TAGS = ImmutableList.of("Access single-layer map tiles");

    @Inject
    EndpointMapTileSetSingleCollection(ExtensionRegistry extensionRegistry,
                                       TilesQueriesHandler queryHandler,
                                       FeaturesCoreProviders providers) {
        super(extensionRegistry, queryHandler, providers);
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        if (apiData.getExtension(MapTilesConfiguration.class, collectionId)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(MapTilesConfiguration::isSingleCollectionEnabled)
            .isPresent())
            return super.isEnabledForApi(apiData, collectionId);
        return false;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        return computeDefinition(apiData,
                                 "collections",
                                 ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE_SET_COLLECTION,
                                 "/collections/{collectionId}",
                                 "/map/tiles/{tileMatrixSetId}",
                                 TAGS);
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

        return super.getTileSet(api.getData(), requestContext, "/collections/{collectionId}/map/tiles/{tileMatrixSetId}", collectionId, tileMatrixSetId);
    }
}
