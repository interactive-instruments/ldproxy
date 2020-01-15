/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.api;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;

import java.util.Locale;
import java.util.Optional;

public interface OgcApiFeatureFormatExtension extends FormatExtension {

    default String getPathPattern() {
        return "^\\/?collections\\/[^\\/]+\\/items(?:\\/[^\\/]+)?$";
    }

    OgcApiMediaType getCollectionMediaType();

    default boolean canPassThroughFeatures() {
        return false;
    }

    default boolean canTransformFeatures() {
        return false;
    }

    default Optional<FeatureConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.empty();
    }

    default Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        return Optional.empty();
    }

    default Optional<TargetMappingProviderFromGml> getMappingGenerator() {
        return Optional.empty();
    }

    default Optional<TargetMappingRefiner> getMappingRefiner() {
        return Optional.empty();
    }
}
