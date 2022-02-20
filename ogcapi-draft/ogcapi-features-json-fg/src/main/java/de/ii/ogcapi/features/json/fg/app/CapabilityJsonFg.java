/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.json.fg.app;

import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.features.json.fg.domain.ImmutableWhereConfiguration;
import de.ii.ogcapi.features.json.fg.domain.ImmutableJsonFgConfiguration.Builder;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class CapabilityJsonFg implements ApiBuildingBlock {

    @Inject
    CapabilityJsonFg() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(false)
                                                         .describedby(true)
                                                         .when(true)
                                                         .coordRefSys(true)
                                                         .where(new ImmutableWhereConfiguration.Builder().enabled(true)
                                                                                                         .alwaysIncludeGeoJsonGeometry(false)
                                                                                                         .build())
                                                         .build();
    }

}
