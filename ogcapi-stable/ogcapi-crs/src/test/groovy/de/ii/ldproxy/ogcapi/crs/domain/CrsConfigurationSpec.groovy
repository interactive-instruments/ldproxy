/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs.domain


import spock.lang.Specification

class CrsConfigurationSpec extends Specification {

    def 'merge differing'() {

        when: "merging a full configuration into another full configuration with differing values"
        def actual = CrsConfigurationFixtures.FULL_2.mergeInto(CrsConfigurationFixtures.FULL_1);

        then: 'simple values from the source configuration should override target values, maps and arrays should be merged'

        actual == CrsConfigurationFixtures.FULL_2_FULL_1_EXPECTED
    }

    def 'merge minimal'() {

        when: "merging a minimal configuration into a full configuration"
        def actual = CrsConfigurationFixtures.ENABLED_ONLY.mergeInto(CrsConfigurationFixtures.FULL_1);

        then: 'null values should not override target values'

        actual == CrsConfigurationFixtures.ENABLED_FULL_1_EXPECTED
    }

}
