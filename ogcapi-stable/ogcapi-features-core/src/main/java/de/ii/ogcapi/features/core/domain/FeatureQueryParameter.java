/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;

@AutoMultiBind
public interface FeatureQueryParameter {

  String getName();

  default boolean isFilterParameter() {
    return false;
  }

  default void applyTo(
      ImmutableFeatureQuery.Builder queryBuilder,
      QueryParameterSet parameters,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData) {
    // filter parameters follow a common pattern
    if (isFilterParameter() && parameters.getTypedValues().containsKey(getName())) {
      queryBuilder.addFilters((Cql2Expression) parameters.getTypedValues().get(getName()));
    }
  }
}
