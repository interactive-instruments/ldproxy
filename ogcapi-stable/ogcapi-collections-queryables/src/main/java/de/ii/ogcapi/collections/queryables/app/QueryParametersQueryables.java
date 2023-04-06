/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.SchemaGeneratorOpenApi;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.RuntimeQueryParametersExtension;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class QueryParametersQueryables
    implements RuntimeQueryParametersExtension, ConformanceClass {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryParametersQueryables.class);

  final SchemaGeneratorOpenApi schemaGeneratorFeature;
  final FeaturesCoreProviders providers;
  final SchemaValidator schemaValidator;

  @Inject
  public QueryParametersQueryables(
      SchemaGeneratorOpenApi schemaGeneratorFeature,
      FeaturesCoreProviders providers,
      SchemaValidator schemaValidator) {
    this.schemaGeneratorFeature = schemaGeneratorFeature;
    this.providers = providers;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return QueryablesConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData.getCollections().keySet().stream()
        .anyMatch(collectionId -> isEnabledForApi(apiData, collectionId));
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    //noinspection deprecation
    return apiData
            .getExtension(QueryablesConfiguration.class, collectionId)
            .filter(QueryablesConfiguration::isEnabled)
            .filter(QueryablesConfiguration::provideAsQueryParameters)
            .isPresent()
        // also check deprecated configuration option
        || apiData
            .getExtension(FeaturesCoreConfiguration.class, collectionId)
            .filter(FeaturesCoreConfiguration::isEnabled)
            .filter(
                cfg -> cfg.getQueryables().map(FeaturesCollectionQueryables::getAll).isPresent())
            .isPresent();
  }

  @Override
  public List<OgcApiQueryParameter> getRuntimeParameters(
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      String definitionPath,
      HttpMethods method) {
    if (collectionId.isEmpty() || !"/collections/{collectionId}/items".equals(definitionPath)) {
      return ImmutableList.of();
    }

    Optional<QueryablesConfiguration> configuration =
        apiData.getExtension(QueryablesConfiguration.class, collectionId.get());
    if (!configuration.map(QueryablesConfiguration::provideAsQueryParameters).orElse(true)) {
      return ImmutableList.of();
    }

    FeatureTypeConfigurationOgcApi collectionData =
        apiData.getCollectionData(collectionId.get()).orElse(null);
    if (collectionData == null) {
      return ImmutableList.of();
    }

    FeatureSchema featureSchema = providers.getFeatureSchema(apiData, collectionData).orElse(null);
    if (featureSchema == null) {
      return ImmutableList.of();
    }

    return configuration
        .map(c -> c.getQueryables(collectionData, featureSchema))
        .orElse(ImmutableMap.of())
        .entrySet()
        .stream()
        .filter(queryable -> !queryable.getValue().isSpatial())
        .map(queryable -> getQueryable(apiData, collectionData, featureSchema, queryable))
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableList());
  }

  private QueryParameterTemplateQueryable getQueryable(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      FeatureSchema featureSchema,
      Map.Entry<String, FeatureSchema> queryableDefinition) {
    String field = queryableDefinition.getKey();
    Optional<Schema<?>> schema2 =
        schemaGeneratorFeature.getProperty(featureSchema, collectionData, field);
    if (schema2.isEmpty()) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Query parameter for property '{}' at path '/collections/{}/items' could not be created, the property was not found in the feature schema.",
            field,
            collectionData.getId());
      }
      return null;
    }
    StringBuilder description =
        new StringBuilder("Filter the collection by property '").append(field).append('\'');
    if (Objects.nonNull(schema2.get().getTitle()) && !schema2.get().getTitle().isEmpty()) {
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
        .collectionId(collectionData.getId())
        .name(field)
        .type(queryableDefinition.getValue().getType())
        .valueType(queryableDefinition.getValue().getValueType())
        .description(description.toString())
        .schema(schema2.get())
        .schemaValidator(schemaValidator)
        .build();
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-features-3/0.0/conf/queryables-query-parameters");
  }
}
