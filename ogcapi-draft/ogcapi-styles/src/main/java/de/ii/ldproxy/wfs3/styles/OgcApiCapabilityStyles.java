/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiBuildingBlock;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class OgcApiCapabilityStyles implements OgcApiBuildingBlock {

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableStylesConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableStylesConfiguration.Builder().enabled(false)
                                                         .managerEnabled(false)
                                                         .mapsEnabled(false)
                                                         .validationEnabled(false)
                                                         .resourcesEnabled(false)
                                                         .resourceManagerEnabled(false)
                                                         .htmlEnabled(false)
                                                         .mbStyleEnabled(false)
                                                         .sld10Enabled(false)
                                                         .sld11Enabled(false)
                                                         .useFormattedJsonOutput(false)
                                                         .build();
    }

}
