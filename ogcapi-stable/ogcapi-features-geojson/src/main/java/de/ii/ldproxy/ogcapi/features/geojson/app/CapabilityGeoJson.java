/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CapabilityGeoJson implements ApiBuildingBlock {

    @Inject
    CapabilityGeoJson() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableGeoJsonConfiguration.Builder().enabled(true)
                                                          .nestedObjectStrategy(
                                                              GeoJsonConfiguration.NESTED_OBJECTS.NEST)
                                                          .multiplicityStrategy(
                                                              GeoJsonConfiguration.MULTIPLICITY.ARRAY)
                                                          .useFormattedJsonOutput(false)
                                                          .separator(".")
                                                          .build();
    }

}
