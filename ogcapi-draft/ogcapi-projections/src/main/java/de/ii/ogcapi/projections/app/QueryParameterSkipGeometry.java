/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.projections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeatureQueryParameter;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery.Builder;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title skipGeometry
 * @endpoints Features, Feature
 * @langEn Use this option to suppress geometries in the response.
 * @langDe Verwenden Sie diese Option, um Geometrien in der Antwort zu unterdrücken.
 */
@Singleton
@AutoBind
public class QueryParameterSkipGeometry extends ApiExtensionCache
    implements OgcApiQueryParameter, FeatureQueryParameter, TypedQueryParameter<Boolean> {

  private static final Schema<?> SCHEMA = new BooleanSchema()._default(false);
  private final SchemaValidator schemaValidator;

  @Inject
  QueryParameterSkipGeometry(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getId(String collectionId) {
    return String.format("%s_%s", getName(), collectionId);
  }

  @Override
  public String getName() {
    return "skipGeometry";
  }

  @Override
  public Boolean parse(
      String value,
      Map<String, Object> typedValues,
      OgcApi api,
      Optional<FeatureTypeConfigurationOgcApi> optionalCollectionData) {
    return "true".equalsIgnoreCase(value);
  }

  @Override
  public void applyTo(
      Builder queryBuilder,
      QueryParameterSet parameters,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData) {
    parameters.getValue(this).ifPresent(queryBuilder::skipGeometry);
  }

  @Override
  public String getDescription() {
    return "Use this option to exclude geometries from the response for each feature.";
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

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return SCHEMA;
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    return SCHEMA;
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return ProjectionsConfiguration.class;
  }
}
