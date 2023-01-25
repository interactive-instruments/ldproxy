/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.domain;

import static de.ii.ogcapi.features.search.app.SearchBuildingBlock.QUERY_ID_PATTERN;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;

@AutoMultiBind
public interface StoredQueryFormat extends GenericFormatExtension {

  @Override
  default String getPathPattern() {
    return "^(?:/search/" + QUERY_ID_PATTERN + "/?$";
  }

  @Override
  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return SearchConfiguration.class;
  }

  /**
   * @return the file extension used for the queries in the store
   */
  String getFileExtension();

  Object getEntity(QueryExpression query, OgcApiDataV2 apiData, ApiRequestContext requestContext);
}
