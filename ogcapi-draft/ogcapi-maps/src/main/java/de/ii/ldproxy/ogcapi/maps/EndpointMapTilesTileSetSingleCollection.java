/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps;

import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.Endpoint;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ldproxy.ogcapi.tiles.infra.TileEndpointsHelper;
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

/**
 * Handle responses under '/collections/{collectionId}/map/tiles/{tileMatrixSetId}'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTilesTileSetSingleCollection extends Endpoint {

    private final TilesQueriesHandler queryHandler;

    EndpointMapTilesTileSetSingleCollection(@Requires ExtensionRegistry extensionRegistry,
                                    @Requires TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        return null;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return MapTilesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats == null) {
            formats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class);
        }
        return formats;
    }

    @Path("/{collectionId}/map/tiles/{tileMatrixSetId}")
    @GET
    public Response getMapTilesTileSet(@Context OgcApi api,
                               @Context ApiRequestContext requestContext,
                               @PathParam("collectionId") String collectionId,
                               @PathParam("tileMatrixSetId") String tileMatrixSetId) {
        OgcApiDataV2 apiData = api.getData();
        String path = "/collections/{collectionId}/map/tiles/{tileMatrixSetId}";
        checkPathParameter(extensionRegistry, apiData, path, "collectionId", collectionId);
        checkPathParameter(extensionRegistry, apiData, path, "tileMatrixSetId", tileMatrixSetId);
        if (!"WebMercatorQuad".equals(tileMatrixSetId)) {
            throw new IllegalArgumentException("TileMatrixSet not supported: " + tileMatrixSetId);
        }
        FeatureTypeConfigurationOgcApi featureType = requestContext.getApi().getData().getCollections().get(collectionId);
        TilesConfiguration tilesConfiguration = featureType.getExtension(TilesConfiguration.class).get();
        TilesQueriesHandler.QueryInputTileSet queryInput = TileEndpointsHelper.getTileSetQueryInput(tilesConfiguration, getGenericQueryInput(apiData), tileMatrixSetId, collectionId);

        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
    }


}
