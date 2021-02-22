/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.domain

import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.domain.*
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutablePropertyTransformation

@SuppressWarnings('ClashingTraitMethods')
class FeaturesHtmlConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<FeaturesHtmlConfiguration>, MergeMinimal<FeaturesHtmlConfiguration>, MergeSimple<FeaturesHtmlConfiguration>, MergeMap<FeaturesHtmlConfiguration> {

    @Override
    FeaturesHtmlConfiguration getFull() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .enabled(true)
                .layout(FeaturesHtmlConfiguration.LAYOUT.CLASSIC)
                .schemaOrgEnabled(true)
                .itemLabelFormat("foo")
                .putTransformations("foo", new ImmutablePropertyTransformation.Builder().rename("bar").build())
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
                .schemaOrgEnabled(false)
                .itemLabelFormat("bar")
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getSimpleFullMerged() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getMap() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .putTransformations("bar", new ImmutablePropertyTransformation.Builder().rename("foo").build())
                .build()
    }

    @Override
    FeaturesHtmlConfiguration getMapFullMerged() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
                .from(getFull())
                .transformations(ImmutableMap.of(
                        "foo", new ImmutablePropertyTransformation.Builder().rename("bar").build(),
                        "bar", new ImmutablePropertyTransformation.Builder().rename("foo").build()
                ))
                .build()
    }
}
