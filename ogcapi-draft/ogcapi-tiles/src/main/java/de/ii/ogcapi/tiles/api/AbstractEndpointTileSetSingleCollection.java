/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.api;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileSet.Builder;
import de.ii.ogcapi.tiles.domain.TileSetFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEndpointTileSetSingleCollection extends EndpointSubCollection {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractEndpointTileSetSingleCollection.class);

  private final TilesQueriesHandler queryHandler;
  private final FeaturesCoreProviders providers;

  public AbstractEndpointTileSetSingleCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      FeaturesCoreProviders providers) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.providers = providers;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null) {
      formats = extensionRegistry.getExtensionsForType(TileSetFormatExtension.class);
    }
    return formats;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    Optional<TilesConfiguration> config =
        apiData.getCollections().get(collectionId).getExtension(TilesConfiguration.class);
    if (config.map(cfg -> !cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
      // Tiles are pre-generated as a static tile set
      return config.filter(ExtensionConfiguration::isEnabled).isPresent();
    } else {
      // Tiles are generated on-demand from a data source
      if (config
          .filter(TilesConfiguration::isEnabled)
          .filter(TilesConfiguration::isSingleCollectionEnabled)
          .isEmpty()) return false;
      // currently no vector tiles support for WFS backends
      return providers
          .getFeatureProvider(apiData)
          .map(FeatureProvider2::supportsHighLoad)
          .orElse(false);
    }
  }

  protected ApiEndpointDefinition computeDefinition(
      OgcApiDataV2 apiData,
      String apiEntrypoint,
      int sortPriority,
      String basePath,
      String subSubPath,
      List<String> tags) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint(apiEntrypoint)
            .sortPriority(sortPriority);
    final String path = basePath + subSubPath;
    final HttpMethods method = HttpMethods.GET;
    final List<OgcApiPathParameter> pathParameters =
        getPathParameters(extensionRegistry, apiData, path);
    final Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
    if (optCollectionIdParam.isEmpty()) {
      LOGGER.error(
          "Path parameter 'collectionId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
      boolean explode = collectionIdParam.isExplodeInOpenApi(apiData);
      final List<String> collectionIds =
          (explode) ? collectionIdParam.getValues(apiData) : ImmutableList.of("{collectionId}");
      for (String collectionId : collectionIds) {
        List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId);
        String operationSummary = "retrieve information about a tile set";
        Optional<String> operationDescription =
            Optional.of("This operation fetches information about a tile set.");
        String resourcePath = path.replace("{collectionId}", collectionId);
        ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder =
            new ImmutableOgcApiResourceAuxiliary.Builder()
                .path(resourcePath)
                .pathParameters(pathParameters);
        Map<MediaType, ApiMediaTypeContent> responseContent =
            collectionId.startsWith("{")
                ? getContent(apiData, Optional.empty(), subSubPath, HttpMethods.GET)
                : getContent(apiData, Optional.of(collectionId), subSubPath, HttpMethods.GET);
        ApiOperation.getResource(
                apiData,
                resourcePath,
                false,
                queryParameters,
                ImmutableList.of(),
                responseContent,
                operationSummary,
                operationDescription,
                Optional.empty(),
                tags)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }

    return definitionBuilder.build();
  }

  protected Response getTileSet(
      OgcApiDataV2 apiData,
      ApiRequestContext requestContext,
      String definitionPath,
      String collectionId,
      String tileMatrixSetId) {

    checkPathParameter(extensionRegistry, apiData, definitionPath, "collectionId", collectionId);
    checkPathParameter(
        extensionRegistry, apiData, definitionPath, "tileMatrixSetId", tileMatrixSetId);

    FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
    TilesConfiguration tilesConfiguration =
        featureType.getExtension(TilesConfiguration.class).get();

    TilesQueriesHandler.QueryInputTileSet queryInput =
        new Builder()
            .from(getGenericQueryInput(apiData))
            .collectionId(collectionId)
            .tileMatrixSetId(tileMatrixSetId)
            .center(tilesConfiguration.getCenterDerived())
            .zoomLevels(tilesConfiguration.getZoomLevelsDerived().get(tileMatrixSetId))
            .path(definitionPath)
            .build();

    return queryHandler.handle(TilesQueriesHandler.Query.TILE_SET, queryInput, requestContext);
  }
}
