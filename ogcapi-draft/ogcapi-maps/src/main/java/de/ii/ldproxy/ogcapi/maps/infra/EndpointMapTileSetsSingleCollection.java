/**
 * Copyright 2021 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ldproxy.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.api.AbstractEndpointTileSetsSingleCollection;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

/**
 * Handle responses under '/collections/{collectionId}/tiles'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTileSetsSingleCollection extends AbstractEndpointTileSetsSingleCollection implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointMapTileSetsSingleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access single-layer map tiles");

    EndpointMapTileSetsSingleCollection(@Requires ExtensionRegistry extensionRegistry,
                                        @Requires TilesQueriesHandler queryHandler,
                                        @Requires FeaturesCoreProviders providers) {
        super(extensionRegistry, queryHandler, providers);
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/tilesets-list",
                                "http://www.opengis.net/spec/ogcapi-tiles-1/0.0/conf/geodata-tilesets");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
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
                                 ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE_SETS_COLLECTION,
                                 "/collections/{collectionId}",
                                 "/map/tiles",
                                 TAGS);
    }

    /**
     * retrieve all available tile matrix sets from the collection
     *
     * @return all tile matrix sets from the collection in a json array
     */
    @Path("/{collectionId}/map/tiles")
    @GET
    public Response getTileSets(@Context OgcApi api, @Context ApiRequestContext requestContext,
                                @PathParam("collectionId") String collectionId) {

        return super.getTileSets(api.getData(), requestContext,
                                 "/collections/{collectionId}/map/tiles", collectionId, true);
    }

}
