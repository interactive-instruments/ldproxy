/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CapabilityResources implements ApiBuildingBlock {

    private final ExtensionRegistry extensionRegistry;

    @Inject
    public CapabilityResources(ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                            .managerEnabled(false)
                                                            .build();
    }

}
