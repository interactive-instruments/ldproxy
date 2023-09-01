/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.ogcapi.foundation.domain.*
import de.ii.ogcapi.tiles.domain.ImmutableTilesConfiguration
import de.ii.ogcapi.tiles.domain.TilesConfiguration
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation
import de.ii.xtraplatform.tiles.domain.ImmutableLevelFilter
import de.ii.xtraplatform.tiles.domain.ImmutableLevelTransformation
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax

@SuppressWarnings('ClashingTraitMethods')
class TilesConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<TilesConfiguration>, MergeMinimal<TilesConfiguration>, MergeSimple<TilesConfiguration>, MergeCollection<TilesConfiguration>, MergeMap<TilesConfiguration>, MergeNested<TilesConfiguration> {
    @Override
    TilesConfiguration getFull() {
        return new ImmutableTilesConfiguration.Builder()
                .enabled(true)
                .collectionTiles(true)
                .datasetTiles(true)
                .ignoreInvalidGeometries(true)
                .limit(1)
                .minimumSizeInPixel(1)
                .center(ImmutableList.<Double> of(1))
                .addTileEncodings("foo")
                .addTileSetEncodings("foo")
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()])
                .putZoomLevels("foo", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putZoomLevelsCache("foo", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putSeeding("foo", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putFilters("foo", ImmutableList.of(new ImmutableLevelFilter.Builder().min(1).max(10).filter("").build()))
                .putRules("foo", ImmutableList.of(new ImmutableLevelTransformation.Builder().min(1).max(10).build()))
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
                .collectionTiles(false)
                .datasetTiles(false)
                .ignoreInvalidGeometries(false)
                .limit(10)
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
                .center(ImmutableList.<Double> of(1, 2))
                .addTileEncodings("foo", "bar")
                .addTileSetEncodings("bar")
                .build()
    }

    @Override
    TilesConfiguration getCollectionFullMerged() {
        return new ImmutableTilesConfiguration.Builder()
                .from(getFull())
                .center(ImmutableList.<Double> of(1, 2))
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
                .putTransformations("bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()])
                .putZoomLevels("bar", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putZoomLevelsCache("bar", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putSeeding("bar", new ImmutableMinMax.Builder().min(1).max(10).build())
                .putFilters("bar", ImmutableList.of(new ImmutableLevelFilter.Builder().min(1).max(10).filter("").build()))
                .putRules("bar", ImmutableList.of(new ImmutableLevelTransformation.Builder().min(1).max(10).build()))
                .build()
    }

    @Override
    TilesConfiguration getMapFullMerged() {
        return new ImmutableTilesConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()],
                        "bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()]
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
                        "foo", ImmutableList.of(new ImmutableLevelFilter.Builder().min(1).max(10).filter("").build()),
                        "bar", ImmutableList.of(new ImmutableLevelFilter.Builder().min(1).max(10).filter("").build())
                ))
                .rules(ImmutableMap.of(
                        "foo", ImmutableList.of(new ImmutableLevelTransformation.Builder().min(1).max(10).build()),
                        "bar", ImmutableList.of(new ImmutableLevelTransformation.Builder().min(1).max(10).build())
                ))
                .build()
    }

    @Override
    TilesConfiguration getNested() {
        return new ImmutableTilesConfiguration.Builder()
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().codelist("cl").build()])
                .build()
    }

    @Override
    TilesConfiguration getNestedFullMerged() {
        return new ImmutableTilesConfiguration.Builder()
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
