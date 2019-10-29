/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.transactional;

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
public class OgcApiCapabilityTransactional implements OgcApiCapabilityExtension {
    @Override
    public ExtensionConfiguration getDefaultConfiguration(OgcApiConfigPreset preset) {
        ImmutableTransactionalConfiguration.Builder config = new ImmutableTransactionalConfiguration.Builder();

        switch (preset) {
            case OGCAPI:
            case GSFS:
                config.enabled(false);
                break;
        }

        return config.build();
    }
}
