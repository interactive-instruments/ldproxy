/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.domain


import spock.lang.Specification

class FeaturesHtmlConfigurationSpec extends Specification {

    def 'merge differing'() {

        when: "merging a full configuration into another full configuration with differing values"
        def actual = FeaturesHtmlConfigurationFixtures.FULL_2.mergeInto(FeaturesHtmlConfigurationFixtures.FULL_1);

        then: 'simple values from the source configuration should override target values, maps should be merged'

        actual == FeaturesHtmlConfigurationFixtures.FULL_2_FULL_1_EXPECTED
    }

    def 'merge nullables'() {

        when: "merging a configuration with null values into a full configuration"
        def actual = FeaturesHtmlConfigurationFixtures.ENABLED_ONLY.mergeInto(FeaturesHtmlConfigurationFixtures.FULL_1);

        then: 'null values should not override target values'

        actual == FeaturesHtmlConfigurationFixtures.ENABLED_FULL_1_EXPECTED
    }

}
