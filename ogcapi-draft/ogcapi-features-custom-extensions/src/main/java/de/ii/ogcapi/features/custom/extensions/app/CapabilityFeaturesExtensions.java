/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.custom.extensions.app;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.features.custom.extensions.domain.ImmutableFeaturesExtensionsConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;


@Singleton
@AutoBind
public class CapabilityFeaturesExtensions implements ApiBuildingBlock {

    @Inject
    CapabilityFeaturesExtensions() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder()
            .enabled(false)
            .postOnItems(false)
            .intersectsParameter(false)
            .build();
    }

}
