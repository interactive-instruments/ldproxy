/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.json.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.json.domain.ImmutableJsonConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CapabilityJson implements ApiBuildingBlock {

    @Inject
    CapabilityJson() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableJsonConfiguration.Builder().enabled(true)
                                                       .useFormattedJsonOutput(false)
                                                       .build();
    }

}
