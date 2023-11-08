/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.api;

import static de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsQueriesHandler.GROUP_TILES_READ;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceSet;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.tiles.app.TilesBuildingBlock;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileSets.Builder;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

public abstract class AbstractEndpointTileSetsMultiCollection extends Endpoint {

  private final TilesQueriesHandler queryHandler;
  private final TilesProviders tilesProviders;

  public AbstractEndpointTileSetsMultiCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      TilesProviders tilesProviders) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.tilesProviders = tilesProviders;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(TilesConfiguration.class)
        .filter(TilesConfiguration::isEnabled)
        .filter(cfg -> cfg.hasDatasetTiles(tilesProviders, apiData))
        .isPresent();
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(TileSetsFormatExtension.class);
    return formats;
  }

  protected ApiEndpointDefinition computeDefinition(
      OgcApiDataV2 apiData,
      String apiEntrypoint,
      int sortPriority,
      String path,
      String dataType,
      List<String> tags) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint(apiEntrypoint)
            .sortPriority(sortPriority);
    HttpMethods method = HttpMethods.GET;
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, path);
    String operationSummary = "retrieve a list of the available tile sets";
    Optional<String> operationDescription =
        Optional.of(
            "This operation fetches the list of multi-layer tile sets supported by this API.");
    ImmutableOgcApiResourceSet.Builder resourceBuilderSet =
        new ImmutableOgcApiResourceSet.Builder().path(path).subResourceType("Tile Set");
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            ImmutableList.of(),
            getResponseContent(apiData),
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId("getTileSetsList", "dataset", dataType),
            GROUP_TILES_READ,
            tags,
            TilesBuildingBlock.MATURITY,
            TilesBuildingBlock.SPEC)
        .ifPresent(operation -> resourceBuilderSet.putOperations(method.name(), operation));
    definitionBuilder.putResources(path, resourceBuilderSet.build());

    return definitionBuilder.build();
  }

  protected Response getTileSets(
      OgcApiDataV2 apiData,
      ApiRequestContext requestContext,
      String definitionPath,
      boolean onlyWebMercatorQuad) {

    if (!isEnabledForApi(apiData))
      throw new NotFoundException("Multi-collection tiles are not available in this API.");

    TilesetMetadata tilesetMetadata = tilesProviders.getTilesetMetadataOrThrow(apiData);

    TilesQueriesHandler.QueryInputTileSets queryInput =
        new Builder()
            .from(getGenericQueryInput(apiData))
            .tileMatrixSetIds(tilesetMetadata.getTileMatrixSets())
            .path(definitionPath)
            .onlyWebMercatorQuad(onlyWebMercatorQuad)
            .tileEncodings(tilesetMetadata.getEncodings())
            .build();

    return queryHandler.handle(TilesQueriesHandler.Query.TILE_SETS, queryInput, requestContext);
  }
}
