/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tiles.api.EndpointTileMixin;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @langEn Access multi-layer map tiles
 * @langDe TODO
 * @name Tile
 * @path /{apiId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}
 * @formats {@link de.ii.ogcapi.maps.domain.MapTileFormatExtension}
 */

/** Handle responses under '/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'. */
@Singleton
@AutoBind
public class EndpointMapTileMultiCollection extends Endpoint implements EndpointTileMixin {

  private static final List<String> TAGS = ImmutableList.of("Access multi-layer map tiles");

  private final TilesQueriesHandler queryHandler;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  EndpointMapTileMultiCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      TileMatrixSetLimitsGenerator limitsGenerator,
      TileMatrixSetRepository tileMatrixSetRepository) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.limitsGenerator = limitsGenerator;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
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
    if (apiData
        .getExtension(MapTilesConfiguration.class)
        .filter(ExtensionConfiguration::isEnabled)
        .filter(MapTilesConfiguration::isMultiCollectionEnabled)
        .isPresent()) return super.isEnabledForApi(apiData);
    return false;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    return computeDefinitionMulti(
        extensionRegistry,
        this,
        apiData,
        "map",
        ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE,
        "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
        getOperationId("getTile", "dataset", "map"),
        TAGS);
  }

  @Path("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
  @GET
  public Response getTile(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @PathParam("tileMatrixSetId") String tileMatrixSetId,
      @PathParam("tileMatrix") String tileMatrix,
      @PathParam("tileRow") String tileRow,
      @PathParam("tileCol") String tileCol,
      @Context UriInfo uriInfo,
      @Context ApiRequestContext requestContext)
      throws CrsTransformationException, IOException, NotFoundException {
    QueryInput queryInput =
        getQueryInputTile(
            extensionRegistry,
            this,
            limitsGenerator,
            tileMatrixSetRepository,
            api,
            requestContext,
            uriInfo,
            "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
            Optional.empty(),
            tileMatrixSetId,
            tileMatrix,
            tileRow,
            tileCol);

    return queryHandler.handle(TilesQueriesHandler.Query.TILE, queryInput, requestContext);
  }
}
