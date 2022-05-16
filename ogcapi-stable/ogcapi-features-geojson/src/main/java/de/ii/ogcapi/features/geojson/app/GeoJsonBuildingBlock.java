/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */

/**
 * @title Features GeoJSON
 * @langEn The module *Features GeoJSON* may be enabled for every API with a feature provider.
 * It provides the resources *Features* and *Feature* encoded as GeoJSON.
 *
 * @conformanceEn *Features GeoJSON* implements all requirements of conformance class *GeoJSON* from
 * [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_geojson)
 * for the two mentioned resources.
 * @langDe Das Modul *Features GeoJSON* kann für jede über ldproxy bereitgestellte API mit einem
 * Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features
 * und Feature in GeoJSON.
 *
 * @conformanceDe Das Modul implementiert für die Ressourcen Features und Feature alle Vorgaben der
 * Konformitätsklasse "GeoJSON" von [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_geojson).
 * @example {@link de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration}
 * @propertyTable {@link de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration}
 * @queryParameterTable {@link de.ii.ogcapi.features.geojson.app.QueryParameterDebugFeaturesGeoJson},
 * {@link de.ii.ogcapi.features.geojson.app.QueryParameterPrettyFeaturesGeoJson}
 */
@Singleton
@AutoBind
public class GeoJsonBuildingBlock implements ApiBuildingBlock {

    @Inject
    public GeoJsonBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(true)
                                                          .nestedObjectStrategy(
                                                              GeoJsonConfiguration.NESTED_OBJECTS.NEST)
                                                          .multiplicityStrategy(
                                                              GeoJsonConfiguration.MULTIPLICITY.ARRAY)
                                                          .useFormattedJsonOutput(false)
                                                          .separator(".")
                                                          .build();
    }

}
