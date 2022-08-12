/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ogcapi.features.gltf.domain.Format3dTilesContent;
import de.ii.ogcapi.features.gltf.domain._3dTilesConfiguration;
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
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import io.dropwizard.auth.Auth;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class Endpoint3dTilesContent extends EndpointSubCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(Endpoint3dTilesContent.class);

  private static final List<String> TAGS = ImmutableList.of("Access data as 3D Tiles");

  private final FeaturesCoreProviders providers;
  private final FeaturesCoreQueriesHandler queriesHandlerFeatures;
  private final FeaturesQuery featuresQuery;

  @Inject
  public Endpoint3dTilesContent(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      FeaturesCoreQueriesHandler queriesHandlerFeatures,
      FeaturesQuery featuresQuery) {
    super(extensionRegistry);
    this.providers = providers;
    this.queriesHandlerFeatures = queriesHandlerFeatures;
    this.featuresQuery = featuresQuery;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return _3dTilesConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null) {
      formats =
          extensionRegistry.getExtensionsForType(FeatureFormatExtension.class).stream()
              .filter(f -> Format3dTilesContent.class.isAssignableFrom(f.getClass()))
              .collect(Collectors.toUnmodifiableList());
    }
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_3D_TILES_CONTENT);
    String subSubPath = "/3dtiles/content/{level}/{x}/{y}";
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
            "retrieve a glTF tile of the feature collection '" + collectionId + "'";
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
  @Path("/{collectionId}/3dtiles/content/{level}/{x}/{y}")
  public Response getContent(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context UriInfo uriInfo,
      @PathParam("collectionId") String collectionId,
      @PathParam("level") String level,
      @PathParam("x") String x,
      @PathParam("y") String y)
      throws URISyntaxException {

    Integer availableLevels =
        api.getData()
            .getCollectionData(collectionId)
            .flatMap(c -> c.getExtension(_3dTilesConfiguration.class))
            .map(_3dTilesConfiguration::getAvailableLevels)
            .orElseThrow();

    int cl = Integer.parseInt(level);
    if (cl < 0 || cl >= availableLevels) {
      throw new NotFoundException();
    }
    long cx = Long.parseLong(x);
    if (cx < 0 || cx >= Math.pow(2, cl)) {
      throw new NotFoundException();
    }
    long cy = Long.parseLong(y);
    if (cy < 0 || cy >= Math.pow(2, cl)) {
      throw new NotFoundException();
    }

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollectionData(collectionId).orElseThrow();

    BoundingBox bboxTile = Helper.computeBbox(api, collectionId, cl, cx, cy);

    String bboxString =
        String.format(
            Locale.US,
            "%f,%f,%f,%f",
            bboxTile.getXmin(),
            bboxTile.getYmin(),
            bboxTile.getXmax(),
            bboxTile.getYmax());

    FeatureQuery query =
        featuresQuery.requestToFeatureQuery(
            api,
            collectionData,
            OgcCrs.CRS84h,
            ImmutableMap.of(),
            1,
            100000, // TODO
            100000, // TODO
            ImmutableMap.of("bbox", bboxString),
            ImmutableList.of());
    FeaturesCoreQueriesHandler.QueryInputFeatures queryInput =
        new ImmutableQueryInputFeatures.Builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .query(query)
            .featureProvider(providers.getFeatureProviderOrThrow(api.getData(), collectionData))
            .defaultCrs(OgcCrs.CRS84h)
            .defaultPageSize(Optional.of(100000))
            .showsFeatureSelfLink(false)
            .build();

    ApiRequestContext requestContextGltf =
        new Builder()
            .from(requestContext)
            .requestUri(
                requestContext
                    .getUriCustomizer()
                    .removeLastPathSegments(5)
                    .ensureLastPathSegment("items")
                    .clearParameters()
                    .addParameter("f", "glb")
                    .addParameter("bbox", bboxString)
                    .build())
            .mediaType(FeaturesFormatGltfBinary.MEDIA_TYPE)
            .alternateMediaTypes(ImmutableList.of())
            .build();

    return queriesHandlerFeatures.handle(
        FeaturesCoreQueriesHandler.Query.FEATURES, queryInput, requestContextGltf);
  }
}
