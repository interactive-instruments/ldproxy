/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.GROUP_DATA_READ;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.EndpointSubCollection;
import de.ii.ogcapi.collections.domain.ImmutableOgcApiResourceData;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.ParameterExtension;
import de.ii.ogcapi.foundation.domain.RuntimeQueryParametersExtension;
import java.util.Collection;
import java.util.Comparator;
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

  protected final FeaturesCoreProviders providers;

  public EndpointFeaturesDefinition(
      ExtensionRegistry extensionRegistry, FeaturesCoreProviders providers) {
    super(extensionRegistry);
    this.providers = providers;
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
        List<OgcApiQueryParameter> queryParameters =
            Stream.concat(
                    extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class).stream()
                        .filter(
                            param ->
                                param.isApplicable(apiData, path, collectionId, HttpMethods.GET))
                        .sorted(Comparator.comparing(ParameterExtension::getName)),
                    extensionRegistry
                        .getExtensionsForType(RuntimeQueryParametersExtension.class)
                        .stream()
                        .map(
                            extension ->
                                extension.getRuntimeParameters(
                                    apiData, Optional.of(collectionId), path, HttpMethods.GET))
                        .flatMap(Collection::stream))
                .collect(Collectors.toUnmodifiableList());

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
      List<OgcApiQueryParameter> queryParameters;
      List<ApiHeader> headers = getHeaders(extensionRegistry, apiData, path, null, HttpMethods.GET);

      if (representativeCollectionId.isPresent()) {
        String collectionId = representativeCollectionId.get();
        queryParameters =
            Stream.concat(
                    extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class).stream()
                        .filter(
                            param ->
                                param.isApplicable(apiData, path, collectionId, HttpMethods.GET))
                        .sorted(Comparator.comparing(ParameterExtension::getName)),
                    extensionRegistry
                        .getExtensionsForType(RuntimeQueryParametersExtension.class)
                        .stream()
                        .map(
                            extension ->
                                extension.getRuntimeParameters(
                                    apiData, Optional.of(collectionId), path, HttpMethods.GET))
                        .flatMap(Collection::stream))
                .collect(Collectors.toUnmodifiableList());
      } else {
        queryParameters =
            extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class).stream()
                .filter(param -> param.isApplicable(apiData, path, HttpMethods.GET))
                .sorted(Comparator.comparing(ParameterExtension::getName))
                .collect(Collectors.toUnmodifiableList());
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
      List<OgcApiQueryParameter> queryParameters,
      List<ApiHeader> headers,
      String collectionId,
      String summary,
      String description,
      String logPrefix) {

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
            queryParameters,
            headers,
            responseContent,
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId(
                subSubPath.contains("{featureId}") ? "getItem" : "getItems", collectionId),
            GROUP_DATA_READ,
            TAGS)
        .ifPresent(operation -> resourceBuilder.putOperations(HttpMethods.GET.name(), operation));

    definitionBuilder.putResources(resourcePath, resourceBuilder.build());
  }
}
