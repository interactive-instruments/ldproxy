/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs.domain

import com.google.common.collect.ImmutableList
import de.ii.xtraplatform.crs.domain.EpsgCrs

class CrsConfigurationFixtures {

    static final CrsConfiguration ENABLED_ONLY = new ImmutableCrsConfiguration.Builder()
            .enabled(true)
            .build()

    static final CrsConfiguration FULL_1 = new ImmutableCrsConfiguration.Builder()
            .enabled(false)
            .addAdditionalCrs(EpsgCrs.of(4326))
            .build()

    static final CrsConfiguration FULL_2 = new ImmutableCrsConfiguration.Builder()
            .enabled(true)
            .addAdditionalCrs(EpsgCrs.of(4258))
            .build()


    static final CrsConfiguration ENABLED_FULL_1_EXPECTED = new ImmutableCrsConfiguration.Builder()
            .from(FULL_1)
            .enabled(true)
            .build()

    static final CrsConfiguration FULL_2_FULL_1_EXPECTED = new ImmutableCrsConfiguration.Builder()
            .from(FULL_2)
            .additionalCrs(ImmutableList.of(
                    EpsgCrs.of(4326),
                    EpsgCrs.of(4258)
            ))
            .build()

}
