/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ldproxy.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.api.AbstractEndpointTileMultiCollection;
import de.ii.ldproxy.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ldproxy.ogcapi.tiles.domain.TileCache;
import de.ii.ldproxy.ogcapi.tiles.domain.TileProvider;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import io.dropwizard.auth.Auth;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle responses under '/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Singleton
@AutoBind
public class EndpointMapTileMultiCollection extends AbstractEndpointTileMultiCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointMapTileMultiCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access multi-layer map tiles");

    private final Client client;

    @Inject
    EndpointMapTileMultiCollection(FeaturesCoreProviders providers,
                                   ExtensionRegistry extensionRegistry,
                                   TilesQueriesHandler queryHandler,
                                   CrsTransformerFactory crsTransformerFactory,
                                   TileMatrixSetLimitsGenerator limitsGenerator,
                                   TileCache cache,
                                   StaticTileProviderStore staticTileProviderStore,
                                   TileMatrixSetRepository tileMatrixSetRepository) {
        super(providers, extensionRegistry, queryHandler, crsTransformerFactory, limitsGenerator, cache, staticTileProviderStore, tileMatrixSetRepository);
        this.client = ClientBuilder.newClient();
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats == null) {
            formats = extensionRegistry.getExtensionsForType(MapTileFormatExtension.class);
        }
        return formats;
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
                                 ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE,
                                 "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                                 TAGS);
    }

    @Path("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    public Response getTile(@Auth Optional<User> optionalUser, @Context OgcApi api,
                            @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                            @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                            @Context UriInfo uriInfo, @Context ApiRequestContext requestContext)
            throws CrsTransformationException, IOException, NotFoundException {

        TileProvider tileProvider = api.getData()
            .getExtension(MapTilesConfiguration.class)
            .map(MapTilesConfiguration::getMapProvider)
            .orElseThrow();
        return super.getTile(api.getData(), requestContext, uriInfo,
                             "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                             tileMatrixSetId, tileMatrix, tileRow, tileCol,
                             tileProvider);
    }
}
