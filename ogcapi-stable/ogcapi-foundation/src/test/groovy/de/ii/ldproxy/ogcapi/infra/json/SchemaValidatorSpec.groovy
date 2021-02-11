package de.ii.ldproxy.ogcapi.infra.json

import spock.lang.Shared
import spock.lang.Specification

class SchemaValidatorSpec extends Specification {

    @Shared SchemaValidatorImpl schemaValidator

    def setupSpec() {
        schemaValidator = new SchemaValidatorImpl()
    }

    def "Validate a feature against its JSON schema"() {
        given:
        String schema = new File('src/test/resources/schema.json').getText()
        String feature = new File('src/test/resources/feature.json').getText()
        when:
        Optional<String> result = schemaValidator.validate(schema, feature)
        then:
        result.isPresent()
    }

}
