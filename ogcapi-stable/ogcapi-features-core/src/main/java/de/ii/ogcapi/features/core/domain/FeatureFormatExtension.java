/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@AutoMultiBind
public interface FeatureFormatExtension extends FormatExtension {

  ApiMediaType getCollectionMediaType();

  default EpsgCrs getContentCrs(EpsgCrs targetCrs) {
    return targetCrs;
  }

  default ApiMediaTypeContent getFeatureContent(
      OgcApiDataV2 apiData, Optional<String> collectionId, boolean featureCollection) {
    return getContent();
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
            .map(featuresCoreConfiguration -> featuresCoreConfiguration);

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

  default Optional<PropertyTransformations> getPropertyTransformations(
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<FeatureSchema> schema,
      List<ProfileFeatures> profiles) {
    if (profiles.isEmpty() || schema.isEmpty()) {
      return getPropertyTransformations(collectionData);
    }

    ImmutableProfileTransformations.Builder builder = new ImmutableProfileTransformations.Builder();

    schema.ifPresent(
        s ->
            profiles.forEach(
                profile ->
                    profile.addPropertyTransformations(
                        s, getMediaType().type().toString(), builder)));

    ProfileTransformations profileTransformations = builder.build();

    return Optional.of(
        getPropertyTransformations(collectionData)
            .map(pts -> pts.mergeInto(profileTransformations))
            .orElse(profileTransformations));
  }

  default boolean supportsHitsOnly() {
    return false;
  }

  default Optional<Long> getNumberMatched(Object content) {
    return Optional.empty();
  }

  default Optional<Long> getNumberReturned(Object content) {
    return Optional.empty();
  }
}
