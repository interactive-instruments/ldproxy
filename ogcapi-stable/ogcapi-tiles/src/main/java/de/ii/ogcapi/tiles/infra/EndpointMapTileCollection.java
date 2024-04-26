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
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiExtensionHealth;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @title Collection Map Tiles
 * @path collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}
 * @langEn Access map tiles of a feature collection.
 * @langDe Zugriff auf Rasterkacheln einer Feature Collection.
 * @ref:formats {@link de.ii.ogcapi.tiles.domain.MapTileFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointMapTileCollection extends EndpointSubCollection
    implements ConformanceClass, EndpointTileMixin, ApiExtensionHealth {

  private static final List<String> TAGS = ImmutableList.of("Access map tiles");

  private final TilesQueriesHandler queryHandler;
  private final TileMatrixSetLimitsGenerator limitsGenerator;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final TilesProviders tilesProviders;

  @Inject
  EndpointMapTileCollection(
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
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
    return Objects.nonNull(collectionData)
        && collectionData.getEnabled()
        && collectionData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(cfg -> cfg.hasCollectionMapTiles(tilesProviders, apiData, collectionId))
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
    return computeDefinitionSingle(
        extensionRegistry,
        this,
        apiData,
        tilesProviders,
        "collections",
        ApiEndpointDefinition.SORT_PRIORITY_TILE_COLLECTION,
        "/collections/{collectionId}",
        "/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
        getOperationId("getTile", EndpointTileMixin.COLLECTION_ID_PLACEHOLDER, "collection", "map"),
        TAGS);
  }

  @Override
  public Map<MediaType, ApiMediaTypeContent> getResponseContent(OgcApiDataV2 apiData) {
    return getResourceFormats().stream()
        .filter(
            inputFormatExtension ->
                apiData.getCollections().keySet().stream()
                    .anyMatch(
                        collectionId ->
                            inputFormatExtension.isEnabledForApi(apiData, collectionId)))
        .map(FormatExtension::getContent)
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
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
            "/collections/{collectionId}/map/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}",
            Optional.of(collectionId),
            Optional.empty(),
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
