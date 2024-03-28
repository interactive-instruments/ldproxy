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
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.api.AbstractEndpointTileSetsSingleCollection;
import de.ii.ogcapi.tiles.api.EndpointTileMixin;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import java.util.List;
import java.util.Set;
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
 * @title Collection Tilesets
 * @path collections/{collectionId}/tiles
 * @langEn Access collection tilesets
 * @langDe Zugriff auf Kachelsätze einer Feature Collection
 * @ref:formats {@link de.ii.ogcapi.tiles.domain.TileSetsFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointTileSetsSingleCollection extends AbstractEndpointTileSetsSingleCollection
    implements ConformanceClass, ApiExtensionHealth {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(EndpointTileSetsSingleCollection.class);

  private static final List<String> TAGS = ImmutableList.of("Access single-layer tiles");

  @Inject
  EndpointTileSetsSingleCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      TilesProviders tilesProviders) {
    super(extensionRegistry, queryHandler, tilesProviders);
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tilesets-list",
        "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/geodata-tilesets");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    return computeDefinition(
        apiData,
        "collections",
        ApiEndpointDefinition.SORT_PRIORITY_TILE_SETS_COLLECTION,
        "/collections/{collectionId}",
        "/tiles",
        getOperationId(
            "getTileSetsList",
            EndpointTileMixin.COLLECTION_ID_PLACEHOLDER,
            "collection",
            EndpointTileMixin.DATA_TYPE_PLACEHOLDER),
        TAGS);
  }

  /**
   * retrieve all available tile matrix sets from the collection
   *
   * @return all tile matrix sets from the collection in a json array
   */
  @Path("/{collectionId}/tiles")
  @GET
  public Response getTileSets(
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @PathParam("collectionId") String collectionId) {
    return super.getTileSets(
        api.getData(), requestContext, "/collections/{collectionId}/tiles", collectionId, false);
  }

  @Override
  public Set<Volatile2> getVolatiles(OgcApiDataV2 apiData) {
    return Set.of(queryHandler, tilesProviders.getTileProviderOrThrow(apiData));
  }
}
