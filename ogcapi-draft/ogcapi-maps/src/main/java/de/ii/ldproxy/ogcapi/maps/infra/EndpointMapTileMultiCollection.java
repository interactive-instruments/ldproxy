/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.maps.infra;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ldproxy.ogcapi.domain.ApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ApiOperation;
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiEndpointDefinition;
import de.ii.ldproxy.ogcapi.domain.ImmutableApiMediaType;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiPathParameter;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.maps.app.MapProviderTileserver;
import de.ii.ldproxy.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ldproxy.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.api.AbstractEndpointTileMultiCollection;
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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Handle responses under '/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Component
@Provides
@Instantiate
public class EndpointMapTileMultiCollection extends AbstractEndpointTileMultiCollection {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointMapTileMultiCollection.class);

    private static final List<String> TAGS = ImmutableList.of("Access multi-layer map tiles");

    private final Client client;

    EndpointMapTileMultiCollection(@Requires FeaturesCoreProviders providers,
                                   @Requires ExtensionRegistry extensionRegistry,
                                   @Requires TilesQueriesHandler queryHandler,
                                   @Requires CrsTransformerFactory crsTransformerFactory,
                                   @Requires TileMatrixSetLimitsGenerator limitsGenerator,
                                   @Requires TileCache cache,
                                   @Requires StaticTileProviderStore staticTileProviderStore,
                                   @Requires TileMatrixSetRepository tileMatrixSetRepository) {
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
        if (!apiData.getExtension(MapTilesConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false))
            return false;
        return super.isEnabledForApi(apiData);
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

        final String urlTemplate = api.getData()
            .getExtension(MapTilesConfiguration.class)
            .map(MapTilesConfiguration::getMapProvider)
            .map(mapProvider -> {
                if (mapProvider instanceof MapProviderTileserver) {
                    return ((MapProviderTileserver) mapProvider).getUrlTemplate();
                }
                return null;
            })
            .orElse(null);

        if (Objects.isNull(urlTemplate))
            throw new IllegalStateException("The MAP_TILES configuration is invalid, no 'urlTemplate' was found.");

        // TODO
        ApiMediaType mediaType = new ImmutableApiMediaType.Builder()
            .type(new MediaType("image", "png"))
            .label("PNG")
            .parameter("png")
            .build();
        return client.target(urlTemplate)
            .resolveTemplate("tileMatrix", tileMatrix)
            .resolveTemplate("tileRow", tileRow)
            .resolveTemplate("tileCol", tileCol)
            .resolveTemplate("fileExtension", mediaType.fileExtension())
            .request(mediaType.type())
            .get();
    }
}
