/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.tiles3d.domain.Format3dTilesSubtree;
import de.ii.ogcapi.tiles3d.domain.ImmutableQueryInputSubtree;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles.Query;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles.QueryInputSubtree;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.services.domain.ServicesContext;
import io.dropwizard.auth.Auth;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class Endpoint3dTilesSubtrees extends EndpointSubCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(Endpoint3dTilesSubtrees.class);

  private static final List<String> TAGS = ImmutableList.of("Access data as 3D Tiles");

  private final FeaturesCoreProviders providers;
  private final QueriesHandler3dTiles queryHandler;
  private final FeaturesCoreQueriesHandler queriesHandlerFeatures;
  private final FeaturesQuery featuresQuery;
  private final URI serviceUri;

  @Inject
  public Endpoint3dTilesSubtrees(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      QueriesHandler3dTiles queryHandler,
      FeaturesCoreQueriesHandler queriesHandlerFeatures,
      FeaturesQuery featuresQuery,
      ServicesContext servicesContext) {
    super(extensionRegistry);
    this.providers = providers;
    this.queryHandler = queryHandler;
    this.queriesHandlerFeatures = queriesHandlerFeatures;
    this.featuresQuery = featuresQuery;
    this.serviceUri = servicesContext.getUri();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Tiles3dConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(Format3dTilesSubtree.class);
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_3D_TILES_SUBTREE);
    String subSubPath = "/3dtiles/subtrees/{level}/{x}/{y}";
    String path = "/collections/{collectionId}" + subSubPath;
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();
    if (!optCollectionIdParam.isPresent()) {
      LOGGER.error(
          "Path parameter 'collectionId' missing for resource at path '"
              + path
              + "'. The resource will not be available.");
    } else {
      final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
      final boolean explode = collectionIdParam.isExplodeInOpenApi(apiData);
      final List<String> collectionIds =
          (explode) ? collectionIdParam.getValues(apiData) : ImmutableList.of("{collectionId}");
      for (String collectionId : collectionIds) {
        List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId);
        String operationSummary =
            "retrieve a 3D Tiles subtree of the feature collection '" + collectionId + "'";
        Optional<String> operationDescription = Optional.of("TODO.");
        String resourcePath = "/collections/" + collectionId + subSubPath;
        ImmutableOgcApiResourceData.Builder resourceBuilder =
            new ImmutableOgcApiResourceData.Builder()
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
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }

    return definitionBuilder.build();
  }

  @GET
  @Path("/{collectionId}/3dtiles/subtrees/{level}/{x}/{y}")
  public Response getSubtree(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context UriInfo uriInfo,
      @PathParam("collectionId") String collectionId,
      @PathParam("level") String level,
      @PathParam("x") String x,
      @PathParam("y") String y) {

    Tiles3dConfiguration cfg =
        api.getData()
            .getCollectionData(collectionId)
            .flatMap(c -> c.getExtension(Tiles3dConfiguration.class))
            .orElseThrow();
    int availableLevels = Objects.requireNonNull(cfg.getAvailableLevels());
    int subtreeLevels = Objects.requireNonNull(cfg.getSubtreeLevels());

    int sl = Integer.parseInt(level);
    if (sl < 0 || sl >= availableLevels || sl % subtreeLevels != 0) {
      throw new NotFoundException();
    }
    long sx = Long.parseLong(x);
    if (sx < 0 || sx >= Math.pow(2, sl)) {
      throw new NotFoundException();
    }
    long sy = Long.parseLong(y);
    if (sy < 0 || sy >= Math.pow(2, sl)) {
      throw new NotFoundException();
    }

    QueryInputSubtree queryInput =
        ImmutableQueryInputSubtree.builder()
            .from(getGenericQueryInput(api.getData()))
            .api(api)
            .featureProvider(providers.getFeatureProviderOrThrow(api.getData()))
            .featureType(
                api.getData()
                    .getExtension(FeaturesCoreConfiguration.class, collectionId)
                    .flatMap(FeaturesCoreConfiguration::getFeatureType)
                    .orElse(collectionId))
            .geometryProperty(
                providers
                    .getFeatureSchema(
                        api.getData(), api.getData().getCollectionData(collectionId).orElseThrow())
                    .flatMap(SchemaBase::getPrimaryGeometry)
                    .map(SchemaBase::getFullPathAsString)
                    .orElseThrow())
            .servicesUri(serviceUri)
            .collectionId(collectionId)
            .level(sl)
            .x(sx)
            .y(sy)
            .build();

    return queryHandler.handle(Query.SUBTREE, queryInput, requestContext);
  }
}
