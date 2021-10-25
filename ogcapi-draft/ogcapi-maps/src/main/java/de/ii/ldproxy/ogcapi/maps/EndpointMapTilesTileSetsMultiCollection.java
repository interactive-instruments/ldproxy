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
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * Handle responses under '/map/tiles'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTilesTileSetsMultiCollection extends Endpoint {

    private final TilesQueriesHandler queryHandler;

    EndpointMapTilesTileSetsMultiCollection(@Requires ExtensionRegistry extensionRegistry,
                                            @Requires TilesQueriesHandler queryHandler) {
        super(extensionRegistry);
        this.queryHandler = queryHandler;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<MapTilesConfiguration> config = apiData.getExtension(MapTilesConfiguration.class);
        Optional<TilesConfiguration> tilesConfig = apiData.getExtension(TilesConfiguration.class);
        boolean mapTilesEnabled = config.filter(MapTilesConfiguration::isEnabled)
                .isPresent();
        boolean tilesEnabled = tilesConfig.filter(TilesConfiguration::isEnabled)
                .filter(TilesConfiguration::isMultiCollectionEnabled)
                .isPresent();
        return mapTilesEnabled && tilesEnabled;
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

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        return null;
    }

    @Path("")
    @GET
    public Response getTileSets(@Context OgcApi api, @Context ApiRequestContext requestContext) {
        OgcApiDataV2 apiData = api.getData();
        if (!isEnabledForApi(apiData)) {
            throw new NotFoundException("Multi-collection tiles are not available in this API.");
        }
        TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();
        TilesQueriesHandler.QueryInputTileSets queryInput = TileEndpointsHelper.getTileSetsQueryInput(tilesConfiguration, getGenericQueryInput(apiData));
        return queryHandler.handle(TilesQueriesHandler.Query.TILE_SETS, queryInput, requestContext);
    }
}
