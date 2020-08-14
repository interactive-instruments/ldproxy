/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiBuildingBlock;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiCapabilityGeoJson implements OgcApiBuildingBlock {

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableGeoJsonConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableGeoJsonConfiguration.Builder().enabled(true)
                                                          .nestedObjectStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS.NEST)
                                                          .multiplicityStrategy(FeatureTransformerGeoJson.MULTIPLICITY.ARRAY)
                                                          .useFormattedJsonOutput(false)
                                                          .separator(".")
                                                          .build();
    }

}
