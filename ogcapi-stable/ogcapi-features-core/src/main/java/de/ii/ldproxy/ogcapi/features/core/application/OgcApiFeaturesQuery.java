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
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.features.domain.FeatureQuery;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface OgcApiFeaturesQuery {
    FeatureQuery requestToFeatureQuery(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                       OgcApiFeaturesCoreConfiguration coreConfiguration,
                                       Map<String, String> parameters, Set<OgcApiQueryParameter> allowedParameters,
                                       String featureId);

    FeatureQuery requestToFeatureQuery(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                       OgcApiFeaturesCoreConfiguration coreConfiguration,
                                       int minimumPageSize,
                                       int defaultPageSize, int maxPageSize, Map<String, String> parameters,
                                       Set<OgcApiQueryParameter> allowedParameters);

    Optional<CqlFilter> getFilterFromQuery(Map<String, String> query, Map<String, String> filterableFields,
                                           Set<String> filterParameters,
                                           Cql.Format cqlFormat);

}
