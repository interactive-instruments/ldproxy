/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.crud.app.CommandHandlerCrud.QueryInputPutFeature;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema.Scope;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.dropwizard.auth.Auth;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @langEn The content of the request is a new feature in one of the supported encodings. The URI of
 *     the new feature is returned in the header `Location`.
 * @langDe TODO
 * @name Features
 * @path {apiId}/collection/{collectionId}/items
 * @format {@link de.ii.ogcapi.features.core.domain.FeatureFormatExtension}
 */

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class EndpointCrud extends EndpointSubCollection implements ConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointCrud.class);
  private static final List<String> TAGS = ImmutableList.of("Mutate data");

  private final FeaturesCoreProviders providers;
  private final CommandHandlerCrud commandHandler;

  @Inject
  public EndpointCrud(ExtensionRegistry extensionRegistry, FeaturesCoreProviders providers, CommandHandlerCrud commandHandler) {
    super(extensionRegistry);
    this.providers = providers;
    this.commandHandler = commandHandler;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CrudConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return super.isEnabledForApi(apiData)
        && providers
            .getFeatureProvider(apiData)
            .map(FeatureProvider2::supportsTransactions)
            .orElse(false);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return super.isEnabledForApi(apiData, collectionId)
        && providers
            .getFeatureProvider(apiData)
            .map(FeatureProvider2::supportsTransactions)
            .orElse(false);
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-features-4/1.0/req/create-replace-delete",
        "http://www.opengis.net/spec/ogcapi-features-4/1.0/req/optimistic-locking",
        "http://www.opengis.net/spec/ogcapi-features-4/1.0/req/features"
        //TODO
        // "http://www.opengis.net/spec/ogcapi-features-4/1.0/req/create-replace-delete/update"
        );
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(FeatureFormatExtension.class).stream()
              .filter(FeatureFormatExtension::canSupportTransactions)
              .collect(Collectors.toList());
    return formats;
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES_TRANSACTION);
    String subSubPath = "/items";
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
        final List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.POST);
        final List<ApiHeader> headers =
            getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.POST);
        final String operationSummary =
            "add a feature in the feature collection '" + collectionId + "'";
        Optional<String> operationDescription =
            Optional.of(
                "The content of the request is a new feature in one of the supported encodings. The URI of the new feature is returned in the header `Location`.");
        String resourcePath = "/collections/" + collectionId + subSubPath;
        ImmutableOgcApiResourceData.Builder resourceBuilder =
            new ImmutableOgcApiResourceData.Builder()
                .path(resourcePath)
                .pathParameters(pathParameters);
        Map<MediaType, ApiMediaTypeContent> requestContent =
            collectionId.startsWith("{")
                ? getRequestContent(apiData, Optional.empty(), subSubPath, HttpMethods.POST)
                : getRequestContent(
                    apiData, Optional.of(collectionId), subSubPath, HttpMethods.POST);
        ApiOperation.of(
                resourcePath,
                HttpMethods.POST,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.POST.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }
    subSubPath = "/items/{featureId}";
    path = "/collections/{collectionId}" + subSubPath;
    pathParameters = getPathParameters(extensionRegistry, apiData, path);
    optCollectionIdParam =
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
          explode ? collectionIdParam.getValues(apiData) : ImmutableList.of("{collectionId}");
      for (String collectionId : collectionIds) {
        List<OgcApiQueryParameter> queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.PUT);
        List<ApiHeader> headers =
            getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.PUT);
        String operationSummary =
            "add or update a feature in the feature collection '" + collectionId + "'";
        Optional<String> operationDescription =
            Optional.of(
                "The content of the request is a new feature in one of the supported encodings. The id of the new or updated feature is `{featureId}`.");
        String resourcePath = "/collections/" + collectionId + subSubPath;
        ImmutableOgcApiResourceData.Builder resourceBuilder =
            new ImmutableOgcApiResourceData.Builder()
                .path(resourcePath)
                .pathParameters(pathParameters);
        Map<MediaType, ApiMediaTypeContent> requestContent =
            collectionId.startsWith("{")
                ? getRequestContent(apiData, Optional.empty(), subSubPath, HttpMethods.PUT)
                : getRequestContent(
                    apiData, Optional.of(collectionId), subSubPath, HttpMethods.PUT);
        ApiOperation.of(
                resourcePath,
                HttpMethods.PUT,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.PUT.name(), operation));

        queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.DELETE);
        headers = getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.DELETE);
        operationSummary = "delete a feature in the feature collection '" + collectionId + "'";
        operationDescription = Optional.of("The feature with id `{featureId}` will be deleted.");
        ApiOperation.of(
                resourcePath,
                HttpMethods.DELETE,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                TAGS)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.DELETE.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }
    return definitionBuilder.build();
  }

  @Path("/{id}/items")
  @POST
  @Consumes("application/geo+json")
  public Response postItems(
      @Auth Optional<User> optionalUser,
      @PathParam("id") String id,
      @Context OgcApi service,
      @Context ApiRequestContext apiRequestContext,
      @Context HttpServletRequest request,
      InputStream requestBody) {
    FeatureProvider2 featureProvider =
        providers.getFeatureProviderOrThrow(
            service.getData(), service.getData().getCollections().get(id));

    checkTransactional(featureProvider);

    checkAuthorization(service.getData(), optionalUser);

    return commandHandler.postItemsResponse(
        (FeatureTransactions) featureProvider,
        apiRequestContext.getMediaType(),
        apiRequestContext.getUriCustomizer().copy(),
        id,
        requestBody);
  }

  @Path("/{collectionId}/items/{featureid}")
  @PUT
  @Consumes("application/geo+json")
  public Response putItem(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @PathParam("featureid") final String featureId,
      @Context OgcApi api,
      @Context ApiRequestContext apiRequestContext,
      @Context HttpServletRequest request,
      InputStream requestBody) {

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    FeatureProvider2 featureProvider =
        providers.getFeatureProviderOrThrow(
            api.getData(), api.getData().getCollections().get(collectionId));

    checkTransactional(featureProvider);

    checkAuthorization(api.getData(), optionalUser);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature)
                        != FeaturesCoreConfiguration.ItemType.unknown)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    FeatureQuery eTagQuery = ImmutableFeatureQuery.builder()
        .type(collectionId)
        .filter(In.of(ScalarLiteral.of(featureId)))
        .returnsSingleFeature(true)
        .crs(coreConfiguration.getDefaultEpsgCrs())
        .schemaScope(Scope.MUTATIONS)
        .build();

    QueryInputPutFeature queryInput =
        ImmutableQueryInputPutFeature.builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .featureType(coreConfiguration.getFeatureType().orElse(collectionId))
            .featureId(featureId)
            .query(eTagQuery)
            .featureProvider(featureProvider)
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .requestBody(requestBody)
            .build();

    return commandHandler.putItemResponse(
        queryInput,
        apiRequestContext);
  }

  @Path("/{id}/items/{featureid}")
  @DELETE
  public Response deleteItem(
      @Auth Optional<User> optionalUser,
      @Context OgcApi service,
      @PathParam("id") String id,
      @PathParam("featureid") final String featureId) {

    FeatureProvider2 featureProvider =
        providers.getFeatureProviderOrThrow(
            service.getData(), service.getData().getCollections().get(id));

    checkTransactional(featureProvider);

    checkAuthorization(service.getData(), optionalUser);

    return commandHandler.deleteItemResponse((FeatureTransactions) featureProvider, id, featureId);
  }

  private void checkTransactional(FeatureProvider2 featureProvider) {
    if (!featureProvider.supportsTransactions()) {
      throw new NotAllowedException("GET");
    }
  }
}
