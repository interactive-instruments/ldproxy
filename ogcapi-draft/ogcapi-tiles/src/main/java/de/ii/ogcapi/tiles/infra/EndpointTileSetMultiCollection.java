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
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.api.AbstractEndpointTileSetMultiCollection;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
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

/**
 * @langEn Access multi-layer tiles
 * @langDe TODO
 * @name Tileset
 * @path /{apiId}/tiles/{tileMatrixSetId}
 * @format {@link de.ii.ogcapi.tiles.domain.TileFormatExtension}
 */

/** Handle responses under '/tiles/{tileMatrixSetId}'. */
@Singleton
@AutoBind
public class EndpointTileSetMultiCollection extends AbstractEndpointTileSetMultiCollection
    implements ConformanceClass {

  private static final List<String> TAGS = ImmutableList.of("Access multi-layer tiles");

  @Inject
  EndpointTileSetMultiCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      FeaturesCoreProviders providers) {
    super(extensionRegistry, queryHandler, providers);
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tileset");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    return computeDefinition(
        apiData,
        "tiles",
        ApiEndpointDefinition.SORT_PRIORITY_TILE_SET,
        "/tiles/{tileMatrixSetId}",
        apiData
                .getExtension(TilesConfiguration.class)
                .map(c -> c.getTileEncodingsDerived().contains("MVT"))
                .orElse(false)
            ? "vector"
            : "map",
        TAGS);
  }

  /**
   * retrieve tilejson for the MVT tile sets
   *
   * @return a tilejson file
   */
  @Path("/{tileMatrixSetId}")
  @GET
  public Response getTileSet(
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @PathParam("tileMatrixSetId") String tileMatrixSetId) {

    return super.getTileSet(
        api.getData(), requestContext, "/tiles/{tileMatrixSetId}", tileMatrixSetId);
  }
}
