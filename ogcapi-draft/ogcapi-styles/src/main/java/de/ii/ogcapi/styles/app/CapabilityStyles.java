/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ogcapi.styles.domain.ImmutableStylesConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CapabilityStyles implements ApiBuildingBlock {

    private final ExtensionRegistry extensionRegistry;

    @Inject
    public CapabilityStyles(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                         .managerEnabled(false)
                                                         .validationEnabled(false)
                                                         .useIdFromStylesheet(false)
                                                         .resourcesEnabled(false)
                                                         .resourceManagerEnabled(false)
                                                         .styleEncodings(extensionRegistry.getExtensionsForType(
                                                                 StyleFormatExtension.class)
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
