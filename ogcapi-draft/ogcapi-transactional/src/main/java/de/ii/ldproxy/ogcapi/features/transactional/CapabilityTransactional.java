/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.transactional;

import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CapabilityTransactional implements ApiBuildingBlock {

    @Inject
    CapabilityTransactional() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableTransactionalConfiguration.Builder().enabled(false)
                                                                .build();
    }

}
