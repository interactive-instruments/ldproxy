/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collection.styleinfo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.application.ImmutableOgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableStyleInfoConfiguration.Builder.class)
public abstract class StyleInfoConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    @Override
    public <T extends ExtensionConfiguration> T mergeDefaults(T extensionConfigurationDefault) {

        return (T) new ImmutableStyleInfoConfiguration.Builder().from(extensionConfigurationDefault)
                .from(this)
                .build();
    }

}
