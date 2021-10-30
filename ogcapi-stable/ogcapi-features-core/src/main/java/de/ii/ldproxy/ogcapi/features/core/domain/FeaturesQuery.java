/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FeaturesQuery {
  FeatureQuery requestToFeatureQuery(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                     EpsgCrs defaultCrs, Map<String, Integer> coordinatePrecision,
                                     Map<String, String> parameters, List<OgcApiQueryParameter> allowedParameters,
                                     String featureId);

  FeatureQuery requestToFeatureQuery(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                     EpsgCrs defaultCrs, Map<String, Integer> coordinatePrecision,
                                     int minimumPageSize,
                                     int defaultPageSize, int maxPageSize, Map<String, String> parameters,
                                     List<OgcApiQueryParameter> allowedParameters);

  FeatureQuery requestToBareFeatureQuery(OgcApiDataV2 apiData, String featureTypeId,
                                         EpsgCrs defaultCrs, Map<String, Integer> coordinatePrecision,
                                         int minimumPageSize,
                                         int defaultPageSize, int maxPageSize, Map<String, String> parameters,
                                         List<OgcApiQueryParameter> allowedParameters);

  Map<String, String> getFilterableFields(OgcApiDataV2 apiData,
                                          FeatureTypeConfigurationOgcApi collectionData);

  Optional<CqlFilter> getFilterFromQuery(Map<String, String> query, Map<String, String> filterableFields,
                                           Set<String> filterParameters,
                                           Cql.Format cqlFormat);

}
