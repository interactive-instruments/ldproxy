/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiConfigPreset;
import de.ii.ldproxy.ogcapi.domain.OgcApiCapabilityExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class OgcApiCapabilityVectorTiles implements OgcApiCapabilityExtension {
    @Override
    public ExtensionConfiguration getDefaultConfiguration(OgcApiConfigPreset preset) {

        ImmutableTilesConfiguration.Builder config = new ImmutableTilesConfiguration.Builder();

        switch (preset) {
            case OGCAPI:
            case GSFS:
                config.enabled(false);
                break;
        }

        config.zoomLevels(ImmutableMap.of("WebMercatorQuad", new ImmutableMinMax.Builder()
                .min(0)
                .max(23)
                .build()));

        return config.build();
    }
}
