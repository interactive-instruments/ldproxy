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
import de.ii.ogcapi.tiles.api.AbstractEndpointTileSetsMultiCollection;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @title Dataset Tilesets
 * @path tiles
 * @langEn Access dataset tilesets
 * @langDe Zugriff auf die Kachelsätze zum Datensatz
 * @ref:formats {@link de.ii.ogcapi.tiles.domain.TileSetsFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointTileSetsMultiCollection extends AbstractEndpointTileSetsMultiCollection
    implements ConformanceClass {

  private static final List<String> TAGS = ImmutableList.of("Access multi-layer tiles");

  private final TilesProviders tilesProviders;

  @Inject
  EndpointTileSetsMultiCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      FeaturesCoreProviders providers,
      TilesProviders tilesProviders) {
    super(extensionRegistry, queryHandler, providers, tilesProviders);
    this.tilesProviders = tilesProviders;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tilesets-list",
        "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/dataset-tilesets");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(TileSetsFormatExtension.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    return computeDefinition(
        apiData,
        "tiles",
        ApiEndpointDefinition.SORT_PRIORITY_TILE_SETS,
        "/tiles",
        tilesProviders.getTilesetMetadataOrThrow(apiData).isVector() ? "vector" : "map",
        TAGS);
  }

  @GET
  public Response getTileSets(@Context OgcApi api, @Context ApiRequestContext requestContext) {

    Set<String> tileEncodings =
        tilesProviders
            .getTilesetMetadata(api.getData())
            .map(TilesetMetadata::getTileEncodings)
            .orElseThrow(() -> new IllegalStateException("No tile encoding available."));
    return super.getTileSets(api.getData(), requestContext, "/tiles", false, tileEncodings);
  }
}
