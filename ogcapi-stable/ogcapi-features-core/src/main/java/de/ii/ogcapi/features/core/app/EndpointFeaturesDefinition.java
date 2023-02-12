/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.collections.domain.ImmutableQueryParameterTemplateQueryable;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EndpointFeaturesDefinition extends EndpointSubCollection {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointFeaturesDefinition.class);
  private static final List<String> TAGS = ImmutableList.of("Access data");

  final SchemaGeneratorOpenApi schemaGeneratorFeature;
  final FeaturesCoreProviders providers;
  final SchemaValidator schemaValidator;

  public EndpointFeaturesDefinition(
      ExtensionRegistry extensionRegistry,
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      FeaturesCoreProviders providers,
      SchemaValidator schemaValidator) {
    super(extensionRegistry);
    this.schemaGeneratorFeature = schemaGeneratorFeature;
    this.providers = providers;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesCoreConfiguration.class;
  }

  @Override
  public List<? extends FormatExtension> getResourceFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
    return formats;
  }

  public Map<MediaType, ApiMediaTypeContent> getFeatureContent(
      List<? extends FormatExtension> formats,
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      boolean featureCollection) {
    return formats.stream()
        .filter(f -> f instanceof FeatureFormatExtension)
        .map(f -> (FeatureFormatExtension) f)
        .filter(
            f ->
                collectionId
                    .map(s -> f.isEnabledForApi(apiData, s))
                    .orElseGet(() -> f.isEnabledForApi(apiData)))
        .map(f -> f.getFeatureContent(apiData, collectionId, featureCollection))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
  }

  void generateDefinition(
      OgcApiDataV2 apiData,
      ImmutableApiEndpointDefinition.Builder definitionBuilder,
      ImmutableList<OgcApiQueryParameter> allQueryParameters,
      String subSubPath,
      String summary,
      String description,
      String logPrefix) {

    String path = "/collections/{collectionId}" + subSubPath;
    List<OgcApiPathParameter> pathParameters = getPathParameters(extensionRegistry, apiData, path);
    Optional<OgcApiPathParameter> optCollectionIdParam =
        pathParameters.stream().filter(param -> param.getName().equals("collectionId")).findAny();

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
            description,
            logPrefix);

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
          description,
          logPrefix);
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
      String description,
      String logPrefix) {

    final List<OgcApiQueryParameter> queryParameters1 =
        path.equals("/collections/{collectionId}/items")
            ? getQueryParametersWithQueryables(queryParameters, apiData, collectionId, logPrefix)
            : queryParameters.collect(Collectors.toList());
    final String operationSummary = summary + collectionId + "'";
    final Optional<String> operationDescription = Optional.of(description);
    String resourcePath = "/collections/" + collectionId + subSubPath;
    ImmutableOgcApiResourceData.Builder resourceBuilder =
        new ImmutableOgcApiResourceData.Builder().path(resourcePath).pathParameters(pathParameters);

    Map<MediaType, ApiMediaTypeContent> responseContent =
        getFeatureContent(
            getResourceFormats(),
            apiData,
            collectionId.startsWith("{") ? Optional.empty() : Optional.of(collectionId),
            path.equals("/collections/{collectionId}/items"));
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
      Stream<OgcApiQueryParameter> generalList,
      OgcApiDataV2 apiData,
      String collectionId,
      String logPrefix) {

    Optional<FeaturesCoreConfiguration> coreConfiguration =
        apiData.getExtension(FeaturesCoreConfiguration.class, collectionId);
    final List<String> filterableFields =
        coreConfiguration
            .map(FeaturesCoreConfiguration::getFilterParameters)
            .orElse(ImmutableList.of());

    Map<String, List<PropertyTransformation>> transformations;
    if (coreConfiguration.isPresent()) {
      transformations = coreConfiguration.get().getTransformations();
      // TODO
    }

    Optional<FeatureTypeConfigurationOgcApi> collectionData =
        apiData.getCollectionData(collectionId);
    Optional<FeatureSchema> featureSchema =
        collectionData.flatMap(cd -> providers.getFeatureSchema(apiData, cd));

    List<OgcApiQueryParameter> build =
        Stream.concat(
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
                          String description = "Filter the collection by property '" + field + "'";
                          if (Objects.nonNull(schema2.get().getTitle())
                              && !schema2.get().getTitle().isEmpty())
                            description += " (" + schema2.get().getTitle() + ")";
                          if (Objects.nonNull(schema2.get().getDescription())
                              && !schema2.get().getDescription().isEmpty())
                            description += ": " + schema2.get().getDescription();
                          else description += ".";
                          return new ImmutableQueryParameterTemplateQueryable.Builder()
                              .apiId(apiData.getId())
                              .collectionId(collectionId)
                              .name(field)
                              .description(description)
                              .schema(schema2.get())
                              .schemaValidator(schemaValidator)
                              .build();
                        })
                    .filter(Objects::nonNull))
            .collect(Collectors.toList());

    return build;
  }
}
