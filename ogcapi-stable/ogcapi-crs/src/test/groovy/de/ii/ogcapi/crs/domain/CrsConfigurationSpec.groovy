/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crs.domain

import com.google.common.collect.ImmutableList
import de.ii.ogcapi.foundation.domain.AbstractExtensionConfigurationSpec
import de.ii.ogcapi.foundation.domain.MergeBase
import de.ii.ogcapi.foundation.domain.MergeCollection
import de.ii.ogcapi.foundation.domain.MergeMinimal
import de.ii.ogcapi.foundation.domain.MergeSimple
import de.ii.xtraplatform.crs.domain.EpsgCrs

@SuppressWarnings('ClashingTraitMethods')
class CrsConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<CrsConfiguration>, MergeMinimal<CrsConfiguration>, MergeSimple<CrsConfiguration>, MergeCollection<CrsConfiguration> {

    @Override
    CrsConfiguration getFull() {
        return new ImmutableCrsConfiguration.Builder()
                .enabled(false)
                .addAdditionalCrs(EpsgCrs.of(4326))
                .build()
    }

    @Override
    CrsConfiguration getMinimal() {
        return new ImmutableCrsConfiguration.Builder()
                .build()
    }

    @Override
    CrsConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    CrsConfiguration getSimple() {
        return new ImmutableCrsConfiguration.Builder()
                .enabled(true)
                .build()
    }

    @Override
    CrsConfiguration getSimpleFullMerged() {
        return new ImmutableCrsConfiguration.Builder()
                .from(getFull())
                .enabled(true)
                .build()
    }

    @Override
    CrsConfiguration getCollection() {
        return new ImmutableCrsConfiguration.Builder()
                .addAdditionalCrs(EpsgCrs.of(4258))
                .build()
    }

    @Override
    CrsConfiguration getCollectionFullMerged() {
        return new ImmutableCrsConfiguration.Builder()
                .from(getFull())
                .additionalCrs(ImmutableList.of(
                        EpsgCrs.of(4326),
                        EpsgCrs.of(4258)
                ))
                .build()
    }
}
