/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.infra;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceAuxiliary;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceSet;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableQueryInputTileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableQueryInputTileMatrixSets;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsFormatExtension;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsQueriesHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @langEn Returns the definition of the tiling scheme according to the [OGC Two Dimensional Tile
 *     Matrix Set standard](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html).
 * @langDe TODO
 * @name Tile Matrix Sets
 * @path /{apiId}/tileMatrixSets
 * @format {@link de.ii.ogcapi.tiles.domain.TileFormatExtension}
 */

/** fetch tiling schemes / tile matrix sets that have been configured for an API */
@Singleton
@AutoBind
public class EndpointTileMatrixSets extends Endpoint implements ConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointTileMatrixSets.class);
  private static final List<String> TAGS = ImmutableList.of("Discover and fetch tiling schemes");

  private final FeaturesCoreProviders providers;
  private final TileMatrixSetsQueriesHandler queryHandler;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  EndpointTileMatrixSets(
      ExtensionRegistry extensionRegistry,
      TileMatrixSetsQueriesHandler queryHandler,
      FeaturesCoreProviders providers,
      TileMatrixSetRepository tileMatrixSetRepository) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.providers = providers;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/tms/2.0/conf/tilematrixset",
        "http://www.opengis.net/spec/tms/2.0/conf/json-tilematrixset");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TileMatrixSetsConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(TileMatrixSetsFormatExtension.class);
    return formats;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(TileMatrixSetsConfiguration.class)
        .filter(TileMatrixSetsConfiguration::isEnabled)
        .isPresent();
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("tileMatrixSets")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_TILE_MATRIX_SETS);
    String path = "/tileMatrixSets";
    HttpMethods method = HttpMethods.GET;
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, path);
    String operationSummary = "retrieve a list of the available tiling schemes";
    Optional<String> operationDescription =
        Optional.of(
            "This operation fetches the set of tiling schemes supported by this API. "
                + "For each tiling scheme the id, a title and the link to the tiling scheme object is provided.");
    ImmutableOgcApiResourceSet.Builder resourceBuilderSet =
        new ImmutableOgcApiResourceSet.Builder().path(path).subResourceType("Tile Matrix Set");
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            ImmutableList.of(),
            getContent(apiData, path),
            operationSummary,
            operationDescription,
            Optional.empty(),
            TAGS)
        .ifPresent(operation -> resourceBuilderSet.putOperations(method.name(), operation));
    definitionBuilder.putResources(path, resourceBuilderSet.build());

    path = "/tileMatrixSets/{tileMatrixSetId}";
    queryParameters = getQueryParameters(extensionRegistry, apiData, path);
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    if (pathParameters.stream().noneMatch(param -> param.getName().equals("tileMatrixSetId"))) {
      LOGGER.error(
          "Path parameter 'tileMatrixSetId' missing for resource at path '"
              + path
              + "'. The GET method will not be available.");
    } else {
      operationSummary = "fetch information about the tiling scheme `{tileMatrixSetId}`";
      operationDescription =
          Optional.of(
              "Returns the definition of the tiling scheme according to the [OGC Two Dimensional Tile Matrix Set standard](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html).");
      ImmutableOgcApiResourceAuxiliary.Builder resourceBuilder =
          new ImmutableOgcApiResourceAuxiliary.Builder().path(path).pathParameters(pathParameters);
      ApiOperation.getResource(
              apiData,
              path,
              false,
              queryParameters,
              ImmutableList.of(),
              getContent(apiData, path),
              operationSummary,
              operationDescription,
              Optional.empty(),
              TAGS)
          .ifPresent(operation -> resourceBuilder.putOperations(method.name(), operation));
      definitionBuilder.putResources(path, resourceBuilder.build());
    }

    return definitionBuilder.build();
  }

  /**
   * retrieve all available tile matrix sets
   *
   * @return all tile matrix sets in a json array or an HTML view
   */
  @GET
  public Response getTileMatrixSets(
      @Context OgcApi api, @Context ApiRequestContext requestContext) {

    if (!isEnabledForApi(api.getData()))
      throw new NotFoundException("Tile matrix sets are not available in this API.");

    ImmutableSet<TileMatrixSet> tmsSet =
        getPathParameters(extensionRegistry, api.getData(), "/tileMatrixSets/{tileMatrixSetId}")
            .stream()
            .filter(param -> param.getName().equalsIgnoreCase("tileMatrixSetId"))
            .findFirst()
            .map(
                param ->
                    param.getValues(api.getData()).stream()
                        .map(tileMatrixSetRepository::get)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(ImmutableSet.toImmutableSet()))
            .orElse(ImmutableSet.of());

    TileMatrixSetsQueriesHandler.QueryInputTileMatrixSets queryInput =
        new ImmutableQueryInputTileMatrixSets.Builder()
            .from(getGenericQueryInput(api.getData()))
            .tileMatrixSets(tmsSet)
            .build();

    return queryHandler.handle(
        TileMatrixSetsQueriesHandler.Query.TILE_MATRIX_SETS, queryInput, requestContext);
  }

  /**
   * retrieve one specific tile matrix set by id
   *
   * @param tileMatrixSetId the local identifier of a specific tile matrix set
   * @return the tiling scheme in a json file
   */
  @Path("/{tileMatrixSetId}")
  @GET
  public Response getTileMatrixSet(
      @PathParam("tileMatrixSetId") String tileMatrixSetId,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext) {

    checkPathParameter(
        extensionRegistry,
        api.getData(),
        "/tileMatrixSets/{tileMatrixSetId}",
        "tileMatrixSetId",
        tileMatrixSetId);

    TileMatrixSetsQueriesHandler.QueryInputTileMatrixSet queryInput =
        new ImmutableQueryInputTileMatrixSet.Builder()
            .from(getGenericQueryInput(api.getData()))
            .tileMatrixSetId(tileMatrixSetId)
            .build();

    return queryHandler.handle(
        TileMatrixSetsQueriesHandler.Query.TILE_MATRIX_SET, queryInput, requestContext);
  }
}
