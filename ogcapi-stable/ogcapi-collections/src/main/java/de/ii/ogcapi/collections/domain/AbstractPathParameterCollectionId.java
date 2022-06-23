/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPathParameterCollectionId implements OgcApiPathParameter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractPathParameterCollectionId.class);

  public static final String COLLECTION_ID_PATTERN = "[\\w\\-]+";

  protected final Map<Integer, Boolean> apiExplodeMap;
  protected final Map<Integer, List<String>> apiCollectionMap;
  protected final SchemaValidator schemaValidator;

  public AbstractPathParameterCollectionId(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
    this.apiCollectionMap = new HashMap<>();
    this.apiExplodeMap = new HashMap<>();
  }

  public abstract boolean matchesPath(String definitionPath);

  @Override
  public String getPattern() {
    return COLLECTION_ID_PATTERN;
  }

  @Override
  public boolean isExplodeInOpenApi(OgcApiDataV2 apiData) {
    if (!apiExplodeMap.containsKey(apiData.hashCode())) {
      apiExplodeMap.put(
          apiData.hashCode(),
          !apiData
              .getExtension(CollectionsConfiguration.class)
              .get()
              .getCollectionIdAsParameter()
              .orElse(false));
    }

    return apiExplodeMap.get(apiData.hashCode());
  }

  @Override
  public List<String> getValues(OgcApiDataV2 apiData) {
    if (!apiCollectionMap.containsKey(apiData.hashCode())) {
      apiCollectionMap.put(
          apiData.hashCode(),
          apiData.getCollections().keySet().stream()
              .filter(collectionId -> apiData.isCollectionEnabled(collectionId))
              .collect(Collectors.toUnmodifiableList()));
    }

    return apiCollectionMap.get(apiData.hashCode());
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return new StringSchema()._enum(ImmutableList.copyOf(getValues(apiData)));
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public String getName() {
    return "collectionId";
  }

  @Override
  public String getDescription() {
    return "The local identifier of a feature collection.";
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CollectionsConfiguration.class;
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    return matchesPath(definitionPath) && isEnabledForApi(apiData);
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
    return matchesPath(definitionPath) && isEnabledForApi(apiData, collectionId);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData.getCollections().values().stream()
        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    final FeatureTypeConfigurationOgcApi collectionData =
        apiData.getCollections().get(collectionId);
    return OgcApiPathParameter.super.isEnabledForApi(apiData, collectionId)
        && Objects.nonNull(collectionData)
        && collectionData.getEnabled();
  }
}
