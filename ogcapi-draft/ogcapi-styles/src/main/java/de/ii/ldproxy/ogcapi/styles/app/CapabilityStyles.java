/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
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

    private final ExtensionRegistry extensionRegistry;

    public CapabilityStyles(@Requires ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableStylesConfiguration.Builder().enabled(false)
                                                         .managerEnabled(false)
                                                         .validationEnabled(false)
                                                         .useIdFromStylesheet(false)
                                                         .resourcesEnabled(false)
                                                         .resourceManagerEnabled(false)
                                                         .styleEncodings(extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                                                                     .stream()
                                                                                     .filter(FormatExtension::isEnabledByDefault)
                                                                                     .map(format -> format.getMediaType().label())
                                                                                     .collect(ImmutableList.toImmutableList()))
                                                         .deriveCollectionStyles(false)
                                                         .webmapWithPopup(true)
                                                         .webmapWithLayerControl(false)
                                                         .layerControlAllLayers(false)
                                                         .build();
    }

}
