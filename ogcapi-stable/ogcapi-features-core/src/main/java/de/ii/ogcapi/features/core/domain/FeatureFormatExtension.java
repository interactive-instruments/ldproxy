/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import de.ii.ogcapi.features.core.app.PathParameterFeatureIdFeatures;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Locale;
import java.util.Optional;

public interface FeatureFormatExtension extends FormatExtension {

  default String getPathPattern() {
    return "^/?collections/"
        + COLLECTION_ID_PATTERN
        + "/items(?:/"
        + PathParameterFeatureIdFeatures.FEATURE_ID_PATTERN
        + ")?$";
  }

  ApiMediaType getCollectionMediaType();

  @Override
  default ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path, HttpMethods method) {
    if (method.equals(HttpMethods.GET)) {
      return getContent(apiData, path);
    }
    return null;
  }

  default boolean canPassThroughFeatures() {
    return false;
  }

  default boolean canEncodeFeatures() {
    return false;
  }

  default Optional<FeatureTokenEncoder<?>> getFeatureEncoderPassThrough(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    return Optional.empty();
  }

  default Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    return Optional.empty();
  }

  default Optional<PropertyTransformations> getPropertyTransformations(
      FeatureTypeConfigurationOgcApi collectionData) {

    Optional<PropertyTransformations> coreTransformations =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .map(
                featuresCoreConfiguration -> ((PropertyTransformations) featuresCoreConfiguration));

    Optional<PropertyTransformations> formatTransformations =
        collectionData
            .getExtension(this.getBuildingBlockConfigurationType())
            .filter(
                buildingBlockConfiguration ->
                    buildingBlockConfiguration instanceof PropertyTransformations)
            .map(
                buildingBlockConfiguration ->
                    ((PropertyTransformations) buildingBlockConfiguration));

    return formatTransformations
        .map(ft -> coreTransformations.map(ft::mergeInto).orElse(ft))
        .or(() -> coreTransformations);
  }
}
