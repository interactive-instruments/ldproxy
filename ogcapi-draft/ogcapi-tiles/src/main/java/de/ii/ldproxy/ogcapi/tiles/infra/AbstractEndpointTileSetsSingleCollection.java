/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import de.ii.ldproxy.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.MinMax;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public abstract class AbstractEndpointTileSetsSingleCollection extends EndpointSubCollection {

    private final TilesQueriesHandler queryHandler;

    AbstractEndpointTileSetsSingleCollection(ExtensionRegistry extensionRegistry, TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    public Response getTileSets(OgcApi api, ApiRequestContext requestContext, String collectionId) {

        OgcApiDataV2 apiData = api.getData();
        checkPathParameter(extensionRegistry, apiData, "/collections/{collectionId}/tiles", "collectionId", collectionId);

        TilesQueriesHandler.QueryInputTileSets queryInput = new ImmutableQueryInputTileSets.Builder()
                .from(getGenericQueryInput(apiData))
                .collectionId(collectionId)
                .center(getCenter(apiData))
                .tileMatrixSetZoomLevels(getTileMatrixSetZoomLevels(apiData, collectionId))
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
