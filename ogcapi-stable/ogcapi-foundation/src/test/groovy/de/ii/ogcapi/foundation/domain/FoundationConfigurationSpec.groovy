/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain

import de.ii.ogcapi.foundation.domain.AbstractExtensionConfigurationSpec
import de.ii.ogcapi.foundation.domain.FoundationConfiguration
import de.ii.ogcapi.foundation.domain.ImmutableFoundationConfiguration
import de.ii.ogcapi.foundation.domain.MergeBase
import de.ii.ogcapi.foundation.domain.MergeMinimal
import de.ii.ogcapi.foundation.domain.MergeSimple

@SuppressWarnings('ClashingTraitMethods')
class FoundationConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<FoundationConfiguration>, MergeMinimal<FoundationConfiguration>, MergeSimple<FoundationConfiguration> {
    @Override
    FoundationConfiguration getFull() {
        return new ImmutableFoundationConfiguration.Builder()
                .enabled(true)
                .useLangParameter(true)
                .includeLinkHeader(true)
                .apiCatalogLabel("foo")
                .apiCatalogDescription("bar")
                .build()
    }

    @Override
    FoundationConfiguration getMinimal() {
        return new ImmutableFoundationConfiguration.Builder()
                .build()
    }

    @Override
    FoundationConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    FoundationConfiguration getSimple() {
        return new ImmutableFoundationConfiguration.Builder()
                .enabled(false)
                .useLangParameter(false)
                .includeLinkHeader(false)
                .apiCatalogLabel("bar")
                .apiCatalogDescription("foo")
                .build()
    }

    @Override
    FoundationConfiguration getSimpleFullMerged() {
        return new ImmutableFoundationConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }
}
