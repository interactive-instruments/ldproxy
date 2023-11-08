/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.xtraplatform.base.domain.ETag;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureSchemaBase;
import java.util.Map;
import java.util.Optional;

public interface FeaturesQuery {
  FeatureQuery requestToFeatureQuery(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      EpsgCrs defaultCrs,
      Map<String, Integer> coordinatePrecision,
      QueryParameterSet queryParameterSet,
      String featureId,
      Optional<ETag.Type> withETag,
      FeatureSchemaBase.Scope withScope);

  FeatureQuery requestToFeatureQuery(
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      EpsgCrs defaultCrs,
      Map<String, Integer> coordinatePrecision,
      int defaultPageSize,
      QueryParameterSet queryParameterSet);

  FeatureQuery requestToBareFeatureQuery(
      OgcApiDataV2 apiData,
      String featureTypeId,
      EpsgCrs defaultCrs,
      Map<String, Integer> coordinatePrecision,
      int defaultPageSize,
      QueryParameterSet queryParameterSet);

  Optional<String> validateFilter(
      String filter, Format filterLang, EpsgCrs filterCrs, Map<String, FeatureSchema> queryables);
}
