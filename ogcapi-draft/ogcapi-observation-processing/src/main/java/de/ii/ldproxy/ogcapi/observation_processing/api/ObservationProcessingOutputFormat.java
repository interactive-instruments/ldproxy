/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface ObservationProcessingOutputFormat extends FormatExtension {

    default String getPathPattern() {
        String DAPA_PATH_ELEMENT = "dapa";
        return "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/position(?:/aggregate-time)?/?$)|" +
                "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/area(?:/aggregate-(space|time|space-time)/?$)|" +
                "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/resample-to-grid(?:/aggregate-time)?)?/?$)";
    }

    // TODO
    default boolean isEnabledForApi(OgcApiApiDataV2 apiData, String subPath) {
        return isEnabledForApi(apiData);
    }

    default boolean canTransformFeatures() {
        return false;
    }

    default Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContext transformationContext, Optional<Locale> language) {
        return Optional.empty();
    }
}
