/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain

import com.google.common.collect.ImmutableMap
import de.ii.ogcapi.foundation.domain.*
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation

@SuppressWarnings('ClashingTraitMethods')
class FeaturesCoreConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<FeaturesCoreConfiguration>, MergeMinimal<FeaturesCoreConfiguration>, MergeSimple<FeaturesCoreConfiguration>, MergeMap<FeaturesCoreConfiguration>, MergeNested<FeaturesCoreConfiguration> {

    @Override
    FeaturesCoreConfiguration getFull() {
        return new ImmutableFeaturesCoreConfiguration.Builder()
                .enabled(true)
                .featureProvider("foo")
                .featureType("bar")
                .defaultCrs(FeaturesCoreConfiguration.DefaultCrs.CRS84)
                .minimumPageSize(1)
                .defaultPageSize(5)
                .maximumPageSize(10)
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()])
                .build()
    }

    @Override
    FeaturesCoreConfiguration getMinimal() {
        return new ImmutableFeaturesCoreConfiguration.Builder()
                .build()
    }

    @Override
    FeaturesCoreConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    FeaturesCoreConfiguration getSimple() {
        return new ImmutableFeaturesCoreConfiguration.Builder()
                .enabled(false)
                .featureProvider("bar")
                .featureType("foo")
                .defaultCrs(FeaturesCoreConfiguration.DefaultCrs.CRS84h)
                .minimumPageSize(10)
                .defaultPageSize(50)
                .maximumPageSize(100)
                .build()
    }

    @Override
    FeaturesCoreConfiguration getSimpleFullMerged() {
        return new ImmutableFeaturesCoreConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }

    @Override
    FeaturesCoreConfiguration getMap() {
        //TODO: test deep merge
        return new ImmutableFeaturesCoreConfiguration.Builder()
                .putTransformations("bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()])
                .build()
    }

    @Override
    FeaturesCoreConfiguration getMapFullMerged() {
        return new ImmutableFeaturesCoreConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()],
                        "bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()]
                ))
                .build()
    }

    @Override
    FeaturesCoreConfiguration getNested() {
        return new ImmutableFeaturesCoreConfiguration.Builder()
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().codelist("cl").build()])
                .build()
    }

    @Override
    FeaturesCoreConfiguration getNestedFullMerged() {
        return new ImmutableFeaturesCoreConfiguration.Builder()
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
