/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeatureQueryTransformer;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.EnumSchema;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.features.domain.FeatureSchemaBase;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn This option can be used to return features with a different schema. The only supported
 *     value is `receivables`, the response is the schema of the features that has to be met when
 *     creating or replacing a feature.
 * @langEn Diese Option kann verwendet werden, um Features in einem anderen Schema zurückzugeben.
 *     Der einzige unterstützte Wert ist `receivables`. Die Antwort ist dann in dem Schema, das beim
 *     Erstellen oder Ersetzen eines Features verwendet werden muss.
 * @title schema
 * @endpoints Feature
 */
@Singleton
@AutoBind
public class QueryParameterSchemaFeatures extends ApiExtensionCache
    implements OgcApiQueryParameter, FeatureQueryTransformer {

  private final SchemaValidator schemaValidator;

  @Inject
  QueryParameterSchemaFeatures(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getName() {
    return "schema";
  }

  @Override
  public String getDescription() {
    return "This option can be used to return features with a different schema.";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && (definitionPath.equals("/collections/{collectionId}/items")
                    || definitionPath.equals("/collections/{collectionId}/items/{featureId}")));
  }

  private final Schema<?> schema = new EnumSchema("receivables").example("receivables");

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return schema;
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    return schema;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return CrudConfiguration.class;
  }

  @Override
  public ImmutableFeatureQuery.Builder transformQuery(
      ImmutableFeatureQuery.Builder queryBuilder,
      Map<String, String> parameters,
      OgcApiDataV2 datasetData,
      FeatureTypeConfigurationOgcApi featureTypeConfiguration) {
    if (!isExtensionEnabled(
        datasetData.getCollections().get(featureTypeConfiguration.getId()),
        CrudConfiguration.class)) {
      return queryBuilder;
    }
    if (parameters.containsKey(getName())
        && Objects.equals(parameters.get(getName()), "receivables")) {
      try {
        queryBuilder.schemaScope(FeatureSchemaBase.Scope.MUTATIONS);
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    return queryBuilder;
  }
}
