/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.OgcApiExtensionRegistry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class OgcApiCapabilityStyles implements OgcApiBuildingBlock {

    @Requires
    OgcApiExtensionRegistry extensionRegistry;

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableStylesConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableStylesConfiguration.Builder().enabled(false)
                                                         .managerEnabled(false)
                                                         .validationEnabled(false)
                                                         .resourcesEnabled(false)
                                                         .resourceManagerEnabled(false)
                                                         .styleEncodings(extensionRegistry.getExtensionsForType(StyleFormatExtension.class)
                                                                                     .stream()
                                                                                     .filter(FormatExtension::isEnabledByDefault)
                                                                                     .map(format -> format.getMediaType().label())
                                                                                     .collect(ImmutableList.toImmutableList()))
                                                         .build();
    }

}
