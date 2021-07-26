/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureTokenEncoder;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;

import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;
import static de.ii.ldproxy.ogcapi.features.core.app.PathParameterFeatureIdFeatures.FEATURE_ID_PATTERN;

public interface FeatureFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^/?collections/"+COLLECTION_ID_PATTERN+"/items(?:/"+FEATURE_ID_PATTERN+")?$";
    }

    ApiMediaType getCollectionMediaType();

    default boolean canPassThroughFeatures() {
        return false;
    }

    default boolean canEncodeFeatures() {
        return false;
    }

    default Optional<FeatureConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.empty();
    }

    default Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        return Optional.empty();
    }

    default Optional<FeatureTokenEncoder<byte[], ?>> getFeatureEncoder(
      FeatureTransformationContext transformationContext,
      Optional<Locale> language) {
        return Optional.empty();
    }

    default Optional<PropertyTransformations> getPropertyTransformations(FeatureTypeConfigurationOgcApi collectionData) {

        Map<String, PropertyTransformation> coreTransformations = collectionData.getExtension(FeaturesCoreConfiguration.class)
            .map(FeaturesCoreConfiguration::getTransformations)
            .orElse(ImmutableMap.of());

        Map<String, PropertyTransformation> formatTransformations = collectionData.getExtension(this.getBuildingBlockConfigurationType())
            .filter(buildingBlockConfiguration -> buildingBlockConfiguration instanceof PropertyTransformations)
            .map(buildingBlockConfiguration -> ((PropertyTransformations)buildingBlockConfiguration).getTransformations())
            .orElse(ImmutableMap.of());

        Map<String, PropertyTransformation> propertyTransformations = new LinkedHashMap<>(coreTransformations);

        formatTransformations.forEach((key, propertyTransformation) -> {
            propertyTransformations.putIfAbsent(key, propertyTransformation);
            propertyTransformations.computeIfPresent(key, (key2, corePropertyTransformation) -> propertyTransformation.mergeInto(corePropertyTransformation));
        });

        if (propertyTransformations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(() -> propertyTransformations);
    }
}
