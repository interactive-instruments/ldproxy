/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiExtensionHealth;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tiles.api.EndpointTileMixin;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @title Dataset Map Tiles
 * @path styles/{styleId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}
 * @langEn Access map tiles of a dataset in a specific style.
 * @langDe Zugriff auf Rasterkacheln eines Datensatzes in einem bestimmten Style.
 * @ref:formats {@link de.ii.ogcapi.tiles.domain.MapTileFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointStyledMapTileDataset extends Endpoint
    implements ConformanceClass, EndpointTileMixin, ApiExtensionHealth {

  private static final List<String> TAGS = ImmutableList.of("Access map tiles");

  private final TilesQueriesHandler queryHandler;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final TilesProviders tilesProviders;

  @Inject
  EndpointStyledMapTileDataset(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      TileMatrixSetLimitsGenerator limitsGenerator,
      TileMatrixSetRepository tileMatrixSetRepository,
      TilesProviders tilesProviders) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.limitsGenerator = limitsGenerator;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.tilesProviders = tilesProviders;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return false // TODO
        && apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(cfg -> cfg.hasDatasetMapTiles(tilesProviders, apiData))
            .isPresent();
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/core");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(TileFormatExtension.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    return computeDefinitionMulti(
        extensionRegistry,
        this,
        apiData,
        tilesProviders,
        "styles",
        ApiEndpointDefinition.SORT_PRIORITY_TILE,
        "/styles/{styleId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
        getOperationId("getTile", "dataset", "style", "map"),
        TAGS);
  }

  @Path("/{styleId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
  @GET
  public Response getTile(
      @Context OgcApi api,
      @PathParam("styleId") String styleId,
      @PathParam("tileMatrixSetId") String tileMatrixSetId,
      @PathParam("tileMatrix") String tileMatrix,
      @PathParam("tileRow") String tileRow,
      @PathParam("tileCol") String tileCol,
      @Context ApiRequestContext requestContext)
      throws CrsTransformationException, IOException, NotFoundException {
    QueryInput queryInput =
        getQueryInputTile(
            extensionRegistry,
            this,
            limitsGenerator,
            tileMatrixSetRepository,
            api,
            tilesProviders,
            requestContext,
            "/styles/{styleId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
            Optional.empty(),
            Optional.of(styleId),
            tileMatrixSetId,
            tileMatrix,
            tileRow,
            tileCol);

    return queryHandler.handle(TilesQueriesHandler.Query.TILE, queryInput, requestContext);
  }

  @Override
  public Set<Volatile2> getVolatiles(OgcApiDataV2 apiData) {
    return Set.of(
        queryHandler, tileMatrixSetRepository, tilesProviders.getTileProviderOrThrow(apiData));
  }
}
