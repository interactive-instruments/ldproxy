/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain

import com.google.common.collect.ImmutableMap
import de.ii.ogcapi.foundation.domain.*
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation

@SuppressWarnings('ClashingTraitMethods')
class GeoJsonConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<GeoJsonConfiguration>, MergeMinimal<GeoJsonConfiguration>, MergeSimple<GeoJsonConfiguration>, MergeMap<GeoJsonConfiguration>, MergeNested<GeoJsonConfiguration> {

    @Override
    GeoJsonConfiguration getFull() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .enabled(true)
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()])
                .build()
    }

    @Override
    GeoJsonConfiguration getMinimal() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .build()
    }

    @Override
    GeoJsonConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    GeoJsonConfiguration getSimple() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .enabled(false)
                .build()
    }

    @Override
    GeoJsonConfiguration getSimpleFullMerged() {
        def transformations = new LinkedHashMap(getFull().getTransformations())

        return new ImmutableGeoJsonConfiguration.Builder()
                .from(getFull())
                .transformations(transformations)
                .from(getSimple())
                .build()
    }

    @Override
    GeoJsonConfiguration getMap() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .putTransformations("bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()])
                .build()
    }

    @Override
    GeoJsonConfiguration getMapFullMerged() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()],
                        "bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()]
                ))
                .build()
    }

    @Override
    GeoJsonConfiguration getNested() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().codelist("cl").build()])
                .build()
    }

    @Override
    GeoJsonConfiguration getNestedFullMerged() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", [
                        new ImmutablePropertyTransformation.Builder().rename("bar").build(),
                        new ImmutablePropertyTransformation.Builder().codelist("cl").build()
                ]
                ))
                .build()
    }
}
