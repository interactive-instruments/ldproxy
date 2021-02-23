/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojsonld.domain

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.AbstractExtensionConfigurationSpec
import de.ii.ldproxy.ogcapi.domain.MergeBase
import de.ii.ldproxy.ogcapi.domain.MergeCollection
import de.ii.ldproxy.ogcapi.domain.MergeMinimal
import de.ii.ldproxy.ogcapi.domain.MergeSimple

@SuppressWarnings('ClashingTraitMethods')
class GeoJsonLdConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<GeoJsonLdConfiguration>, MergeMinimal<GeoJsonLdConfiguration>, MergeSimple<GeoJsonLdConfiguration>, MergeCollection<GeoJsonLdConfiguration> {
    @Override
    GeoJsonLdConfiguration getFull() {
        return new ImmutableGeoJsonLdConfiguration.Builder()
                .enabled(false)
                .context("foo")
                .idTemplate("foo")
                .addTypes("foo")
                .build()
    }

    @Override
    GeoJsonLdConfiguration getMinimal() {
        return new ImmutableGeoJsonLdConfiguration.Builder()
                .build()
    }

    @Override
    GeoJsonLdConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    GeoJsonLdConfiguration getSimple() {
        return new ImmutableGeoJsonLdConfiguration.Builder()
                .enabled(true)
                .context("bar")
                .idTemplate("bar")
                .build()
    }

    @Override
    GeoJsonLdConfiguration getSimpleFullMerged() {
        return new ImmutableGeoJsonLdConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }

    @Override
    GeoJsonLdConfiguration getCollection() {
        return new ImmutableGeoJsonLdConfiguration.Builder()
                .addTypes("bar")
                .build()
    }

    @Override
    GeoJsonLdConfiguration getCollectionFullMerged() {
        return new ImmutableGeoJsonLdConfiguration.Builder()
                .from(getFull())
                .types(ImmutableList.of(
                        "foo",
                        "bar"
                ))
                .build()
    }
}
