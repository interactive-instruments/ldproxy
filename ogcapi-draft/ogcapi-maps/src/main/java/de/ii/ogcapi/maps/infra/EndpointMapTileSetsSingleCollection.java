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
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tiles.api.AbstractEndpointTileSetsSingleCollection;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
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
 * Handle responses under '/collections/{collectionId}/tiles'.
 */
@Singleton
@AutoBind
public class EndpointMapTileSetsSingleCollection extends AbstractEndpointTileSetsSingleCollection implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointMapTileSetsSingleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access single-layer map tiles");

    @Inject
    EndpointMapTileSetsSingleCollection(ExtensionRegistry extensionRegistry,
                                        TilesQueriesHandler queryHandler,
                                        FeaturesCoreProviders providers) {
        super(extensionRegistry, queryHandler, providers);
    }

    @Override
    public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
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

        List<String> tileEncodings = api.getData()
            .getExtension(MapTilesConfiguration.class, collectionId)
            .map(MapTilesConfiguration::getTileEncodingsDerived)
            .orElseThrow(() -> new IllegalStateException("No tile encoding available."));
        return super.getTileSets(api.getData(), requestContext,
                                 "/collections/{collectionId}/map/tiles", collectionId,
                                 true, tileEncodings);
    }

}
