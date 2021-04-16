/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.common.domain

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.AbstractExtensionConfigurationSpec
import de.ii.ldproxy.ogcapi.domain.ImmutableLink
import de.ii.ldproxy.ogcapi.domain.MergeBase
import de.ii.ldproxy.ogcapi.domain.MergeCollection
import de.ii.ldproxy.ogcapi.domain.MergeMinimal
import de.ii.ldproxy.ogcapi.domain.MergeSimple

@SuppressWarnings('ClashingTraitMethods')
class CommonConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<CommonConfiguration>, MergeMinimal<CommonConfiguration>, MergeSimple<CommonConfiguration>, MergeCollection<CommonConfiguration> {
    @Override
    CommonConfiguration getFull() {
        return new ImmutableCommonConfiguration.Builder()
                .enabled(false)
                .addAdditionalLinks(new ImmutableLink.Builder()
                        .title("foo")
                        .href("bar")
                        .build())
                .build()
    }

    @Override
    CommonConfiguration getMinimal() {
        return new ImmutableCommonConfiguration.Builder()
                .build()
    }

    @Override
    CommonConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    CommonConfiguration getSimple() {
        return new ImmutableCommonConfiguration.Builder()
                .enabled(true)
                .build()
    }

    @Override
    CommonConfiguration getSimpleFullMerged() {
        return new ImmutableCommonConfiguration.Builder()
                .from(getFull())
                .enabled(true)
                .build()
    }

    @Override
    CommonConfiguration getCollection() {
        return new ImmutableCommonConfiguration.Builder()
                .addAdditionalLinks(new ImmutableLink.Builder()
                        .title("bar")
                        .href("foo")
                        .build())
                .build()
    }

    @Override
    CommonConfiguration getCollectionFullMerged() {
        return new ImmutableCommonConfiguration.Builder()
                .from(getFull())
                .additionalLinks(ImmutableList.of(
                        new ImmutableLink.Builder()
                                .title("foo")
                                .href("bar")
                                .build(),
                        new ImmutableLink.Builder()
                                .title("bar")
                                .href("foo")
                                .build()
                ))
                .build()
    }
}
