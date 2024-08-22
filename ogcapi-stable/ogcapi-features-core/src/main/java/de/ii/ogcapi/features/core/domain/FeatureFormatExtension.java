/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.features.core.domain.ImmutableProfileTransformations.Builder;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@AutoMultiBind
public abstract class FeatureFormatExtension implements FormatExtension {

  protected final ExtensionRegistry extensionRegistry;

  protected FeatureFormatExtension(ExtensionRegistry extensionRegistry) {
    this.extensionRegistry = extensionRegistry;
  }

  public abstract ApiMediaType getCollectionMediaType();

  public EpsgCrs getContentCrs(EpsgCrs targetCrs) {
    return targetCrs;
  }

  public ApiMediaTypeContent getFeatureContent(
      OgcApiDataV2 apiData, Optional<String> collectionId, boolean featureCollection) {
    return getContent();
  }

  public boolean canPassThroughFeatures() {
    return false;
  }

  public boolean canEncodeFeatures() {
    return false;
  }

  public Optional<FeatureTokenEncoder<?>> getFeatureEncoderPassThrough(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    return Optional.empty();
  }

  public Optional<FeatureTokenEncoder<?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext, Optional<Locale> language) {
    return Optional.empty();
  }

  public Optional<PropertyTransformations> getPropertyTransformations(
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

  public Optional<PropertyTransformations> getPropertyTransformations(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<FeatureSchema> schema,
      List<String> profiles) {
    if (profiles.isEmpty() || schema.isEmpty()) {
      return getPropertyTransformations(collectionData);
    }

    Builder builder = new Builder();

    List<ProfileExtensionFeatures> profileExtensions =
        extensionRegistry.getExtensionsForType(ProfileExtensionFeatures.class);

    schema.ifPresent(
        s ->
            profiles.forEach(
                profile ->
                    profileExtensions.stream()
                        .filter(pe -> pe.isEnabledForApi(apiData, collectionData.getId()))
                        .forEach(
                            pe ->
                                pe.addPropertyTransformations(
                                    profile, s, getMediaType().type().toString(), builder))));

    ProfileTransformations profileTransformations = builder.build();

    return Optional.of(
        getPropertyTransformations(collectionData)
            .map(pts -> pts.mergeInto(profileTransformations))
            .orElse(profileTransformations));
  }

  public boolean supportsHitsOnly() {
    return false;
  }

  public Optional<Long> getNumberMatched(Object content) {
    return Optional.empty();
  }

  public Optional<Long> getNumberReturned(Object content) {
    return Optional.empty();
  }

  public boolean isComplex() {
    return false;
  }

  public boolean isForHumans() {
    return false;
  }

  public Map<String, String> getDefaultProfiles(OgcApiDataV2 apiData, String collectionId) {
    return apiData
        .getExtension(getBuildingBlockConfigurationType(), collectionId)
        .filter(
            buildingBlockConfiguration ->
                buildingBlockConfiguration instanceof FeatureFormatConfiguration)
        .map(
            buildingBlockConfiguration ->
                ((FeatureFormatConfiguration) buildingBlockConfiguration).getDefaultProfiles())
        .orElse(Map.of());
  }

  public final Optional<String> getDefaultProfile(
      String prefix, OgcApiDataV2 apiData, String collectionId) {
    return Optional.ofNullable(getDefaultProfiles(apiData, collectionId).get(prefix));
  }
}
