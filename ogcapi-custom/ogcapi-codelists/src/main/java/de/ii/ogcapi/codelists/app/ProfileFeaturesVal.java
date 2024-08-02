/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.ImmutableProfileTransformations.Builder;
import de.ii.ogcapi.features.core.domain.ProfileFeatures;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;

public abstract class ProfileFeaturesVal extends ProfileFeatures {

  protected ProfileFeaturesVal(
      ExtensionRegistry extensionRegistry, FeaturesCoreProviders providers) {
    super(extensionRegistry, providers);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return super.isEnabledForApi(apiData) && usesCodedValue(apiData);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return super.isEnabledForApi(apiData) && usesCodedValue(apiData, collectionId);
  }

  @Override
  public void addPropertyTransformations(FeatureSchema schema, String mediaType, Builder builder) {
    schema.getAllNestedProperties().stream()
        .filter(p -> p.getConstraints().map(c -> c.getCodelist().isPresent()).orElse(false))
        .forEach(property -> addValTransformations(property, mediaType, builder));
  }

  protected abstract void addValTransformations(
      FeatureSchema property, String mediaType, Builder builder);

  private boolean usesCodedValue(OgcApiDataV2 apiData) {
    return apiData.getCollections().values().stream()
        .anyMatch(
            collectionData ->
                providers
                    .getFeatureSchema(apiData, collectionData)
                    .map(
                        schema ->
                            schema.getAllNestedProperties().stream()
                                .anyMatch(
                                    p ->
                                        p.getConstraints()
                                            .map(c -> c.getCodelist().isPresent())
                                            .orElse(false)))
                    .orElse(false));
  }

  private boolean usesCodedValue(OgcApiDataV2 apiData, String collectionId) {
    return apiData
        .getCollectionData(collectionId)
        .flatMap(
            collectionData ->
                providers
                    .getFeatureSchema(apiData, collectionData)
                    .map(
                        schema ->
                            schema.getAllNestedProperties().stream()
                                .anyMatch(
                                    p ->
                                        p.getConstraints()
                                            .map(c -> c.getCodelist().isPresent())
                                            .orElse(false))))
        .orElse(false);
  }

  static void mapToTitle(FeatureSchema property, Builder builder) {
    property
        .getConstraints()
        .flatMap(SchemaConstraints::getCodelist)
        .ifPresent(
            codelist -> {
              builder.putTransformations(
                  property.getFullPathAsString(),
                  ImmutableList.of(
                      new ImmutablePropertyTransformation.Builder().codelist(codelist).build()));
            });
  }
}
