/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
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

  default boolean supportsProfile(Profile profile) {
    return profile == Profile.AS_KEY || profile == Profile.AS_URI;
  }

  default Profile negotiateProfile(Profile profile) {
    if (supportsProfile(profile)) {
      return profile;
    } else if (supportsProfile(Profile.AS_LINK)) {
      return Profile.AS_LINK;
    } else if (supportsProfile(Profile.AS_KEY)) {
      return Profile.AS_KEY;
    } else if (supportsProfile(Profile.AS_URI)) {
      return Profile.AS_URI;
    }

    throw new IllegalStateException(
        String.format("Format '%s' does not support any profile.", getMediaType().label()));
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
      Profile profile) {
    if (schema.isEmpty()) {
      return getPropertyTransformations(collectionData);
    }

    ImmutableProfileTransformations.Builder builder = new ImmutableProfileTransformations.Builder();
    switch (profile) {
      default:
      case AS_KEY:
        return getPropertyTransformations(collectionData);
      case AS_URI:
        schema
            .map(SchemaBase::getAllNestedProperties)
            .ifPresent(
                properties ->
                    properties.stream()
                        .filter(SchemaBase::isFeatureRef)
                        .forEach(
                            property ->
                                getTemplate(property)
                                    .ifPresent(
                                        template ->
                                            builder.putTransformations(
                                                property.getFullPathAsString(),
                                                ImmutableList.of(
                                                    new ImmutablePropertyTransformation.Builder()
                                                        .stringFormat(template)
                                                        .build())))));
        break;
      case AS_LINK:
        schema
            .map(SchemaBase::getAllNestedProperties)
            .ifPresent(
                properties ->
                    properties.stream()
                        .filter(SchemaBase::isFeatureRef)
                        .forEach(
                            property ->
                                getTemplate(property)
                                    .ifPresent(
                                        template ->
                                            builder.putTransformations(
                                                property.getFullPathAsString(),
                                                ImmutableList.of(
                                                    new ImmutablePropertyTransformation.Builder()
                                                        // TODO: asLink not yet implemented
                                                        .asLink(template)
                                                        .build())))));
        break;
    }

    ProfileTransformations profileTransformations = builder.build();
    return Optional.of(
        getPropertyTransformations(collectionData)
            .map(pts -> pts.mergeInto(profileTransformations))
            .orElse(profileTransformations));
  }

  static Optional<String> getTemplate(FeatureSchema property) {
    return Optional.ofNullable(
        property
            .getRefUriTemplate()
            .orElse(
                property
                    .getRefType()
                    .map(
                        refType ->
                            String.format("{{apiUri}}/collections/%s/items/{{value}}", refType))
                    .orElse(null)));
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
