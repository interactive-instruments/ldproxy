/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn TODO
 * @langDe TODO
 * @name clampToGround
 * @endpoints Features
 */
@Singleton
@AutoBind
public class QueryParameterClampToGround extends ApiExtensionCache implements OgcApiQueryParameter {

  private final FeaturesCoreProviders providers;
  private final SchemaValidator schemaValidator;

  @Inject
  public QueryParameterClampToGround(
      FeaturesCoreProviders providers, SchemaValidator schemaValidator) {
    this.providers = providers;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getId() {
    return "clampToGround";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && (definitionPath.equals("/collections/{collectionId}/items")
                    || definitionPath.equals("/collections/{collectionId}/items/{featureId}"))
                && method == HttpMethods.GET);
  }

  @Override
  public String getName() {
    return "clampToGround";
  }

  @Override
  public String getDescription() {
    return "TODO. Only affects glTF output.";
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    return new BooleanSchema();
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return super.isEnabledForApi(apiData, collectionId)
        && apiData.getCollections().containsKey(collectionId)
        && apiData.getCollections().get(collectionId).getEnabled()
        && apiData
            .getExtension(GltfConfiguration.class, collectionId)
            .map(GltfConfiguration::isEnabled)
            .orElse(false);
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return GltfConfiguration.class;
  }
}
