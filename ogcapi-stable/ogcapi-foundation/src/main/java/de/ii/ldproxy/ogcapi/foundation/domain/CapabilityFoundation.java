/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CapabilityFoundation implements ApiBuildingBlock {

    @Inject
    CapabilityFoundation() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableFoundationConfiguration.Builder().enabled(true)
                                                             .includeLinkHeader(true)
                                                             .useLangParameter(false)
                                                             .build();
    }
}
