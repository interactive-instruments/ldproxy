/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * @title Features GeoJSON
 * @langEn Encode features as GeoJSON.
 * @langDe Enkodierung von Features als GeoJSON.
 * @conformanceEn *Features GeoJSON* implements all requirements of conformance class *GeoJSON* from
 *     [OGC API - Features - Part 1: Core
 *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_geojson) for the two mentioned
 *     resources.
 * @conformanceDe Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der
 *     Konformitätsklasse "GeoJSON" von [OGC API - Features - Part 1: Core
 *     1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_geojson).
 * @ref:cfg {@link de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration}
 */
@Singleton
@AutoBind
public class GeoJsonBuildingBlock implements ApiBuildingBlock {

  @Inject
  public GeoJsonBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder()
        .enabled(true)
        .nestedObjectStrategy(GeoJsonConfiguration.NESTED_OBJECTS.NEST)
        .multiplicityStrategy(GeoJsonConfiguration.MULTIPLICITY.ARRAY)
        .useFormattedJsonOutput(false)
        .separator(".")
        .build();
  }
}
