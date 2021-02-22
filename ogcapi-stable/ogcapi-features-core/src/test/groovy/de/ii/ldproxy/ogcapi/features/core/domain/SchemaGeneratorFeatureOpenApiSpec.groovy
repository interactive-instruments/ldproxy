package de.ii.ldproxy.ogcapi.features.core.domain

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Shared
import spock.lang.Specification

class SchemaGeneratorFeatureOpenApiSpec extends Specification {

    @Shared SchemaGeneratorFeatureOpenApi schemaGenerator

    def setupSpec() {
        schemaGenerator = new SchemaGeneratorFeatureOpenApi()
        schemaGenerator.schemaInfo = new SchemaInfo()
    }

    def 'Test Open API schema generation for QUERYABLES type'() {
        given:
        FeatureSchema featureSchema = new ImmutableFeatureSchema.Builder()
                .name("test-name")
                .description("foobar")
                .putPropertyMap("property1", new ImmutableFeatureSchema.Builder()
                        .name("date")
                        .type(SchemaBase.Type.DATETIME)
                        .build())
                .type(SchemaBase.Type.OBJECT)
                .build()
        FeatureTypeConfigurationOgcApi collectionData = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                .id("test-id")
                .label("test-label")
                .addExtensions(new ImmutableFeaturesCoreConfiguration.Builder()
                        .queryables(new ImmutableFeaturesCollectionQueryables.Builder()
                                .temporal(Collections.singletonList("date"))
                                .build())
                        .build())
                .build()
        when:
        Schema schema = schemaGenerator.getSchemaOpenApi(featureSchema, collectionData, SchemaGeneratorFeature.SCHEMA_TYPE.QUERYABLES)
        then:
        Objects.nonNull(schema)
        schema.getTitle() == "test-label"
        schema.getDescription() == "foobar"
        schema.getType() == "object"
        schema.getProperties().get("date").getType() == "string"
        schema.getProperties().get("date").getFormat() == "date-time"
    }

    def 'Test Open API schema generation for RETURNABLES type'() {
        given:
        FeatureSchema featureSchema = new ImmutableFeatureSchema.Builder()
                .name("test-name")
                .description("foobar")
                .putPropertyMap("property1", new ImmutableFeatureSchema.Builder()
                        .name("property1")
                        .type(SchemaBase.Type.OBJECT)
                        .build())
                .putPropertyMap("property2", new ImmutableFeatureSchema.Builder()
                        .name("geometry")
                        .type(SchemaBase.Type.GEOMETRY)
                        .geometryType(SimpleFeatureGeometry.POINT)
                        .build())
                .putPropertyMap("property3", new ImmutableFeatureSchema.Builder()
                        .name("id")
                        .type(SchemaBase.Type.STRING)
                        .role(SchemaBase.Role.ID)
                        .build())
                .type(SchemaBase.Type.OBJECT)
                .build()
        FeatureTypeConfigurationOgcApi collectionData = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                .id("test-id")
                .label("test-label")
                .build()
        when:
        Schema schema = schemaGenerator.getSchemaOpenApi(featureSchema, collectionData, SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES)
        then:
        Objects.nonNull(schema)
        schema.getRequired() == ["type", "geometry", "properties"]
        schema.getProperties().get("properties").getProperties().get("property1").getType() == "object"
        schema.getProperties().get("type").getType() == "string"
        schema.getProperties().get("id").getType() == "string"
        schema.getProperties().get("links").getType() == "array"
    }

    def 'Test Open API schema generation for RETURNABLES_FLAT type'() {
        FeatureSchema featureSchema = new ImmutableFeatureSchema.Builder()
                .name("test-name")
                .description("foobar")
                .putPropertyMap("property1", new ImmutableFeatureSchema.Builder()
                        .name("property1")
                        .type(SchemaBase.Type.OBJECT)
                        .build())
                .putPropertyMap("property2", new ImmutableFeatureSchema.Builder()
                        .name("geometry")
                        .type(SchemaBase.Type.GEOMETRY)
                        .geometryType(SimpleFeatureGeometry.POINT)
                        .build())
                .putPropertyMap("property3", new ImmutableFeatureSchema.Builder()
                        .name("id")
                        .type(SchemaBase.Type.STRING)
                        .role(SchemaBase.Role.ID)
                        .build())
                .type(SchemaBase.Type.OBJECT)
                .build()
        FeatureTypeConfigurationOgcApi collectionData = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                .id("test-id")
                .label("test-label")
                .build()
        when:
        Schema schema = schemaGenerator.getSchemaOpenApi(featureSchema, collectionData, SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES)
        then:
        Objects.nonNull(schema)
        schema.getRequired() == ["type", "geometry", "properties"]
        schema.getProperties().get("properties").getProperties().get("property1").getType() == "object"
        schema.getProperties().get("type").getType() == "string"
        schema.getProperties().get("id").getType() == "string"
        schema.getProperties().get("links").getType() == "array"
    }

}
