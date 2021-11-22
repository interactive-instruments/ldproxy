/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

public abstract class AbstractEndpointTileSetsMultiCollection extends Endpoint {

    private final TilesQueriesHandler queryHandler;

    AbstractEndpointTileSetsMultiCollection(ExtensionRegistry extensionRegistry, TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    public Response getTileSets(OgcApi api, ApiRequestContext requestContext) {

        OgcApiDataV2 apiData = api.getData();
        if (!isEnabledForApi(apiData))
            throw new NotFoundException("Multi-collection tiles are not available in this API.");

        TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileSets queryInput = new ImmutableQueryInputTileSets.Builder()
                .from(getGenericQueryInput(apiData))
                .center(tilesConfiguration.getCenterDerived())
                .tileMatrixSetZoomLevels(tilesConfiguration.getZoomLevelsDerived())
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SETS, queryInput, requestContext);
    }

}
