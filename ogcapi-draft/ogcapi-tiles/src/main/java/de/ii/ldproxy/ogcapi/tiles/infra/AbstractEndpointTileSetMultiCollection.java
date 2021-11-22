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
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableQueryInputTileSet;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;

import javax.ws.rs.core.Response;
import java.util.List;

public abstract class AbstractEndpointTileSetMultiCollection extends Endpoint {

    private final TilesQueriesHandler queryHandler;

    AbstractEndpointTileSetMultiCollection(ExtensionRegistry extensionRegistry, TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats == null) {
            formats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class);
        }
        return formats;
    }

    public Response getTileSet(OgcApi api, ApiRequestContext requestContext, String tileMatrixSetId) {

        OgcApiDataV2 apiData = api.getData();
        String path = "/tiles/{tileMatrixSetId}";
        checkPathParameter(extensionRegistry, apiData, path, "tileMatrixSetId", tileMatrixSetId);
        TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();

        TilesQueriesHandler.QueryInputTileSet queryInput = new ImmutableQueryInputTileSet.Builder()
                .from(getGenericQueryInput(apiData))
                .tileMatrixSetId(tileMatrixSetId)
                .center(tilesConfiguration.getCenterDerived())
                .zoomLevels(tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId))
                .build();

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
    }

}
