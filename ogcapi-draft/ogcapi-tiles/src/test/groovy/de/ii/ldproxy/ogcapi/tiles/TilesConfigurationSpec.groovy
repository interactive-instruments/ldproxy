/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.domain.AbstractExtensionConfigurationSpec
import de.ii.ldproxy.ogcapi.domain.MergeBase
import de.ii.ldproxy.ogcapi.domain.MergeCollection
import de.ii.ldproxy.ogcapi.domain.MergeMap
import de.ii.ldproxy.ogcapi.domain.MergeMinimal
import de.ii.ldproxy.ogcapi.domain.MergeNested
import de.ii.ldproxy.ogcapi.domain.MergeSimple
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutablePropertyTransformation
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableMinMax
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutablePredefinedFilter
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableRule
import de.ii.ldproxy.ogcapi.tiles.domain.ImmutableTilesConfiguration
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration

@SuppressWarnings('ClashingTraitMethods')
class TilesConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<TilesConfiguration>, MergeMinimal<TilesConfiguration>, MergeSimple<TilesConfiguration>, MergeCollection<TilesConfiguration>, MergeMap<TilesConfiguration>, MergeNested<TilesConfiguration> {
    @Override
    TilesConfiguration getFull() {
        return new ImmutableTilesConfiguration.Builder()
                .enabled(true)
                .singleCollectionEnabled(true)
                .multiCollectionEnabled(true)
                .ignoreInvalidGeometries(true)
                .limit(1)
                .maxPointPerTileDefault(1)
                .maxLineStringPerTileDefault(1)
                .maxPolygonPerTileDefault(1)
                .maxRelativeAreaChangeInPolygonRepair(1)
                .maxAbsoluteAreaChangeInPolygonRepair(1)
                .minimumSizeInPixel(1)
                .center(1)
                .addTileEncodings("foo")
                .addTileSetEncodings("foo")
                .putTransformations("foo", new ImmutablePropertyTransformation.Builder().rename("bar").build())
                .putZoomLevels("foo", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putZoomLevelsCache("foo", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putSeeding("foo", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putFilters("foo", ImmutableList.of(new ImmutablePredefinedFilter.Builder().min(1).max(10).build()))
                .putRules("foo", ImmutableList.of(new ImmutableRule.Builder().min(1).max(10).build()))
                .build()
    }

    @Override
    TilesConfiguration getMinimal() {
        return new ImmutableTilesConfiguration.Builder()
                .build()
    }

    @Override
    TilesConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    TilesConfiguration getSimple() {
        return new ImmutableTilesConfiguration.Builder()
                .enabled(false)
                .singleCollectionEnabled(false)
                .multiCollectionEnabled(false)
                .ignoreInvalidGeometries(false)
                .limit(10)
                .maxPointPerTileDefault(10)
                .maxLineStringPerTileDefault(10)
                .maxPolygonPerTileDefault(10)
                .maxRelativeAreaChangeInPolygonRepair(10)
                .maxAbsoluteAreaChangeInPolygonRepair(10)
                .minimumSizeInPixel(10)
                .build()
    }

    @Override
    TilesConfiguration getSimpleFullMerged() {
        return new ImmutableTilesConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }

    @Override
    TilesConfiguration getCollection() {
        return new ImmutableTilesConfiguration.Builder()
                .center(1, 2)
                .addTileEncodings("foo", "bar")
                .addTileSetEncodings("bar")
                .build()
    }

    @Override
    TilesConfiguration getCollectionFullMerged() {
        return new ImmutableTilesConfiguration.Builder()
                .from(getFull())
                .center(1, 2)
                .tileEncodings(ImmutableList.of(
                        "foo",
                        "bar"
                ))
                .tileSetEncodings(ImmutableList.of(
                        "foo",
                        "bar"
                ))
                .build()
    }

    @Override
    TilesConfiguration getMap() {
        return new ImmutableTilesConfiguration.Builder()
                .putTransformations("bar", new ImmutablePropertyTransformation.Builder().rename("foo").build())
                .putZoomLevels("bar", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putZoomLevelsCache("bar", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putSeeding("bar", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putFilters("bar", ImmutableList.of(new ImmutablePredefinedFilter.Builder().min(1).max(10).build()))
                .putRules("bar", ImmutableList.of(new ImmutableRule.Builder().min(1).max(10).build()))
                .build()
    }

    @Override
    TilesConfiguration getMapFullMerged() {
        return new ImmutableTilesConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", new ImmutablePropertyTransformation.Builder().rename("bar").build(),
                        "bar", new ImmutablePropertyTransformation.Builder().rename("foo").build()
                ))
                .zoomLevels(ImmutableMap.of(
                        "foo", new ImmutableMinMax.Builder().min(1).max(10).build(),
                        "bar", new ImmutableMinMax.Builder().min(1).max(10).build()
                ))
                .zoomLevelsCache(ImmutableMap.of(
                        "foo", new ImmutableMinMax.Builder().min(1).max(10).build(),
                        "bar", new ImmutableMinMax.Builder().min(1).max(10).build()
                ))
                .seeding(ImmutableMap.of(
                        "foo", new ImmutableMinMax.Builder().min(1).max(10).build(),
                        "bar", new ImmutableMinMax.Builder().min(1).max(10).build()
                ))
                .filters(ImmutableMap.of(
                        "foo", ImmutableList.of(new ImmutablePredefinedFilter.Builder().min(1).max(10).build()),
                        "bar", ImmutableList.of(new ImmutablePredefinedFilter.Builder().min(1).max(10).build())
                ))
                .rules(ImmutableMap.of(
                        "foo", ImmutableList.of(new ImmutableRule.Builder().min(1).max(10).build()),
                        "bar", ImmutableList.of(new ImmutableRule.Builder().min(1).max(10).build())
                ))
                .build()
    }

    @Override
    TilesConfiguration getNested() {
        return new ImmutableTilesConfiguration.Builder()
                .putTransformations("foo", new ImmutablePropertyTransformation.Builder().codelist("cl").build())
                .build()
    }

    @Override
    TilesConfiguration getNestedFullMerged() {
        return new ImmutableTilesConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", new ImmutablePropertyTransformation.Builder().rename("bar").codelist("cl").build()
                ))
                .build()
    }
}
