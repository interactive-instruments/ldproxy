/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;

import java.util.Map;

/**
 * @author zahnen
 */
public interface Wfs3ParameterExtension extends Wfs3Extension {

    default Map<String, String> transformParameters(FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, Map<String, String> parameters) {
        return parameters;
    }

    default ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, ImmutableFeatureQuery.Builder queryBuilder, Map<String, String> parameters) {
        return queryBuilder;
    }
}
