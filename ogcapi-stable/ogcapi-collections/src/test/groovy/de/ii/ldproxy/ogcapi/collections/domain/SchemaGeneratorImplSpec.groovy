package de.ii.ldproxy.ogcapi.collections.domain

import de.ii.ldproxy.ogcapi.infra.json.SchemaGeneratorImpl
import de.ii.ldproxy.ogcapi.common.domain.LandingPage
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclaration
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Shared
import spock.lang.Specification

class SchemaGeneratorImplSpec extends Specification {

    @Shared SchemaGeneratorImpl schemaGenerator

    def setupSpec() {
        schemaGenerator = new SchemaGeneratorImpl()
    }

    def 'Test Landing Page schema generation'() {
        when:
        Schema schema = schemaGenerator.getSchema(LandingPage.class)
        then:
        Objects.nonNull(schema)
        schema.getType() == "object"
        schema.getProperties().get("extent").getType() == "object"
        schema.getProperties().get("extent").getProperties().get("spatial").getType()  == "object"
        schema.getProperties().get("extent").getProperties().get("temporal").getType() == "object"
        schema.getProperties().get("externalDocs").getType() == "object"
        schema.getProperties().get("externalDocs").getProperties().get("description").getType() == "string"
        schema.getProperties().get("externalDocs").getProperties().get("url").getType() == "string"
        schema.getProperties().get("description").getType() == "string"
        schema.getProperties().get("title").getType() == "string"
        schema.getProperties().get("links").getType() == "array"
    }

    def 'Test Conformance Declaration schema generation'() {
        when:
        Schema schema = schemaGenerator.getSchema(ConformanceDeclaration.class)
        then:
        Objects.nonNull(schema)
        schema.getType() == "object"
        schema.getProperties().get("conformsTo").getType() == "array"
        schema.getProperties().get("links").getType() == "array"
        schema.getProperties().get("title").getType() == "string"
        schema.getProperties().get("description").getType() == "string"
    }

    def 'Test Collections schema generation'() {
        when:
        Schema schema = schemaGenerator.getSchema(Collections.class)
        then:
        Objects.nonNull(schema)
        schema.getType() == "object"
        schema.getProperties().get("collections").getType() == "array"
        schema.getProperties().get("crs").getType() == "array"
        schema.getProperties().get("description").getType() == "string"
        schema.getProperties().get("title").getType() == "string"
        schema.getProperties().get("links").getType() == "array"
    }

    def 'Test OgcApiCollection schema generation'() {
        when:
        Schema schema = schemaGenerator.getSchema(OgcApiCollection.class)
        then:
        Objects.nonNull(schema)
        schema.getType() == "object"
        schema.getProperties().get("extent").getType() == "object"
        schema.getProperties().get("extent").getProperties().get("spatial").getType()  == "object"
        schema.getProperties().get("extent").getProperties().get("temporal").getType() == "object"
        schema.getProperties().get("crs").getType() == "array"
        schema.getProperties().get("storageCrs").getType() == "string"
        schema.getProperties().get("storageCrsCoordinateEpoch").getType() == "number"
        schema.getProperties().get("nativeCrs").getType() == "string"
        schema.getProperties().get("nativeCrsCoordinateEpoch").getType() == "number"
        schema.getProperties().get("itemType").getType() == "string"
        schema.getProperties().get("id").getType() == "string"
        schema.getProperties().get("description").getType() == "string"
        schema.getProperties().get("title").getType() == "string"
        schema.getProperties().get("links").getType() == "array"
    }

}
