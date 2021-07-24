/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.domain

import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutablePropertyTransformation
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonConfiguration
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration

@SuppressWarnings('ClashingTraitMethods')
class JsonFgConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<JsonFgConfiguration>, MergeMinimal<JsonFgConfiguration>, MergeSimple<JsonFgConfiguration>, MergeMap<JsonFgConfiguration>, MergeNested<JsonFgConfiguration> {

    @Override
    JsonFgConfiguration getFull() {
        return new ImmutableJsonFgConfiguration.Builder()
                .enabled(true)
                .addFeatureTypes("foo")
                .describedby(true)
                .when(true)
                .refSys(true)
                .where(new ImmutableWhereConfiguration.Builder()
                        .enabled(true)
                        .alwaysIncludeGeoJsonGeometry(true)
                        .maxAllowableOffsetGeometry(0.001)
                        .build())
                .build()
    }

    @Override
    JsonFgConfiguration getMinimal() {
        return new ImmutableJsonFgConfiguration.Builder()
                .build()
    }

    @Override
    JsonFgConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    JsonFgConfiguration getSimple() {
        return new ImmutableJsonFgConfiguration.Builder()
                .enabled(true)
                .addFeatureTypes("bar")
                .build()
    }

    @Override
    JsonFgConfiguration getSimpleFullMerged() {
        return new ImmutableJsonFgConfiguration.Builder()
                .enabled(true)
                .addFeatureTypes("bar")
                .describedby(true)
                .when(true)
                .where(new ImmutableWhereConfiguration.Builder()
                        .enabled(true)
                        .alwaysIncludeGeoJsonGeometry(true)
                        .maxAllowableOffsetGeometry(0.001)
                        .build())
                .refSys(true)
                .build()
    }

    @Override
    JsonFgConfiguration getMap() {
        return new ImmutableJsonFgConfiguration.Builder()
                .build()
    }

    @Override
    JsonFgConfiguration getMapFullMerged() {
        return getFull()
    }

    @Override
    JsonFgConfiguration getNested() {
        return new ImmutableJsonFgConfiguration.Builder()
                .where(new ImmutableWhereConfiguration.Builder()
                        .enabled(false)
                        .alwaysIncludeGeoJsonGeometry(false)
                        .maxAllowableOffsetGeometry(0.002)
                        .build())
                .build()
    }

    @Override
    JsonFgConfiguration getNestedFullMerged() {
        return new ImmutableJsonFgConfiguration.Builder()
                .enabled(true)
                .addFeatureTypes("foo")
                .describedby(true)
                .when(true)
                .refSys(true)
                .where(new ImmutableWhereConfiguration.Builder()
                        .enabled(false)
                        .alwaysIncludeGeoJsonGeometry(false)
                        .maxAllowableOffsetGeometry(0.002)
                        .build())
                .build()
    }
}
