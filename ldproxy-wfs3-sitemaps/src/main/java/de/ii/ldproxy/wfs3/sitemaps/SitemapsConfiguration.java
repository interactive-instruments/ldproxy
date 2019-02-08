/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.sitemaps;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.wfs3.api.ExtensionConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(as = ModifiableSitemapsConfiguration.class)
public abstract class SitemapsConfiguration implements ExtensionConfiguration {

    public static final String EXTENSION_KEY = "sitemapsExtension";
    public static final String EXTENSION_TYPE = "SITEMAPS";

    @Override
    public ExtensionConfiguration mergeDefaults(ExtensionConfiguration extensionConfigurationDefault) {
        return this;
    }
}
