/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;

import java.util.Locale;
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

    default boolean canTransformFeatures() {
        return false;
    }

    default Optional<FeatureConsumer> getFeatureConsumer(FeatureTransformationContext transformationContext) {
        return Optional.empty();
    }

    default Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        return Optional.empty();
    }

}
