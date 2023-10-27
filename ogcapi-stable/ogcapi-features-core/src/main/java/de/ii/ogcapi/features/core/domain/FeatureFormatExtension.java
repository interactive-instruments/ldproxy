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
      Optional<Profile> profile) {
    if (profile.isEmpty() || schema.isEmpty()) {
      return getPropertyTransformations(collectionData);
    }

    // FIXME
    // TODO: Currently there is no PropertyTransformation to convert a FEATURE_REF to a Link
    //       object, so we just map it to the URI for now. For now, the Link object must be
    //       handled in the feature encoders of the formats that support 'rel-as-link'.
    //       FIXME
    ImmutableProfileTransformations.Builder builder = new ImmutableProfileTransformations.Builder();
    switch (profile.get()) {
      default:
      case AS_KEY:
        reduceToKey(schema, builder);
        break;
      case AS_URI:
        reduceToUri(schema, builder);
        break;
      case AS_LINK:
        // mapHref(schema, builder);
    }
    ProfileTransformations profileTransformations = builder.build();
    return Optional.of(
        getPropertyTransformations(collectionData)
            .map(pts -> pts.mergeInto(profileTransformations))
            .orElse(profileTransformations));
  }

  private static void mapHref(
      Optional<FeatureSchema> schema, ImmutableProfileTransformations.Builder builder) {
    schema
        .map(SchemaBase::getAllNestedProperties)
        .ifPresent(
            properties ->
                properties.stream()
                    .filter(SchemaBase::isFeatureRef)
                    .forEach(
                        property -> {
                          if (property.isValue()) {
                            getUriTemplate(property)
                                .ifPresent(
                                    template ->
                                        builder.putTransformations(
                                            property.getFullPathAsString(),
                                            ImmutableList.of(
                                                new ImmutablePropertyTransformation.Builder()
                                                    .stringFormat(
                                                        template.replace("{featureId}", "{value}"))
                                                    .build())));
                          } else if (property.isObject()) {
                            getUriTemplate(property)
                                .ifPresent(
                                    template ->
                                        property.getProperties().stream()
                                            .filter(p -> "featureId".equals(p.getName()))
                                            .map(SchemaBase::getFullPathAsString)
                                            .findFirst()
                                            .ifPresent(
                                                path ->
                                                    builder.putTransformations(
                                                        path,
                                                        ImmutableList.of(
                                                            new ImmutablePropertyTransformation
                                                                    .Builder()
                                                                .stringFormat(
                                                                    template.replace(
                                                                        "{featureId}", "{value}"))
                                                                .rename("href")
                                                                .build()))));
                          }
                        }));
  }

  static void reduceToUri(
      Optional<FeatureSchema> schema, ImmutableProfileTransformations.Builder builder) {
    schema
        .map(SchemaBase::getAllNestedProperties)
        .ifPresent(
            properties ->
                properties.stream()
                    .filter(SchemaBase::isFeatureRef)
                    .forEach(
                        property -> {
                          if (property.isValue()) {
                            getUriTemplate(property)
                                .ifPresent(
                                    template ->
                                        builder.putTransformations(
                                            property.getFullPathAsString(),
                                            ImmutableList.of(
                                                new ImmutablePropertyTransformation.Builder()
                                                    .stringFormat(
                                                        template.replace("{featureId}", "{value}"))
                                                    .build())));
                          } else if (property.isObject()) {
                            getUriTemplate(property)
                                .ifPresent(
                                    template ->
                                        builder.putTransformations(
                                            property.getFullPathAsString(),
                                            ImmutableList.of(
                                                new ImmutablePropertyTransformation.Builder()
                                                    .reduceStringFormat(
                                                        template.replace(
                                                            "{value}", "{hatObjekt.3_featureId}"))
                                                    .build())));
                          }
                        }));
  }

  static void reduceToKey(
      Optional<FeatureSchema> schema, ImmutableProfileTransformations.Builder builder) {
    schema
        .map(SchemaBase::getAllNestedProperties)
        .ifPresent(
            properties ->
                properties.stream()
                    .filter(SchemaBase::isFeatureRef)
                    .filter(SchemaBase::isObject)
                    .forEach(
                        property -> {
                          builder.putTransformations(
                              property.getFullPathAsString(),
                              ImmutableList.of(
                                  new ImmutablePropertyTransformation.Builder()
                                      .reduceStringFormat(
                                          getKeyTemplate(property)
                                              .orElse("{{hatObjekt.3_featureId}}"))
                                      .build()));
                        }));
  }

  /* FIXME
  private static void reduceToKey(Optional<FeatureSchema> schema,
      ImmutableProfileTransformations.Builder builder) {
    schema
        .map(SchemaBase::getAllNestedProperties)
        .ifPresent(
            properties ->
                properties.stream()
                    .filter(SchemaBase::isFeatureRef)
                    .filter(SchemaBase::isObject)
                    .forEach(
                        property ->
                            builder.putTransformations(
                            property.getFullPathAsString(),
                            ImmutableList.of(
                                new ImmutablePropertyTransformation.Builder()
                                    .reduceStringFormat(
                                        property.getProperties().stream().anyMatch(p -> "collectionId".equals(p.getName())) ?
                                        "{{collectionId}}::{{featureId}}" : "{{featureId}}")
                                    .build())) ));
  }
   */

  private static void removeTitle(
      Optional<FeatureSchema> schema, ImmutableProfileTransformations.Builder builder) {
    schema
        .map(SchemaBase::getAllNestedProperties)
        .ifPresent(
            properties ->
                properties.stream()
                    .filter(SchemaBase::isFeatureRef)
                    .filter(SchemaBase::isObject)
                    .forEach(
                        property ->
                            property.getProperties().stream()
                                .filter(p -> "title".equals(p.getName()))
                                .map(SchemaBase::getFullPathAsString)
                                .findFirst()
                                .ifPresent(
                                    path ->
                                        builder.putTransformations(
                                            path,
                                            ImmutableList.of(
                                                new ImmutablePropertyTransformation.Builder()
                                                    .remove("ALWAYS")
                                                    .build())))));
  }

  static Optional<String> getUriTemplate(FeatureSchema property) {
    return Optional.ofNullable(
        property
            .getRefUriTemplate()
            .orElse(
                property
                    .getRefType()
                    .map(
                        refType ->
                            String.format("{{apiUri}}/collections/%s/items/{{value}}", refType))
                    .or(
                        () ->
                            property.getConcat().isEmpty() && property.getCoalesce().isEmpty()
                                ? Optional.empty()
                                : Optional.of("{{apiUri}}/collections/{{type}}/items/{{value}}"))
                    .orElse(null)));
  }

  static Optional<String> getKeyTemplate(FeatureSchema property) {
    return property.getRefKeyTemplate();
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
