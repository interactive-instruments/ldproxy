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
import com.google.common.io.ByteStreams;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
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
import de.ii.ogcapi.tiles3d.domain.ImmutableTileResource;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles;
import de.ii.ogcapi.tiles3d.domain.TileResource;
import de.ii.ogcapi.tiles3d.domain.TileResource.TYPE;
import de.ii.ogcapi.tiles3d.domain.TileResourceCache;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.cql.domain.Cql;
import io.dropwizard.auth.Auth;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final QueriesHandler3dTiles queryHandler;
  private final Cql cql;
  private final TileResourceCache tileResourceCache;

  @Inject
  public Endpoint3dTilesContent(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      FeaturesCoreQueriesHandler queriesHandlerFeatures,
      FeaturesQuery featuresQuery,
      QueriesHandler3dTiles queryHandler,
      Cql cql,
      TileResourceCache tileResourceCache) {
    super(extensionRegistry);
    this.providers = providers;
    this.queriesHandlerFeatures = queriesHandlerFeatures;
    this.featuresQuery = featuresQuery;
    this.queryHandler = queryHandler;
    this.cql = cql;
    this.tileResourceCache = tileResourceCache;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Tiles3dConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null) {
      formats =
          extensionRegistry.getExtensionsForType(FeatureFormatExtension.class).stream()
              // TODO better solution
              .filter(f -> f.getClass().getSimpleName().equals("FeaturesFormatGltfBinary"))
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
    String subSubPath = "/3dtiles/content_{level}_{x}_{y}";
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
  @Path("/{collectionId}/3dtiles/content_{level}_{x}_{y}")
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

    Tiles3dConfiguration cfg =
        api.getData()
            .getCollectionData(collectionId)
            .flatMap(c -> c.getExtension(Tiles3dConfiguration.class))
            .orElseThrow();

    int maxLevel = cfg.getMaxLevel();

    int cl = Integer.parseInt(level);
    if (cl < 0 || cl > maxLevel) {
      throw new NotFoundException();
    }
    int cx = Integer.parseInt(x);
    if (cx < 0 || cx >= Math.pow(2, cl)) {
      throw new NotFoundException();
    }
    int cy = Integer.parseInt(y);
    if (cy < 0 || cy >= Math.pow(2, cl)) {
      throw new NotFoundException();
    }

    int firstLevelWithContent = Objects.requireNonNull(cfg.getFirstLevelWithContent());
    if (cl < firstLevelWithContent) {
      throw new NotFoundException();
    }

    TileResource r =
        new ImmutableTileResource.Builder()
            .level(cl)
            .x(cx)
            .y(cy)
            .api(api)
            .collectionId(collectionId)
            .type(TYPE.CONTENT)
            .build();

    byte[] result = null;

    try {
      if (tileResourceCache.tileResourceExists(r)) {
        Optional<InputStream> content = tileResourceCache.getTileResource(r);
        if (content.isPresent()) {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ByteStreams.copy(content.get(), baos);
          result = baos.toByteArray();
          // TODO other processing and headers
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    if (Objects.isNull(result)) {
      return Tiles3dHelper.getContent(
          featuresQuery,
          providers,
          queriesHandlerFeatures,
          tileResourceCache,
          requestContext.getUriCustomizer(),
          cfg,
          r,
          cql,
          Optional.of(getGenericQueryInput(api.getData())),
          // TODO better solution
          getFormats().get(0).getMediaType());
    }

    return Response.ok().entity(result).build();
  }
}
