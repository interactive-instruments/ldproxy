/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.crs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.List;
import java.util.Objects;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableCrsConfiguration.Builder.class)
public abstract class CrsConfiguration implements ExtensionConfiguration {

    @Value.Default
    @Override
    public boolean getEnabled() {
        return true;
    }

    //TODO: migrate
    public abstract List<EpsgCrs> getAdditionalCrs();

    @Override
    public <T extends ExtensionConfiguration> T mergeDefaults(T extensionConfigurationDefault) {
        boolean enabled = this.getEnabled();
        List<EpsgCrs> crsList = this.getAdditionalCrs();
        ImmutableCrsConfiguration.Builder configBuilder = new ImmutableCrsConfiguration.Builder().from(extensionConfigurationDefault);

        if (Objects.nonNull(enabled))
            configBuilder.enabled(enabled);
        if (Objects.nonNull(crsList) && !crsList.isEmpty())
            configBuilder.additionalCrs(crsList);

        return (T) configBuilder.build();
    }
}
