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
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tiles.api.AbstractEndpointTileSingleCollection;
import de.ii.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ogcapi.tiles.domain.TileCache;
import de.ii.ogcapi.tiles.domain.TileProvider;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import java.io.IOException;
import java.util.List;
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
 * @langEn Access single-layer map tiles
 * @langDe TODO
 * @name Tile
 * @path /{apiId}/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol
 */

/**
 * Handle responses under '/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Singleton
@AutoBind
public class EndpointMapTileSingleCollection extends AbstractEndpointTileSingleCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointMapTileSingleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access single-layer map tiles");

    private final Client client;

    @Inject
    EndpointMapTileSingleCollection(FeaturesCoreProviders providers,
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
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats == null) {
            formats = extensionRegistry.getExtensionsForType(MapTileFormatExtension.class);
        }
        return formats;
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
                                 ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE_COLLECTION,
                                 "/collections/{collectionId}",
                                 "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                                 TAGS);
    }

    @Path("/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    public Response getTile(@Context OgcApi api, @PathParam("collectionId") String collectionId,
                            @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                            @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                            @Context UriInfo uriInfo, @Context ApiRequestContext requestContext)
            throws CrsTransformationException, IOException, NotFoundException {
        TileProvider tileProvider = api.getData()
            .getExtension(MapTilesConfiguration.class, collectionId)
            .map(MapTilesConfiguration::getMapProvider)
            .orElseThrow();
        return super.getTile(api, requestContext, uriInfo,
                             "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
                             collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol,
                             tileProvider);
    }
}
