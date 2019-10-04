/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.projections;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiConfigPreset;
import de.ii.ldproxy.ogcapi.domain.OgcApiCapabilityExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiCapabilityProjections implements OgcApiCapabilityExtension {
    @Override
    public ExtensionConfiguration getDefaultConfiguration(OgcApiConfigPreset preset) {
        ImmutableProjectionsConfiguration.Builder config = new ImmutableProjectionsConfiguration.Builder();

        switch (preset) {
            case OGCAPI:
                config.enabled(true);
                break;
            case GSFS:
                config.enabled(false);
                break;
        }

        return config.build();
    }
}
