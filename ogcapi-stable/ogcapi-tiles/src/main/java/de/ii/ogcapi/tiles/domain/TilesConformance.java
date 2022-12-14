/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.oas30.domain.Oas30Configuration;
import java.util.List;

public class TilesConformance implements ConformanceClass {

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    if (apiData
        .getExtension(Oas30Configuration.class)
        .map(ExtensionConfiguration::isEnabled)
        .orElse(true)) {
      return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/oas30");
    }
    return ImmutableList.of();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }
}
