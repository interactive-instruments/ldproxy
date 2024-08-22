/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.codelists.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.ImmutableProfileTransformations.Builder;
import de.ii.ogcapi.features.core.domain.ProfileExtensionFeatures;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class ProfileExtensionExtensionFeaturesVal extends ProfileExtensionFeatures {

  public static final String VAL = "val";
  public static final String AS_CODE = "val-as-code";
  public static final String AS_TITLE = "val-as-title";

  @Inject
  public ProfileExtensionExtensionFeaturesVal(
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
  public String getPrefix() {
    return VAL;
  }

  @Override
  public List<String> getValues() {
    return List.of(AS_CODE, AS_TITLE);
  }

  @Override
  public List<String> getSupportedValues(boolean complex, boolean humanReadable) {
    return getValues();
  }

  @Override
  public String getDefaultValue(boolean complex, boolean humanReadable) {
    if (humanReadable) {
      return AS_TITLE;
    }

    return AS_CODE;
  }

  @Override
  public void addPropertyTransformations(
      String value, FeatureSchema schema, String mediaType, Builder builder) {
    if (!getValues().contains(value)) {
      return;
    }

    if (AS_TITLE.equals(value)) {
      schema.getAllNestedProperties().stream()
          .filter(p -> p.getConstraints().map(c -> c.getCodelist().isPresent()).orElse(false))
          .forEach(property -> mapToTitle(property, builder));
    }
  }

  private boolean usesCodedValue(OgcApiDataV2 apiData) {
    return apiData.getCollections().keySet().stream()
        .anyMatch(collectionId -> usesCodedValue(apiData, collectionId));
  }

  private boolean usesCodedValue(OgcApiDataV2 apiData, String collectionId) {
    // only consider codelist transformations in the provider constraints as the other
    // transformations are fixed and cannot be disabled.
    return apiData
        .getCollectionData(collectionId)
        .map(
            collectionData ->
                providers
                    .getFeatureSchema(apiData, collectionData)
                    .map(
                        schema ->
                            schema.getAllNestedProperties().stream()
                                .anyMatch(
                                    p ->
                                        p.getConstraints()
                                            .flatMap(SchemaConstraints::getCodelist)
                                            .isPresent()))
                    .orElse(false))
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
