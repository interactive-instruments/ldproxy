/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.styleinfo;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiCapabilityExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiConfigPreset;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;


@Component
@Provides
@Instantiate
public class OgcApiCapabilityStyleInfo implements OgcApiCapabilityExtension {

    @Override
    public ExtensionConfiguration getDefaultConfiguration(OgcApiConfigPreset preset) {
        ImmutableStyleInfoConfiguration.Builder config = new ImmutableStyleInfoConfiguration.Builder();

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
