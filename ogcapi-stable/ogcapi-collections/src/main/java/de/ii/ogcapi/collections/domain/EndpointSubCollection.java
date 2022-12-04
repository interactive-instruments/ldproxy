/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiHeader;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.ParameterExtension;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

public abstract class EndpointSubCollection extends Endpoint {

  // NOTE: If there would be a need for collection-specific path parameters or headers, the API
  // definitions would need to be adapted for this

  public static final String COLLECTION_ID_TEMPLATE = "{collectionId}";
  public static final String COLLECTIONS_PATH_START = "/collections/";

  public EndpointSubCollection(ExtensionRegistry extensionRegistry) {
    super(extensionRegistry);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData.getCollections().values().stream()
        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    FeatureTypeConfigurationOgcApi configuration = apiData.getCollections().get(collectionId);
    return super.isEnabledForApi(apiData, collectionId)
        && Objects.nonNull(configuration)
        && configuration.getEnabled();
  }

  public Map<MediaType, ApiMediaTypeContent> getContent(
      OgcApiDataV2 apiData, Optional<String> collectionId, String subSubPath, HttpMethods method) {
    return getFormats().stream()
        .filter(
            outputFormatExtension ->
                collectionId
                    .map(s -> outputFormatExtension.isEnabledForApi(apiData, s))
                    .orElseGet(() -> outputFormatExtension.isEnabledForApi(apiData)))
        .map(
            f ->
                f.getContent(
                    apiData,
                    COLLECTIONS_PATH_START
                        + collectionId.orElse(COLLECTION_ID_TEMPLATE)
                        + subSubPath,
                    method))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
  }

  protected Map<MediaType, ApiMediaTypeContent> getRequestContent(
      OgcApiDataV2 apiData, Optional<String> collectionId, String subSubPath, HttpMethods method) {
    return getFormats().stream()
        .filter(
            outputFormatExtension ->
                collectionId
                    .map(s -> outputFormatExtension.isEnabledForApi(apiData, s))
                    .orElseGet(() -> outputFormatExtension.isEnabledForApi(apiData)))
        .map(
            f ->
                f.getRequestContent(
                    apiData,
                    COLLECTIONS_PATH_START
                        + collectionId.orElse(COLLECTION_ID_TEMPLATE)
                        + subSubPath,
                    method))
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(c -> c.getOgcApiMediaType().type(), c -> c));
  }

  protected void checkCollectionExists(
      @Context OgcApiDataV2 apiData, @PathParam("collectionId") String collectionId) {
    if (!apiData.isCollectionEnabled(collectionId)) {
      throw new NotFoundException(
          MessageFormat.format("The collection ''{0}'' does not exist in this API.", collectionId));
    }
  }

  public ImmutableList<OgcApiQueryParameter> getQueryParameters(
      ExtensionRegistry extensionRegistry,
      OgcApiDataV2 apiData,
      String definitionPath,
      String collectionId) {
    return getQueryParameters(
        extensionRegistry, apiData, definitionPath, collectionId, HttpMethods.GET);
  }

  public ImmutableList<OgcApiQueryParameter> getQueryParameters(
      ExtensionRegistry extensionRegistry,
      OgcApiDataV2 apiData,
      String definitionPath,
      String collectionId,
      HttpMethods method) {
    Optional<String> representativeCollectionId = Optional.empty();
    if (COLLECTION_ID_TEMPLATE.equals(collectionId)) {
      representativeCollectionId = getRepresentativeCollectionId(apiData);
      if (representativeCollectionId.isEmpty()) {
        return getQueryParameters(extensionRegistry, apiData, definitionPath, method);
      }
    }
    final String finalCollectionId = representativeCollectionId.orElse(collectionId);
    return extensionRegistry.getExtensionsForType(OgcApiQueryParameter.class).stream()
        .filter(param -> param.isApplicable(apiData, definitionPath, finalCollectionId, method))
        .sorted(Comparator.comparing(ParameterExtension::getName))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<ApiHeader> getHeaders(
      ExtensionRegistry extensionRegistry,
      OgcApiDataV2 apiData,
      String definitionPath,
      @SuppressWarnings("unused") String collectionId,
      HttpMethods method) {
    return getHeaders(extensionRegistry, apiData, definitionPath, method);
  }

  protected Optional<String> getRepresentativeCollectionId(OgcApiDataV2 apiData) {
    if (apiData
        .getExtension(CollectionsConfiguration.class)
        .filter(config -> config.getCollectionDefinitionsAreIdentical().orElse(false))
        .isPresent()) {
      return Optional.ofNullable(apiData.getCollections().keySet().iterator().next());
    }

    return Optional.empty();
  }

  @Override
  protected Optional<String> getOperationId(String name, String... prefixes) {
    // prefixes is never empty and the first prefix is the collectionId or the collectionId template
    if (COLLECTION_ID_TEMPLATE.equals(prefixes[0])) {
      prefixes[0] = "collection";
    }
    return super.getOperationId(name, prefixes);
  }
}
