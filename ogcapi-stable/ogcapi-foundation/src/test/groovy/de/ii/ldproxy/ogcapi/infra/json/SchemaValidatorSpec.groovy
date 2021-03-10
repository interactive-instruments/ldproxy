package de.ii.ldproxy.ogcapi.infra.json

import com.fasterxml.jackson.core.JsonParseException
import com.networknt.schema.SpecVersion
import spock.lang.Shared
import spock.lang.Specification

class SchemaValidatorSpec extends Specification {

    @Shared SchemaValidatorImpl schemaValidator

    def setupSpec() {
        schemaValidator = new SchemaValidatorImpl()
    }

    def "Validate a feature against its JSON schema (draft 2019-09, explicit)"() {
        given:
        String schema = new File('src/test/resources/schema.json').getText()
        String feature = new File('src/test/resources/feature.json').getText()
        when:
        Optional<String> result = schemaValidator.validate(schema, feature, SpecVersion.VersionFlag.V201909)
        then:
        result.isEmpty()
    }

    def "Validate a feature against its JSON schema (draft 2019-09, auto-detect)"() {
        given:
        String schema = new File('src/test/resources/schema.json').getText()
        String feature = new File('src/test/resources/feature.json').getText()
        when:
        Optional<String> result = schemaValidator.validate(schema, feature)
        then:
        result.isEmpty()
    }

    def "Validate a feature against its JSON schema (draft 07, explicit)"() {
        given:
        String schema = new File('src/test/resources/schema-draft07.json').getText()
        String feature = new File('src/test/resources/feature.json').getText()
        when:
        Optional<String> result = schemaValidator.validate(schema, feature, SpecVersion.VersionFlag.V7)
        then:
        result.isEmpty()
    }

    def "Validate a feature against its JSON schema (draft 07, auto-detect)"() {
        given:
        String schema = new File('src/test/resources/schema-draft07.json').getText()
        String feature = new File('src/test/resources/feature.json').getText()
        when:
        Optional<String> result = schemaValidator.validate(schema, feature)
        then:
        result.isEmpty()
    }

    def "Validate and catch errors"() {
        when:
        schemaValidator.validate(schema, feature)
        then:
        thrown(exception)
        where:
        schema                                                | feature                                                | exception
        // empty feature and empty schema
        ""                                                    | ""                                                     | NullPointerException
        // proper feature and empty schema
        ""                                                    | new File('src/test/resources/feature.json').getText()  | NullPointerException
        // empty feature and proper schema
        new File('src/test/resources/schema.json').getText()  | ""                                                     | NullPointerException
        // proper schema, invalid feature json
        new File('src/test/resources/schema.json').getText()  | new File('src/test/resources/feature2.json').getText() | JsonParseException
        // invalid schema json, proper feature
        new File('src/test/resources/schema2.json').getText() | new File('src/test/resources/feature.json').getText()  | JsonParseException
    }
}
