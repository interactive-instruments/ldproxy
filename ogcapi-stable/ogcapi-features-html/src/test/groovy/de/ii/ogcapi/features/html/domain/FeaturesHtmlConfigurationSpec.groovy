/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.domain

import com.google.common.collect.ImmutableMap
import de.ii.ogcapi.foundation.domain.AbstractExtensionConfigurationSpec
import de.ii.ogcapi.foundation.domain.MergeBase
import de.ii.ogcapi.foundation.domain.MergeMap
import de.ii.ogcapi.foundation.domain.MergeMinimal
import de.ii.ogcapi.foundation.domain.MergeNested
import de.ii.ogcapi.foundation.domain.MergeSimple
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation

@SuppressWarnings('ClashingTraitMethods')
class FeaturesHtmlConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<FeaturesHtmlConfiguration>, MergeMinimal<FeaturesHtmlConfiguration>, MergeSimple<FeaturesHtmlConfiguration>, MergeMap<FeaturesHtmlConfiguration>, MergeNested<FeaturesHtmlConfiguration> {

    @Override
    FeaturesHtmlConfiguration getFull() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .enabled(true)
                .layout(FeaturesHtmlConfiguration.LAYOUT.CLASSIC)
                .featureTitleTemplate("foo")
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()])
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getMinimal() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    FeaturesHtmlConfiguration getSimple() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .enabled(false)
                .layout(FeaturesHtmlConfiguration.LAYOUT.COMPLEX_OBJECTS)
                .featureTitleTemplate("bar")
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getSimpleFullMerged() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .transformations(ImmutableMap.of(
                        "foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()],
                        "*{objectType=Link}", [new ImmutablePropertyTransformation.Builder().objectReduceFormat("<a href=\"{{href}}\">{{title}}</a>").build()],
                        "*", [new ImmutablePropertyTransformation.Builder().flatten(".").build()]
                ))
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getMap() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .putTransformations("bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()])
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getMapFullMerged() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", [new ImmutablePropertyTransformation.Builder().rename("bar").build()],
                        "bar", [new ImmutablePropertyTransformation.Builder().rename("foo").build()]
                ))
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getNested() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .putTransformations("foo", [new ImmutablePropertyTransformation.Builder().codelist("cl").build()])
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getNestedFullMerged() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
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
