/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;

import java.util.Map;

public interface OgcApiFeaturesQuery {
    FeatureQuery requestToFeatureQuery(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                       OgcApiFeaturesCoreConfiguration coreConfiguration,
                                       Map<String, String> parameters,
                                       String featureId);

    FeatureQuery requestToFeatureQuery(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                       OgcApiFeaturesCoreConfiguration coreConfiguration,
                                       int minimumPageSize,
                                       int defaultPageSize, int maxPageSize, Map<String, String> parameters);
}
