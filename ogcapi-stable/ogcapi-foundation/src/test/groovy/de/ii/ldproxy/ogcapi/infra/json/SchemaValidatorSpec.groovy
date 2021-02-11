package de.ii.ldproxy.ogcapi.infra.json

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
}
