/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.api.AbstractEndpointTileSetsMultiCollection;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Handle responses under '/tiles'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTileSetsMultiCollection extends AbstractEndpointTileSetsMultiCollection implements ConformanceClass {

    private static final List<String> TAGS = ImmutableList.of("Access multi-layer map tiles");

    EndpointMapTileSetsMultiCollection(@Requires ExtensionRegistry extensionRegistry,
                                       @Requires TilesQueriesHandler queryHandler,
                                       @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry, queryHandler, providers);
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/tilesets-list",
                                "http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/dataset-tilesets");
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
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        return computeDefinition(apiData,
                                 "map",
                                 ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE_SETS,
                                 "/map/tiles",
                                 TAGS);
    }

    @Path("/tiles")
    @GET
    public Response getTileSets(@Context OgcApi api, @Context ApiRequestContext requestContext) {

        List<String> tileEncodings = api.getData()
            .getExtension(MapTilesConfiguration.class)
            .map(MapTilesConfiguration::getTileEncodingsDerived)
            .orElseThrow(() -> new IllegalStateException("No tile encoding available."));
        return super.getTileSets(api.getData(), requestContext, "/map/tiles",
                                 true, tileEncodings);
    }
}
