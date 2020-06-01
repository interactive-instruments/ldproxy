/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;

import java.util.Map;
import java.util.Set;

// TODO no longer used
public interface OgcApiParameterExtension extends OgcApiExtension {

    ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath);

    default Set<String> getFilterParameters(Set<String> filterParameters, OgcApiApiDataV2 serviceData) {
        return filterParameters;
    }

    default Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureType,
                                                    Map<String, String> parameters,
                                                    OgcApiApiDataV2 serviceData) {
        return parameters;
    }

    default ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureType,
                                                         ImmutableFeatureQuery.Builder queryBuilder,
                                                         Map<String, String> parameters,
                                                         OgcApiApiDataV2 serviceData) {
        return queryBuilder;
    }

    default Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                 Map<String, Object> context,
                                                 Map<String, String> parameters,
                                                 OgcApiApiDataV2 serviceData) {
        return context;
    }

}
