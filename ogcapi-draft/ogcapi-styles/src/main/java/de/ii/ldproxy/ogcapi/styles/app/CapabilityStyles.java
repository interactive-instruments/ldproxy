/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.styles.domain.ImmutableStylesConfiguration;
import de.ii.ldproxy.ogcapi.styles.domain.StyleFormatExtension;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class CapabilityStyles implements ApiBuildingBlock {

    @Requires
    ExtensionRegistry extensionRegistry;

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableStylesConfiguration.Builder().enabled(false)
                                                         .styleInfosOnCollection(false)
                                                         .managerEnabled(false)
                                                         .validationEnabled(false)
                                                         .resourcesEnabled(false)
                                                         .resourceManagerEnabled(false)
                                                         .styleEncodings(extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                                                                     .stream()
                                                                                     .filter(FormatExtension::isEnabledByDefault)
                                                                                     .map(format -> format.getMediaType().label())
                                                                                     .collect(ImmutableList.toImmutableList()))
                                                         .webmapWithPopup(true)
                                                         .webmapWithLayerControl(false)
                                                         .layerControlAllLayers(false)
                                                         .build();
    }

}
