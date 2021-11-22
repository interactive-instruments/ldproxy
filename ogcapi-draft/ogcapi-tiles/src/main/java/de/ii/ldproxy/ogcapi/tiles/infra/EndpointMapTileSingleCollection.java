/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.tiles.domain.StaticTileProviderStore;
import de.ii.ldproxy.ogcapi.tiles.domain.TileCache;
import de.ii.ldproxy.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetLimitsGenerator;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import io.dropwizard.auth.Auth;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Handle responses under '/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTileSingleCollection extends AbstractEndpointTileSingleCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointMapTileSingleCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access single-layer tiles");

    private final FeaturesCoreProviders providers;

    EndpointMapTileSingleCollection(@Requires FeaturesCoreProviders providers,
                                    @Requires ExtensionRegistry extensionRegistry,
                                    @Requires TilesQueriesHandler queryHandler,
                                    @Requires CrsTransformerFactory crsTransformerFactory,
                                    @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                                    @Requires TileCache cache,
                                    @Requires StaticTileProviderStore staticTileProviderStore,
                                    @Requires TileMatrixSetRepository tileMatrixSetRepository) {
        super(providers, extensionRegistry, queryHandler, crsTransformerFactory, limitsGenerator, cache, staticTileProviderStore, tileMatrixSetRepository);
        this.providers = providers;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public List<? extends FormatExtension> getFormats() {
        if (formats==null)
            formats = extensionRegistry.getExtensionsForType(TileFormatExtension.class);
        return formats;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        Optional<TilesConfiguration> config = apiData.getCollections()
                                                     .get(collectionId)
                                                     .getExtension(TilesConfiguration.class);
        if (config.map(cfg -> !cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
            // Tiles are pre-generated as a static tile set
            return config.filter(ExtensionConfiguration::isEnabled)
                         .isPresent();
        } else {
            // Tiles are generated on-demand from a data source
            if (config.filter(TilesConfiguration::isEnabled)
                      .filter(TilesConfiguration::isSingleCollectionEnabled)
                      .isEmpty()) return false;
            // currently no vector tiles support for WFS backends
            return providers.getFeatureProvider(apiData)
                            .map(FeatureProvider2::supportsHighLoad)
                            .orElse(false);
        }
    }

    @Override
    protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
        return null;
    }

    @Path("/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
    @GET
    public Response getTile(@Auth Optional<User> optionalUser, @Context OgcApi api, @PathParam("collectionId") String collectionId,
                            @PathParam("tileMatrixSetId") String tileMatrixSetId, @PathParam("tileMatrix") String tileMatrix,
                            @PathParam("tileRow") String tileRow, @PathParam("tileCol") String tileCol,
                            @Context UriInfo uriInfo, @Context ApiRequestContext requestContext)
            throws CrsTransformationException, IOException, NotFoundException {

        return super.getTile(optionalUser, api, collectionId, tileMatrixSetId, tileMatrix, tileRow, tileCol, uriInfo, requestContext);
    }
}
