/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.ImmutableWhereConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class CapabilityJsonFg implements ApiBuildingBlock {

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableJsonFgConfiguration.Builder().enabled(false)
                                                         .describedby(true)
                                                         .when(true)
                                                         .coordRefSys(true)
                                                         .where(new ImmutableWhereConfiguration.Builder().enabled(true)
                                                                                                         .alwaysIncludeGeoJsonGeometry(false)
                                                                                                         .build())
                                                         .build();
    }

}
