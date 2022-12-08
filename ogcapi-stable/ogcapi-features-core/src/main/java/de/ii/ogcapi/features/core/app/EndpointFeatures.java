/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.collections.domain.ImmutableQueryParameterTemplateQueryable;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreValidation;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeature;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
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
import de.ii.ogcapi.foundation.domain.ParameterExtension;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.web.domain.ETag.Type;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.models.media.Schema;
import java.text.MessageFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

/**
 * @langEn The response is a document consisting of features in the collection. The features
 *     included in the response are determined by the server based on the query parameters of the
 *     request.
 *     <p>To support access to larger collections without overloading the client, the API supports
 *     paged access with links to the next page, if more features are selected that the page size.
 *     <p>The `bbox` and `datetime` parameter can be used to select only a subset of the features in
 *     the collection (the features that are in the bounding box or time interval). The `bbox`
 *     parameter matches all features in the collection that are not associated with a location,
 *     too. The `datetime` parameter matches all features in the collection that are not associated
 *     with a time stamp or interval, too. The `limit` parameter may be used to control the subset
 *     of the selected features that should be returned in the response, the page size. Each page
 *     may include information about the number of selected and returned features (`numberMatched`
 *     and `numberReturned`) as well as links to support paging (link relation `next`).
 *     <p>See the details of this operation for a description of additional query parameters
 *     supported by this resource.
 * @langDe Die Antwort ist ein Dokument, das aus den Featuresn der Collection besteht. Die in der
 *     Antwort enthaltenen Features werden vom Server auf der Grundlage der Abfrageparameter der
 *     Anfrage bestimmt.
 *     <p>Um den Zugriff auf größere Collections zu unterstützen, ohne den Client zu überlasten,
 *     unterstützt die API den seitenweisen Zugriff mit Links zur nächsten Seite, wenn mehr Features
 *     ausgewählt sind, als die Seitengröße zulässt.
 *     <p>Die Parameter `bbox` und `datetime` können verwendet werden, um nur eine Teilmenge der
 *     Features in der Collection auszuwählen (die Features, die sich in der Bounding Box oder im
 *     Zeitintervall befinden). Der Parameter "bbox" entspricht allen Featuresn in der Collection,
 *     die nicht mit einem Ort verbunden sind. Der Parameter `datetime` passt zu allen Featuresn in
 *     der Collection, die nicht mit einem Zeitstempel oder Zeitintervall verknüpft sind. Der
 *     Parameter `limit` kann verwendet werden, um die Teilmenge der ausgewählten Features, die in
 *     der Antwort zurückgegeben werden soll, die Seitengröße. Jede Seite kann enthalten
 *     Informationen über die Anzahl der ausgewählten und zurückgegebenen Features (`numberMatched`
 *     und `numberReturned`) sowie Links zur Unterstützung des Blätterns (Link Relation `next`).
 *     <p>Eine Beschreibung der zusätzlichen Abfrageparameter, die von dieser Ressource unterstützt
 *     werden, finden Sie in den Details dieser Operation.
 * @name Features
 * @path /{apiId}/collections/{collectionId}/items/{featureId}
 * @formats {@link de.ii.ogcapi.features.core.domain.FeatureFormatExtension}
 */
@Singleton
@AutoBind
public class EndpointFeatures extends EndpointSubCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointFeatures.class);
  private static final List<String> TAGS = ImmutableList.of("Access data");

  private final SchemaGeneratorOpenApi schemaGeneratorFeature;
  private final EntityRegistry entityRegistry;
  private final FeaturesCoreProviders providers;
  private final FeaturesQuery ogcApiFeaturesQuery;
  private final FeaturesCoreQueriesHandler queryHandler;
  private final FeaturesCoreValidation featuresCoreValidator;
  private final SchemaValidator schemaValidator;

  @Inject
  public EndpointFeatures(
      ExtensionRegistry extensionRegistry,
      EntityRegistry entityRegistry,
      FeaturesCoreProviders providers,
      FeaturesQuery ogcApiFeaturesQuery,
      FeaturesCoreQueriesHandler queryHandler,
      FeaturesCoreValidation featuresCoreValidator,
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      SchemaValidator schemaValidator) {
    super(extensionRegistry);
    this.entityRegistry = entityRegistry;
    this.providers = providers;
    this.ogcApiFeaturesQuery = ogcApiFeaturesQuery;
    this.queryHandler = queryHandler;
    this.featuresCoreValidator = featuresCoreValidator;
    this.schemaGeneratorFeature = schemaGeneratorFeature;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesCoreConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null) {
      formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
    }
    return formats;
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    ValidationResult result = super.onStartup(api, apiValidation);

    // no additional operational checks for now, only validation; we can stop, if no validation is
    // requested
    if (apiValidation == MODE.NONE) {
      return result;
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().from(result).mode(apiValidation);

    Map<String, FeatureSchema> featureSchemas = providers.getFeatureSchemas(api.getData());

    identifyCollectionsWithoutType(api, builder, featureSchemas);

    // get Features Core configurations to process
    Map<String, FeaturesCoreConfiguration> coreConfigs = getConfigurations(api);

    validateTransformationKeys(builder, featureSchemas, coreConfigs);

    validateQueryables(builder, featureSchemas, coreConfigs);

    validatePaging(builder, coreConfigs);

    validateCodelists(builder, coreConfigs);

    return builder.build();
  }

  private void validateCodelists(
      ImmutableValidationResult.Builder builder,
      Map<String, FeaturesCoreConfiguration> coreConfigs) {
    Set<String> codelists =
        entityRegistry.getEntitiesForType(Codelist.class).stream()
            .map(Codelist::getId)
            .collect(Collectors.toUnmodifiableSet());
    for (Map.Entry<String, FeaturesCoreConfiguration> entry : coreConfigs.entrySet()) {
      String collectionId = entry.getKey();
      for (Map.Entry<String, List<PropertyTransformation>> entry2 :
          entry.getValue().getTransformations().entrySet()) {
        String property = entry2.getKey();
        for (PropertyTransformation transformation : entry2.getValue()) {
          transformation.validate(builder, collectionId, property, codelists);
        }
      }
    }
  }

  private void validatePaging(
      ImmutableValidationResult.Builder builder,
      Map<String, FeaturesCoreConfiguration> coreConfigs) {
    for (Map.Entry<String, FeaturesCoreConfiguration> entry : coreConfigs.entrySet()) {
      String collectionId = entry.getKey();
      FeaturesCoreConfiguration config = entry.getValue();
      if (config.getMinimumPageSize() < 1) {
        builder.addStrictErrors(
            MessageFormat.format(
                "The minimum page size ''{0}'' in collection ''{1}'' is invalid, it must be a positive integer.",
                config.getMinimumPageSize(), collectionId));
      }
      if (config.getMinimumPageSize() > config.getMaximumPageSize()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "The minimum page size ''{0}'' in collection ''{1}'' is invalid, it cannot be greater than the maximum page size ''{2}''.",
                config.getMinimumPageSize(), collectionId, config.getMaximumPageSize()));
      }
      if (config.getMinimumPageSize() > config.getDefaultPageSize()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "The minimum page size ''{0}'' in collection ''{1}'' is invalid, it cannot be greater than the default page size ''{2}''.",
                config.getMinimumPageSize(), collectionId, config.getDefaultPageSize()));
      }
      if (config.getMaximumPageSize() < config.getDefaultPageSize()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "The maxmimum page size ''{0}'' in collection ''{1}'' is invalid, it must be at least the default page size ''{2}''.",
                config.getMaximumPageSize(), collectionId, config.getDefaultPageSize()));
      }
    }
  }

  private void validateQueryables(
      ImmutableValidationResult.Builder builder,
      Map<String, FeatureSchema> featureSchemas,
      Map<String, FeaturesCoreConfiguration> coreConfigs) {
    Map<String, Collection<String>> queryables =
        coreConfigs.entrySet().stream()
            .map(
                entry ->
                    new AbstractMap.SimpleImmutableEntry<>(
                        entry.getKey(),
                        entry
                            .getValue()
                            .getQueryables()
                            .orElse(FeaturesCollectionQueryables.of())
                            .getAll()))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    for (Map.Entry<String, Collection<String>> stringCollectionEntry :
        featuresCoreValidator.getInvalidPropertyKeys(queryables, featureSchemas).entrySet()) {
      for (String property : stringCollectionEntry.getValue()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "A queryable ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.",
                property, stringCollectionEntry.getKey()));
      }
    }
  }

  private void validateTransformationKeys(
      ImmutableValidationResult.Builder builder,
      Map<String, FeatureSchema> featureSchemas,
      Map<String, FeaturesCoreConfiguration> coreConfigs) {
    Map<String, Collection<String>> transformationKeys =
        coreConfigs.entrySet().stream()
            .map(
                entry ->
                    new AbstractMap.SimpleImmutableEntry<>(
                        entry.getKey(), entry.getValue().getTransformations().keySet()))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    for (Map.Entry<String, Collection<String>> stringCollectionEntry :
        featuresCoreValidator
            .getInvalidPropertyKeys(transformationKeys, featureSchemas)
            .entrySet()) {
      for (String property : stringCollectionEntry.getValue()) {
        builder.addStrictErrors(
            MessageFormat.format(
                "A transformation for property ''{0}'' in collection ''{1}'' is invalid, because the property was not found in the provider schema.",
                property, stringCollectionEntry.getKey()));
      }
    }
  }

  private void identifyCollectionsWithoutType(
      OgcApi api,
      ImmutableValidationResult.Builder builder,
      Map<String, FeatureSchema> featureSchemas) {
    List<String> invalidCollections =
        featuresCoreValidator.getCollectionsWithoutType(api.getData(), featureSchemas);
    for (String invalidCollection : invalidCollections) {
      builder.addStrictErrors(
          MessageFormat.format(
              "The Collection ''{0}'' is invalid, because its feature type was not found in the provider schema.",
              invalidCollection));
    }
  }

  private ImmutableMap<String, FeaturesCoreConfiguration> getConfigurations(OgcApi api) {
    return api.getData().getCollections().entrySet().stream()
        .map(
            entry -> {
              final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
              final FeaturesCoreConfiguration config =
                  collectionData.getExtension(FeaturesCoreConfiguration.class).orElse(null);
              if (Objects.isNull(config)) {
                return null;
              }
              return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), config);
            })
        .filter(Objects::nonNull)
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  protected ApiEndpointDefinition computeDefinition(OgcApiDataV2 apiData) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint("collections")
            .sortPriority(ApiEndpointDefinition.SORT_PRIORITY_FEATURES);
    ImmutableList<OgcApiQueryParameter> allQueryParameters =
        extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class).stream()
            .sorted(Comparator.comparing(ParameterExtension::getName))
            .collect(ImmutableList.toImmutableList());

    generateDefinition(
        apiData,
        definitionBuilder,
        allQueryParameters,
        "/items",
        "retrieve features in the feature collection '",
        "The response is a document consisting of features in the collection. "
            + "The features included in the response are determined by the server based on the query parameters of the request.\n\n"
            + "To support access to larger collections without overloading the client, the API supports paged access with links "
            + "to the next page, if more features are selected that the page size.\n\nThe `bbox` and `datetime` parameter can be "
            + "used to select only a subset of the features in the collection (the features that are in the bounding box or time interval). "
            + "The `bbox` parameter matches all features in the collection that are not associated with a location, too. "
            + "The `datetime` parameter matches all features in the collection that are not associated with a time stamp or interval, too. "
            + "The `limit` parameter may be used to control the subset of the selected features that should be returned in the response, "
            + "the page size. Each page may include information about the number of selected and returned features (`numberMatched` "
            + "and `numberReturned`) as well as links to support paging (link relation `next`).\n\nSee the details of this operation for "
            + "a description of additional query parameters supported by this resource.");

    generateDefinition(
        apiData,
        definitionBuilder,
        allQueryParameters,
        "/items/{featureId}",
        "retrieve a feature in the feature collection '",
        "Fetch the feature with id `{featureId}`.");

    return definitionBuilder.build();
  }

  private void generateDefinition(
      OgcApiDataV2 apiData,
      ImmutableApiEndpointDefinition.Builder definitionBuilder,
      ImmutableList<OgcApiQueryParameter> allQueryParameters,
      String subSubPath,
      String summary,
      String description) {

    String path = "/collections/{collectionId}" + subSubPath;
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> "collectionId".equals(param.getName())).findAny();

    if (optCollectionIdParam.isEmpty()) {
      LOGGER.error(
          "Path parameter 'collectionId' is missing for resource at path '{}'. The resource will not be available.",
          path);
      return;
    }

    final OgcApiPathParameter collectionIdParam = optCollectionIdParam.get();
    final boolean explode = collectionIdParam.isExplodeInOpenApi(apiData);

    if (explode) {
      for (String collectionId : collectionIdParam.getValues(apiData)) {
        Stream<OgcApiQueryParameter> queryParameters =
            allQueryParameters.stream()
                .filter(qp -> qp.isApplicable(apiData, path, collectionId, HttpMethods.GET));
        List<ApiHeader> headers =
            getHeaders(extensionRegistry, apiData, path, collectionId, HttpMethods.GET);

        generateCollectionDefinition(
            apiData,
            definitionBuilder,
            subSubPath,
            path,
            pathParameters,
            queryParameters,
            headers,
            collectionId,
            summary,
            description);

        // since the generation is quite expensive, check if the startup was interrupted
        // after every collection
        if (Thread.interrupted()) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    } else {
      Optional<String> representativeCollectionId = getRepresentativeCollectionId(apiData);
      Stream<OgcApiQueryParameter> queryParameters = allQueryParameters.stream();
      List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, null, HttpMethods.GET);

      if (representativeCollectionId.isPresent()) {
        String collectionId = representativeCollectionId.get();
        queryParameters =
            allQueryParameters.stream()
                .filter(qp -> qp.isApplicable(apiData, path, collectionId, HttpMethods.GET));
      }

      generateCollectionDefinition(
          apiData,
          definitionBuilder,
          subSubPath,
          path,
          pathParameters,
          queryParameters,
          headers,
          "{collectionId}",
          summary,
          description);
    }
  }

  private void generateCollectionDefinition(
      OgcApiDataV2 apiData,
      ImmutableApiEndpointDefinition.Builder definitionBuilder,
      String subSubPath,
      String path,
      List<OgcApiPathParameter> pathParameters,
      Stream<OgcApiQueryParameter> queryParameters,
      List<ApiHeader> headers,
      String collectionId,
      String summary,
      String description) {

    final List<OgcApiQueryParameter> queryParameters1 =
        "/collections/{collectionId}/items".equals(path)
            ? getQueryParametersWithQueryables(queryParameters, apiData, collectionId)
            : queryParameters.collect(Collectors.toList());
    final String operationSummary = summary + collectionId + "'";
    final Optional<String> operationDescription = Optional.of(description);
    String resourcePath = "/collections/" + collectionId + subSubPath;
    ImmutableOgcApiResourceData.Builder resourceBuilder =
        new ImmutableOgcApiResourceData.Builder().path(resourcePath).pathParameters(pathParameters);

    Map<MediaType, ApiMediaTypeContent> responseContent =
        collectionId.startsWith("{")
            ? getContent(apiData, Optional.empty(), subSubPath, HttpMethods.GET)
            : getContent(apiData, Optional.of(collectionId), subSubPath, HttpMethods.GET);
    ApiOperation.getResource(
            apiData,
            resourcePath,
            false,
            queryParameters1,
            headers,
            responseContent,
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId(
                subSubPath.contains("{featureId}") ? "getItem" : "getItems", collectionId),
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));

    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
  }

  private List<OgcApiQueryParameter> getQueryParametersWithQueryables(
      Stream<OgcApiQueryParameter> generalList, OgcApiDataV2 apiData, String collectionId) {

    Optional<FeaturesCoreConfiguration> coreConfiguration =
        apiData.getExtension(FeaturesCoreConfiguration.class, collectionId);
    final List<String> filterableFields =
        coreConfiguration
            .map(FeaturesCoreConfiguration::getFilterParameters)
            .orElse(ImmutableList.of());

    Optional<FeatureTypeConfigurationOgcApi> collectionData =
        apiData.getCollectionData(collectionId);
    Optional<FeatureSchema> featureSchema =
        collectionData.flatMap(cd -> providers.getFeatureSchema(apiData, cd));

    return Stream.concat(
            generalList,
            filterableFields.stream()
                .map(
                    field -> {
                      Optional<Schema<?>> schema2 =
                          featureSchema.flatMap(
                              fs ->
                                  schemaGeneratorFeature.getProperty(
                                      fs, collectionData.get(), field));
                      if (schema2.isEmpty()) {
                        LOGGER.warn(
                            "Query parameter for property '{}' at path '/collections/{}/items' could not be created, the property was not found in the feature schema.",
                            field,
                            collectionId);
                        return null;
                      }
                      StringBuilder description =
                          new StringBuilder("Filter the collection by property '")
                              .append(field)
                              .append('\'');
                      if (Objects.nonNull(schema2.get().getTitle())
                          && !schema2.get().getTitle().isEmpty()) {
                        description.append(" (").append(schema2.get().getTitle()).append(')');
                      }
                      if (Objects.nonNull(schema2.get().getDescription())
                          && !schema2.get().getDescription().isEmpty()) {
                        description.append(": ").append(schema2.get().getDescription());
                      } else {
                        description.append('.');
                      }
                      return new ImmutableQueryParameterTemplateQueryable.Builder()
                          .apiId(apiData.getId())
                          .collectionId(collectionId)
                          .name(field)
                          .description(description.toString())
                          .schema(schema2.get())
                          .schemaValidator(schemaValidator)
                          .build();
                    })
                .filter(Objects::nonNull))
        .collect(Collectors.toList());
  }

  @GET
  @Path("/{collectionId}/items")
  public Response getItems(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context UriInfo uriInfo,
      @PathParam("collectionId") String collectionId) {
    checkCollectionExists(api.getData(), collectionId);

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.FEATURE)
                        != FeaturesCoreConfiguration.ItemType.UNKNOWN)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        MessageFormat.format(
                            "Features are not supported in API ''{0}'', collection ''{1}''.",
                            api.getId(), collectionId)));

    int defaultPageSize = coreConfiguration.getDefaultPageSize();
    boolean showsFeatureSelfLink = coreConfiguration.getShowsFeatureSelfLink();

    List<OgcApiQueryParameter> allowedParameters =
        getQueryParameters(
            extensionRegistry, api.getData(), "/collections/{collectionId}/items", collectionId);
    FeatureQuery query =
        ogcApiFeaturesQuery.requestToFeaturesQuery(
            api,
            collectionData,
            coreConfiguration.getDefaultEpsgCrs(),
            coreConfiguration.getCoordinatePrecision(),
            defaultPageSize,
            toFlatMap(uriInfo.getQueryParameters()),
            allowedParameters);
    FeaturesCoreQueriesHandler.QueryInputFeatures queryInput =
        new ImmutableQueryInputFeatures.Builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .query(query)
            .featureProvider(providers.getFeatureProviderOrThrow(api.getData(), collectionData))
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
            .defaultPageSize(Optional.of(defaultPageSize))
            .showsFeatureSelfLink(showsFeatureSelfLink)
            .build();

    return queryHandler.handle(
        FeaturesCoreQueriesHandlerImpl.Query.FEATURES, queryInput, requestContext);
  }

  @GET
  @Path("/{collectionId}/items/{featureId}")
  public Response getItem(
      @Auth Optional<User> optionalUser,
      @Context OgcApi api,
      @Context ApiRequestContext requestContext,
      @Context UriInfo uriInfo,
      @PathParam("collectionId") String collectionId,
      @PathParam("featureId") String featureId) {
    checkCollectionExists(api.getData(), collectionId);

    FeatureTypeConfigurationOgcApi collectionData =
        api.getData().getCollections().get(collectionId);

    FeaturesCoreConfiguration coreConfiguration =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .filter(
                cfg ->
                    cfg.getItemType().orElse(FeaturesCoreConfiguration.ItemType.FEATURE)
                        != FeaturesCoreConfiguration.ItemType.UNKNOWN)
            .orElseThrow(() -> new NotFoundException("Features are not supported for this API."));

    List<OgcApiQueryParameter> allowedParameters =
        getQueryParameters(
            extensionRegistry,
            api.getData(),
            "/collections/{collectionId}/items/{featureId}",
            collectionId);
    FeatureQuery query =
        ogcApiFeaturesQuery.requestToFeatureQuery(
            api.getData(),
            collectionData,
            coreConfiguration.getDefaultEpsgCrs(),
            coreConfiguration.getCoordinatePrecision(),
            toFlatMap(uriInfo.getQueryParameters()),
            allowedParameters,
            featureId,
            Optional.of(Type.STRONG));

    ImmutableQueryInputFeature.Builder queryInputBuilder =
        new ImmutableQueryInputFeature.Builder()
            .from(getGenericQueryInput(api.getData()))
            .collectionId(collectionId)
            .featureId(featureId)
            .query(query)
            .featureProvider(providers.getFeatureProviderOrThrow(api.getData(), collectionData))
            .defaultCrs(coreConfiguration.getDefaultEpsgCrs());

    if (Objects.nonNull(coreConfiguration.getCaching())
        && Objects.nonNull(coreConfiguration.getCaching().getCacheControlItems())) {
      queryInputBuilder.cacheControl(coreConfiguration.getCaching().getCacheControlItems());
    }

    return queryHandler.handle(
        FeaturesCoreQueriesHandler.Query.FEATURE, queryInputBuilder.build(), requestContext);
  }
}
