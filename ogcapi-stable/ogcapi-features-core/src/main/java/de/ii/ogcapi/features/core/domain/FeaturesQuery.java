/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface FeaturesQuery {
  FeatureQuery requestToFeatureQuery(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi collectionData,
                                     EpsgCrs defaultCrs, Map<String, Integer> coordinatePrecision,
                                     Map<String, String> parameters, List<OgcApiQueryParameter> allowedParameters,
                                     String featureId);

  FeatureQuery requestToFeatureQuery(OgcApi api, FeatureTypeConfigurationOgcApi collectionData,
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

  Map<String, String> getQueryableTypes(OgcApiDataV2 apiData,
                                        FeatureTypeConfigurationOgcApi collectionData);

  Optional<Cql2Expression> getFilterFromQuery(Map<String, String> query, Map<String, String> filterableFields,
                                         Set<String> filterParameters, Map<String, String> queryableTypes,
                                         Format cqlFormat);

  @Value.Immutable
  interface QueryValidationInputCoordinates {
    boolean getEnabled();
    Optional<EpsgCrs> getBboxCrs();
    Optional<EpsgCrs> getFilterCrs();
    Optional<EpsgCrs> getNativeCrs();

    static QueryValidationInputCoordinates none() {
      return new ImmutableQueryValidationInputCoordinates.Builder()
          .enabled(false)
          .build();
    }
  }
}
