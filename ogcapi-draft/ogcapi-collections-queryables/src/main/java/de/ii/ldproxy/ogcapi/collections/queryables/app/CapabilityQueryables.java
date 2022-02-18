/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables.app;


import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
@AutoBind
public class CapabilityQueryables implements ApiBuildingBlock {

    @Inject
    CapabilityQueryables() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableQueryablesConfiguration.Builder().enabled(false)
                                                             .build();
    }

}
