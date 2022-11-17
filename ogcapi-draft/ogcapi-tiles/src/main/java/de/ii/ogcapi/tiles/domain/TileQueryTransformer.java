/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileQuery;
import java.util.Map;

public interface TileQueryTransformer {

  default ImmutableTileQuery.Builder transformQuery(
      ImmutableTileQuery.Builder queryBuilder,
      Map<String, String> parameters,
      OgcApiDataV2 apiData) {
    return queryBuilder;
  }

  default ImmutableTileQuery.Builder transformQuery(
      ImmutableTileQuery.Builder queryBuilder,
      Map<String, String> parameters,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData) {
    return queryBuilder;
  }
}
