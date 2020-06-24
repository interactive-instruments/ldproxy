/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.immutables.value.Value;

import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableFilterConfiguration.Builder.class)
public abstract class FilterConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return false;
    }

    @Override
    public <T extends ExtensionConfiguration> T mergeDefaults(T extensionConfigurationDefault) {
        boolean enabled = this.getEnabled();
        ImmutableFilterConfiguration.Builder configBuilder = new ImmutableFilterConfiguration.Builder().from(extensionConfigurationDefault);

        if (Objects.nonNull(enabled))
            configBuilder.enabled(enabled);

        return (T) configBuilder.build();
    }
}
