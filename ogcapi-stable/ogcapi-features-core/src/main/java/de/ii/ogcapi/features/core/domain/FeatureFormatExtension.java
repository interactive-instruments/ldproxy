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
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.transform.FeatureRefResolver;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@AutoMultiBind
public interface FeatureFormatExtension extends FormatExtension {

  String URI_TEMPLATE =
      String.format(
          "{{%s | orElse:'{{apiUri}}/collections/%s/items/%s'}}",
          FeatureRefResolver.URI_TEMPLATE, FeatureRefResolver.SUB_TYPE, FeatureRefResolver.SUB_ID);

  String KEY_TEMPLATE =
      String.format(
          "{{%s | orElse:'%s::%s'}}",
          FeatureRefResolver.KEY_TEMPLATE, FeatureRefResolver.SUB_TYPE, FeatureRefResolver.SUB_ID);

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

  default boolean supportsEmbedding() {
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
      Optional<Profile> profile) {
    if (profile.isEmpty() || schema.isEmpty()) {
      return getPropertyTransformations(collectionData);
    }

    ImmutableProfileTransformations.Builder builder = new ImmutableProfileTransformations.Builder();

    schema
        .map(SchemaBase::getAllNestedProperties)
        .ifPresent(
            properties ->
                properties.stream()
                    .filter(SchemaBase::isFeatureRef)
                    .forEach(
                        property -> {
                          switch (profile.get()) {
                            default:
                            case AS_KEY:
                              reduceToKey(property, builder);
                              break;
                            case AS_URI:
                              reduceToUri(property, builder);
                              break;
                            case AS_LINK:
                              mapToLink(property, builder);
                              break;
                          }
                        }));

    ProfileTransformations profileTransformations = builder.build();
    return Optional.of(
        getPropertyTransformations(collectionData)
            .map(pts -> pts.mergeInto(profileTransformations))
            .orElse(profileTransformations));
  }

  static void reduceToKey(FeatureSchema schema, ImmutableProfileTransformations.Builder builder) {
    builder.putTransformations(
        schema.getFullPathAsString(),
        ImmutableList.of(
            schema
                        .getRefType()
                        .filter(
                            refType ->
                                !Objects.equals(refType, FeatureRefResolver.REF_TYPE_DYNAMIC))
                        .isPresent()
                    && schema.getRefKeyTemplate().isEmpty()
                ? new ImmutablePropertyTransformation.Builder()
                    .objectRemoveSelect(FeatureRefResolver.ID)
                    .objectReduceSelect(FeatureRefResolver.ID)
                    .build()
                : new ImmutablePropertyTransformation.Builder()
                    .objectRemoveSelect(FeatureRefResolver.ID)
                    .objectReduceFormat(KEY_TEMPLATE)
                    .build()));
  }

  static void reduceToUri(FeatureSchema schema, ImmutableProfileTransformations.Builder builder) {
    builder.putTransformations(
        schema.getFullPathAsString(),
        ImmutableList.of(
            new ImmutablePropertyTransformation.Builder()
                .objectRemoveSelect(FeatureRefResolver.ID)
                .objectReduceFormat(URI_TEMPLATE)
                .build()));
  }

  static void mapToLink(FeatureSchema schema, ImmutableProfileTransformations.Builder builder) {
    builder.putTransformations(
        schema.getFullPathAsString(),
        ImmutableList.of(
            new ImmutablePropertyTransformation.Builder()
                .objectRemoveSelect(FeatureRefResolver.ID)
                .objectMapFormat(
                    ImmutableMap.of("title", FeatureRefResolver.SUB_TITLE, "href", URI_TEMPLATE))
                .build()));
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
