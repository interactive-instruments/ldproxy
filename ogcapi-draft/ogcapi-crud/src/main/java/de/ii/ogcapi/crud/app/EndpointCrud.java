/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.GROUP_DATA_WRITE;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.crud.app.CommandHandlerCrud.QueryInputFeatureCreate;
import de.ii.ogcapi.crud.app.CommandHandlerCrud.QueryInputFeatureDelete;
import de.ii.ogcapi.crud.app.CommandHandlerCrud.QueryInputFeatureReplace;
import de.ii.ogcapi.features.core.domain.EndpointFeaturesDefinition;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ProfileFeatures;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiExtensionHealth;
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
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.ETag.Type;
import de.ii.xtraplatform.base.domain.resiliency.OptionalCapability;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entities.domain.ValidationResult;
import de.ii.xtraplatform.entities.domain.ValidationResult.MODE;
import de.ii.xtraplatform.features.domain.FeatureProvider;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.SchemaBase;
import io.dropwizard.auth.Auth;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
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
 * @title Features, Feature
 * @path collections/{collectionId}/items, collections/{collectionId}/items/{featureId}
 * @langEn Create, Replace, Update and Delete features.
 * @langDe Erzeugen, Ersetzen, Aktualisieren und Löschen von Features.
 */
@Singleton
@AutoBind
public class EndpointCrud extends EndpointSubCollection
    implements ConformanceClass, ApiExtensionHealth {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointCrud.class);
  private static final List<String> TAGS = ImmutableList.of("Mutate data");

  private final FeaturesCoreProviders providers;
  private final CommandHandlerCrud commandHandler;
  private final CrsInfo crsInfo;
  private final FeaturesQuery queryParser;
  private List<ProfileFeatures> crudProfiles;

  @Inject
  public EndpointCrud(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      CommandHandlerCrud commandHandler,
      CrsInfo crsInfo,
      FeaturesQuery queryParser) {
    super(extensionRegistry);
    this.providers = providers;
    this.commandHandler = commandHandler;
    this.crsInfo = crsInfo;
    this.queryParser = queryParser;
    this.crudProfiles = ImmutableList.of();
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
            .map(FeatureProvider::mutations)
            .filter(OptionalCapability::isSupported)
            .isPresent();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return super.isEnabledForApi(apiData, collectionId)
        && providers
            .getFeatureProvider(apiData, apiData.getCollections().get(collectionId))
            .map(FeatureProvider::mutations)
            .filter(OptionalCapability::isSupported)
            .isPresent();
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    ImmutableList.Builder<String> builder =
        new ImmutableList.Builder<String>()
            .add(
                "http://www.opengis.net/spec/ogcapi-features-4/0.0/conf/create-replace-delete",
                "http://www.opengis.net/spec/ogcapi-features-4/0.0/conf/update",
                "http://www.opengis.net/spec/ogcapi-features-4/0.0/conf/features");

    if (apiData.getCollections().values().stream()
        .anyMatch(
            cd ->
                cd.getExtension(CrudConfiguration.class)
                    .map(CrudConfiguration::supportsLastModified)
                    .orElse(false))) {
      builder.add(
          "http://www.opengis.net/spec/ogcapi-features-4/0.0/conf/optimistic-locking-timestamps");
    }

    if (apiData.getCollections().values().stream()
        .anyMatch(
            cd ->
                cd.getExtension(CrudConfiguration.class)
                    .map(CrudConfiguration::supportsEtag)
                    .orElse(false))) {
      builder.add(
          "http://www.opengis.net/spec/ogcapi-features-4/0.0/conf/optimistic-locking-etags");
    }

    return builder.build();
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats =
          extensionRegistry.getExtensionsForType(FeatureFormatExtension.class).stream()
              .filter(FeatureFormatExtension::canSupportTransactions)
              .collect(Collectors.toList());
    return formats;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    this.crudProfiles =
        extensionRegistry.getExtensionsForType(ProfileFeatures.class).stream()
            .filter(
                profile ->
                    "rel-as-key".equals(profile.getName())
                        || "val-as-code".equals(profile.getName()))
            .collect(Collectors.toList());

    return super.onStartup(api, apiValidation);
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
        Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData);
        ApiOperation.of(
                resourcePath,
                HttpMethods.POST,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                getOperationId(EndpointFeaturesDefinition.OP_ID_CREATE_ITEM, collectionId),
                GROUP_DATA_WRITE,
                TAGS,
                CrudBuildingBlock.MATURITY,
                CrudBuildingBlock.SPEC)
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
        Map<MediaType, ApiMediaTypeContent> requestContent = getRequestContent(apiData);
        ApiOperation.of(
                resourcePath,
                HttpMethods.PUT,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                getOperationId(EndpointFeaturesDefinition.OP_ID_REPLACE_ITEM, collectionId),
                GROUP_DATA_WRITE,
                TAGS,
                CrudBuildingBlock.MATURITY,
                CrudBuildingBlock.SPEC)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.PUT.name(), operation));

        queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.PATCH);
        headers = getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.PATCH);
        operationSummary = "update a feature in the feature collection '" + collectionId + "'";
        operationDescription =
            Optional.of(
                "The content of the request is a partial feature in one of the supported encodings. The id of updated feature is `{featureId}`.");
        requestContent = getRequestContent(apiData);
        ApiOperation.of(
                resourcePath,
                HttpMethods.PATCH,
                requestContent,
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                getOperationId(EndpointFeaturesDefinition.OP_ID_UPDATE_ITEM, collectionId),
                GROUP_DATA_WRITE,
                TAGS,
                CrudBuildingBlock.MATURITY,
                CrudBuildingBlock.SPEC)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.PATCH.name(), operation));

        queryParameters =
            getQueryParameters(extensionRegistry, apiData, path, collectionId, HttpMethods.DELETE);
        headers = getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.DELETE);
        operationSummary = "delete a feature in the feature collection '" + collectionId + "'";
        operationDescription = Optional.of("The feature with id `{featureId}` will be deleted.");
        ApiOperation.of(
                resourcePath,
                HttpMethods.DELETE,
                ImmutableMap.of(),
                queryParameters,
                headers,
                operationSummary,
                operationDescription,
                Optional.empty(),
                getOperationId(EndpointFeaturesDefinition.OP_ID_DELETE_ITEM, collectionId),
                GROUP_DATA_WRITE,
                TAGS,
                CrudBuildingBlock.MATURITY,
                CrudBuildingBlock.SPEC)
            .ifPresent(
                operation -> resourceBuilder.putOperations(HttpMethods.DELETE.name(), operation));
        definitionBuilder.putResources(resourcePath, resourceBuilder.build());
      }
    }
    return definitionBuilder.build();
  }

  @Path("/{collectionId}/items")
  @POST
  @Consumes("application/geo+json")
  public Response postItems(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @HeaderParam("Content-Crs") String crs,
      @Context OgcApi api,
      @Context ApiRequestContext apiRequestContext,
      @Context HttpServletRequest request,
      InputStream requestBody) {

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    FeatureProvider featureProvider =
        providers.getFeatureProviderOrThrow(api.getData(), collectionData);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature)
                        != FeaturesCoreConfiguration.ItemType.unknown)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    String featureType = coreConfiguration.getFeatureType().orElse(collectionId);

    QueryInputFeatureCreate queryInput =
        ImmutableQueryInputFeatureCreate.builder()
            .collectionId(collectionId)
            .featureType(featureType)
            .crs(
                Optional.ofNullable(crs)
                    .map(s -> s.substring(1, s.length() - 1))
                    .map(s -> EpsgCrs.fromString(s))
                    .orElseGet(coreConfiguration::getDefaultEpsgCrs))
            .featureProvider(featureProvider)
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .requestBody(requestBody)
            .build();

    return commandHandler.postItemsResponse(queryInput, apiRequestContext);
  }

  @Path("/{collectionId}/items/{featureId}")
  @PUT
  @Consumes("application/geo+json")
  public Response putItem(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @PathParam("featureId") final String featureId,
      @HeaderParam("Content-Crs") String crs,
      @HeaderParam("If-Match") String ifMatch,
      @HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince,
      @Context OgcApi api,
      @Context ApiRequestContext apiRequestContext,
      @Context HttpServletRequest request,
      InputStream requestBody) {

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    Optional<CrudConfiguration> crudConfiguration =
        collectionData.getExtension(CrudConfiguration.class);
    checkHeader(crudConfiguration, ifMatch, ifUnmodifiedSince);

    FeatureProvider featureProvider =
        providers.getFeatureProviderOrThrow(api.getData(), collectionData);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature)
                        != FeaturesCoreConfiguration.ItemType.unknown)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    String featureType = coreConfiguration.getFeatureType().orElse(collectionId);

    QueryParameterSet queryParameterSet = getQueryParameterSet(api, collectionData, crs);
    FeatureQuery query =
        queryParser.requestToFeatureQuery(
            api.getData(),
            collectionData,
            coreConfiguration.getDefaultEpsgCrs(),
            coreConfiguration.getCoordinatePrecision(),
            queryParameterSet,
            featureId,
            crudConfiguration.filter(CrudConfiguration::supportsEtag).map(ignore -> Type.STRONG),
            SchemaBase.Scope.RECEIVABLE);

    QueryInputFeatureReplace queryInput =
        ImmutableQueryInputFeatureReplace.builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .featureType(featureType)
            .featureId(featureId)
            .query(query)
            .queryParameterSet(queryParameterSet)
            .featureProvider(featureProvider)
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .requestBody(requestBody)
            .profiles(crudProfiles)
            .build();

    return commandHandler.putItemResponse(queryInput, apiRequestContext);
  }

  @Path("/{collectionId}/items/{featureId}")
  @PATCH
  @Consumes("application/geo+json")
  public Response patchItem(
      @Auth Optional<User> optionalUser,
      @PathParam("collectionId") String collectionId,
      @PathParam("featureId") final String featureId,
      @HeaderParam("Content-Crs") String crs,
      @HeaderParam("If-Match") String ifMatch,
      @HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince,
      @Context OgcApi api,
      @Context ApiRequestContext apiRequestContext,
      @Context HttpServletRequest request,
      InputStream requestBody) {

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    Optional<CrudConfiguration> crudConfiguration =
        collectionData.getExtension(CrudConfiguration.class);
    checkHeader(crudConfiguration, ifMatch, ifUnmodifiedSince);

    FeatureProvider featureProvider =
        providers.getFeatureProviderOrThrow(api.getData(), collectionData);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature)
                        != FeaturesCoreConfiguration.ItemType.unknown)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    String featureType = coreConfiguration.getFeatureType().orElse(collectionId);

    QueryParameterSet queryParameterSet = getQueryParameterSet(api, collectionData, crs);
    FeatureQuery query =
        queryParser.requestToFeatureQuery(
            api.getData(),
            collectionData,
            coreConfiguration.getDefaultEpsgCrs(),
            coreConfiguration.getCoordinatePrecision(),
            queryParameterSet,
            featureId,
            crudConfiguration.filter(CrudConfiguration::supportsEtag).map(ignore -> Type.STRONG),
            SchemaBase.Scope.RECEIVABLE);

    QueryInputFeatureReplace queryInput =
        ImmutableQueryInputFeatureReplace.builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .featureType(featureType)
            .featureId(featureId)
            .query(query)
            .queryParameterSet(queryParameterSet)
            .featureProvider(featureProvider)
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .requestBody(requestBody)
            .profiles(crudProfiles)
            .build();

    return commandHandler.patchItemResponse(queryInput, apiRequestContext);
  }

  @Path("/{collectionId}/items/{featureId}")
  @DELETE
  public Response deleteItem(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext apiRequestContext,
      @PathParam("collectionId") String collectionId,
      @PathParam("featureId") final String featureId,
      @HeaderParam("If-Match") String ifMatch,
      @HeaderParam("If-Unmodified-Since") String ifUnmodifiedSince) {

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    Optional<CrudConfiguration> crudConfiguration =
        collectionData.getExtension(CrudConfiguration.class);
    checkHeader(crudConfiguration, ifMatch, ifUnmodifiedSince);

    FeatureProvider featureProvider =
        providers.getFeatureProviderOrThrow(
            api.getData(), api.getData().getCollections().get(collectionId));

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.feature)
                        != FeaturesCoreConfiguration.ItemType.unknown)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    QueryParameterSet queryParameterSet = getQueryParameterSet(api, collectionData, null);
    FeatureQuery query =
        queryParser.requestToFeatureQuery(
            api.getData(),
            collectionData,
            coreConfiguration.getDefaultEpsgCrs(),
            coreConfiguration.getCoordinatePrecision(),
            queryParameterSet,
            featureId,
            crudConfiguration.filter(CrudConfiguration::supportsEtag).map(ignore -> Type.STRONG),
            SchemaBase.Scope.RECEIVABLE);

    QueryInputFeatureDelete queryInput =
        ImmutableQueryInputFeatureDelete.builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .featureId(featureId)
            .query(query)
            .queryParameterSet(queryParameterSet)
            .featureProvider(featureProvider)
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .profiles(crudProfiles)
            .build();

    return commandHandler.deleteItemResponse(queryInput, apiRequestContext);
  }

  private QueryParameterSet getQueryParameterSet(
      OgcApi api, FeatureTypeConfigurationOgcApi collectionData, String crs) {
    List<OgcApiQueryParameter> parameterDefinitions =
        getQueryParameters(
            extensionRegistry,
            api.getData(),
            "/collections/{collectionId}/items/{featureId}",
            collectionData.getId(),
            HttpMethods.GET);
    Map<String, String> values =
        Objects.nonNull(crs)
            ? ImmutableMap.of("schema", "receivables", "crs", crs.substring(1, crs.length() - 1))
            : ImmutableMap.of("schema", "receivables");
    return QueryParameterSet.of(parameterDefinitions, values)
        .evaluate(api, Optional.of(collectionData));
  }

  private static void checkHeader(
      Optional<CrudConfiguration> crudConfiguration, String ifMatch, String ifUnmodifiedSince) {
    if (crudConfiguration.map(CrudConfiguration::supportsEtag).orElse(false)
        && Objects.isNull(ifMatch)) {
      throw new BadRequestException(
          "Requests to change a feature for this collection must include an 'If-Match' header.");
    } else if (crudConfiguration.map(CrudConfiguration::supportsLastModified).orElse(false)
        && Objects.isNull(ifUnmodifiedSince)) {
      throw new BadRequestException(
          "Requests to change a feature for this collection must include an 'If-Unmodified-Since' header.");
    }
  }

  private ImmutableFeatureQuery.Builder processCoordinatePrecision(
      ImmutableFeatureQuery.Builder queryBuilder, Map<String, Integer> coordinatePrecision) {
    // check, if we need to add a precision value; for this we need the target CRS,
    // so we need to build the query to get the CRS
    ImmutableFeatureQuery query = queryBuilder.build();
    if (!coordinatePrecision.isEmpty() && query.getCrs().isPresent()) {
      List<Integer> precisionList =
          crsInfo.getPrecisionList(query.getCrs().get(), coordinatePrecision);
      if (!precisionList.isEmpty()) {
        queryBuilder.geometryPrecision(precisionList);
      }
    }
    return queryBuilder;
  }

  @Override
  public Set<Volatile2> getVolatiles(OgcApiDataV2 apiData) {
    return Set.of(
        commandHandler, crsInfo, queryParser, providers.getFeatureProviderOrThrow(apiData));
  }
}
