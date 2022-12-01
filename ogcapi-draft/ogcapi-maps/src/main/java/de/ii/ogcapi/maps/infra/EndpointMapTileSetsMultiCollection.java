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
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tiles.api.AbstractEndpointTileSetsMultiCollection;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @langEn Access multi-layer map tiles
 * @langDe TODO
 * @name Tilesets
 * @path /{apiId}/map/tiles
 */

/** Handle responses under '/tiles'. */
@Singleton
@AutoBind
public class EndpointMapTileSetsMultiCollection extends AbstractEndpointTileSetsMultiCollection
    implements ConformanceClass {

  private static final List<String> TAGS = ImmutableList.of("Access multi-layer map tiles");

  @Inject
  EndpointMapTileSetsMultiCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      FeaturesCoreProviders providers) {
    super(extensionRegistry, queryHandler, providers);
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tilesets-list",
        "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/dataset-tilesets");
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
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    return computeDefinition(
        apiData,
        "map",
        ApiEndpointDefinition.SORT_PRIORITY_MAP_TILE_SETS,
        "/map/tiles",
        "map",
        TAGS);
  }

  @Path("/tiles")
  @GET
  public Response getTileSets(@Context OgcApi api, @Context ApiRequestContext requestContext) {

    List<String> tileEncodings =
        api.getData()
            .getExtension(MapTilesConfiguration.class)
            .map(MapTilesConfiguration::getTileEncodingsDerived)
            .orElseThrow(() -> new IllegalStateException("No tile encoding available."));
    return super.getTileSets(api.getData(), requestContext, "/map/tiles", true, tileEncodings);
  }
}
