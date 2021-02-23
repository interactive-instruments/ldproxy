/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain

import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.domain.AbstractExtensionConfigurationSpec
import de.ii.ldproxy.ogcapi.domain.MergeBase
import de.ii.ldproxy.ogcapi.domain.MergeMap
import de.ii.ldproxy.ogcapi.domain.MergeMinimal
import de.ii.ldproxy.ogcapi.domain.MergeNested
import de.ii.ldproxy.ogcapi.domain.MergeSimple
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutablePropertyTransformation

@SuppressWarnings('ClashingTraitMethods')
class GeoJsonConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<GeoJsonConfiguration>, MergeMinimal<GeoJsonConfiguration>, MergeSimple<GeoJsonConfiguration>, MergeMap<GeoJsonConfiguration>, MergeNested<GeoJsonConfiguration> {

    @Override
    GeoJsonConfiguration getFull() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .enabled(true)
                .nestedObjectStrategy(FeatureTransformerBase.NESTED_OBJECTS.NEST)
                .multiplicityStrategy(FeatureTransformerBase.MULTIPLICITY.ARRAY)
                .useFormattedJsonOutput(true)
                .separator(".")
                .putTransformations("foo", new ImmutablePropertyTransformation.Builder().rename("bar").build())
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
                .nestedObjectStrategy(FeatureTransformerBase.NESTED_OBJECTS.FLATTEN)
                .multiplicityStrategy(FeatureTransformerBase.MULTIPLICITY.SUFFIX)
                .useFormattedJsonOutput(false)
                .separator("-")
                .build()
    }

    @Override
    GeoJsonConfiguration getSimpleFullMerged() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }

    @Override
    GeoJsonConfiguration getMap() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .putTransformations("bar", new ImmutablePropertyTransformation.Builder().rename("foo").build())
                .build()
    }

    @Override
    GeoJsonConfiguration getMapFullMerged() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", new ImmutablePropertyTransformation.Builder().rename("bar").build(),
                        "bar", new ImmutablePropertyTransformation.Builder().rename("foo").build()
                ))
                .build()
    }

    @Override
    GeoJsonConfiguration getNested() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .putTransformations("foo", new ImmutablePropertyTransformation.Builder().codelist("cl").build())
                .build()
    }

    @Override
    GeoJsonConfiguration getNestedFullMerged() {
        return new ImmutableGeoJsonConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", new ImmutablePropertyTransformation.Builder().rename("bar").codelist("cl").build()
                ))
                .build()
    }
}
