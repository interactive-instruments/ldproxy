/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;

import java.util.Map;

public interface OgcApiParameterExtension extends OgcApiExtension {

    default Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureTypeConfigurationWfs3,
                                                    Map<String, String> parameters, OgcApiDatasetData serviceData) {
        return parameters;
    }

    default ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfigurationWfs3,
                                                         ImmutableFeatureQuery.Builder queryBuilder,
                                                         Map<String, String> parameters,
                                                         OgcApiDatasetData serviceData) {
        return queryBuilder;
    }
}
