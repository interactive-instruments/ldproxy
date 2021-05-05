/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain

import spock.lang.Specification
import spock.lang.Unroll

abstract class AbstractExtensionConfigurationSpec extends Specification{

    abstract def getUseCases()

    @Unroll //TODO: should not be needed, spock 2.0 unrolls by default
    def "merge #usecase"() {

        when: "#when"

        def actual = source.mergeInto(target)

        then: "#then"

        actual == expected

        where:

        [usecase, when, then, source, target, expected] << getUseCases()
    }
}
