/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.oas30.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.oas30.domain.Oas30Configuration;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class EndpointOpenApi implements ConformanceClass {

  @Inject
  EndpointOpenApi() {}

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    // TODO only return the URIs for Features and Tiles, if the building blocks are enabled
    return ImmutableList.of(
        "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/oas30",
        "http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/oas30",
        "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/oas30");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Oas30Configuration.class;
  }
}
