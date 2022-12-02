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
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.maps.domain.MapTileFormatExtension;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.api.EndpointTileMixin;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
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
 * @langEn Access single-layer map tiles
 * @langDe TODO
 * @name Tile
 * @path
 *     /{apiId}/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol
 */

/**
 * Handle responses under
 * '/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}'.
 */
@Singleton
@AutoBind
public class EndpointMapTileSingleCollection extends EndpointSubCollection
    implements EndpointTileMixin {

  private static final List<String> TAGS = ImmutableList.of("Access single-layer map tiles");

  private final TilesQueriesHandler queryHandler;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  EndpointMapTileSingleCollection(
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
    if (apiData
        .getExtension(MapTilesConfiguration.class, collectionId)
        .filter(ExtensionConfiguration::isEnabled)
        .filter(MapTilesConfiguration::isSingleCollectionEnabled)
        .isPresent()) return super.isEnabledForApi(apiData, collectionId);
    return false;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    return computeDefinitionSingle(
        extensionRegistry,
        this,
        apiData,
        "collections",
        ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE_COLLECTION,
        "/collections/{collectionId}",
        "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
        getOperationId("getTile", EndpointTileMixin.COLLECTION_ID_PLACEHOLDER, "collection", "map"),
        TAGS);
  }

  @Path("/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
  @GET
  public Response getTile(
      @Context OgcApi api,
      @PathParam("collectionId") String collectionId,
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
            Optional.of(collectionId),
            tileMatrixSetId,
            tileMatrix,
            tileRow,
            tileCol);

    return queryHandler.handle(TilesQueriesHandler.Query.TILE, queryInput, requestContext);
  }
}
