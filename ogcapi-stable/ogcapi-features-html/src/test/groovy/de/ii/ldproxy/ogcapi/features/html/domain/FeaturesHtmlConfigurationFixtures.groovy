/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.domain

import com.google.common.collect.ImmutableMap
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutablePropertyTransformation

class FeaturesHtmlConfigurationFixtures {

    static final FeaturesHtmlConfiguration ENABLED_ONLY = new ImmutableFeaturesHtmlConfiguration.Builder()
            .enabled(true)
            .build()

    static final FeaturesHtmlConfiguration FULL_1 = new ImmutableFeaturesHtmlConfiguration.Builder()
            .enabled(false)
            .layout(FeaturesHtmlConfiguration.LAYOUT.CLASSIC)
            .schemaOrgEnabled(true)
            .itemLabelFormat("foo")
            .putTransformations("foo", new ImmutablePropertyTransformation.Builder().rename("bar").build())
            .build()

    static final FeaturesHtmlConfiguration FULL_2 = new ImmutableFeaturesHtmlConfiguration.Builder()
            .enabled(true)
            .layout(FeaturesHtmlConfiguration.LAYOUT.COMPLEX_OBJECTS)
            .schemaOrgEnabled(false)
            .itemLabelFormat("bar")
            .putTransformations("bar", new ImmutablePropertyTransformation.Builder().rename("foo").build())
            .build()


    static final FeaturesHtmlConfiguration ENABLED_FULL_1_EXPECTED = new ImmutableFeaturesHtmlConfiguration.Builder()
            .from(FULL_1)
            .enabled(true)
            .build()

    static final FeaturesHtmlConfiguration FULL_2_FULL_1_EXPECTED = new ImmutableFeaturesHtmlConfiguration.Builder()
            .from(FULL_2)
            .transformations(ImmutableMap.of(
                    "foo", new ImmutablePropertyTransformation.Builder().rename("bar").build(),
                    "bar", new ImmutablePropertyTransformation.Builder().rename("foo").build()
            ))
            .build()

}
