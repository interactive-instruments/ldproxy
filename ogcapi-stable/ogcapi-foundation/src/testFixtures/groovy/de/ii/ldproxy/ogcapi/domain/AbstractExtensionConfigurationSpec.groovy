package de.ii.ldproxy.ogcapi.domain

import spock.lang.Specification
import spock.lang.Unroll

abstract class AbstractExtensionConfigurationSpec extends Specification{

    abstract def getUseCases()

    @Unroll //TODO: should not be needed, spock 2.0 unrolls by default
    def "merge #usecase"() {

        when: "#when"

        def actual = source.mergeInto(target);

        then: "#then"

        actual == expected

        where:

        [usecase, when, then, source, target, expected] << getUseCases()
    }
}
