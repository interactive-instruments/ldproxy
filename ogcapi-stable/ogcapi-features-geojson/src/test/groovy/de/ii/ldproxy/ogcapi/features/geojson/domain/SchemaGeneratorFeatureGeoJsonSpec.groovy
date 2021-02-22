package de.ii.ldproxy.ogcapi.features.geojson.domain

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCollectionQueryables
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import spock.lang.Shared
import spock.lang.Specification

class SchemaGeneratorFeatureGeoJsonSpec extends Specification {

    @Shared SchemaGeneratorFeatureGeoJson schemaGenerator

    def setupSpec() {
        schemaGenerator = new SchemaGeneratorFeatureGeoJson()
        schemaGenerator.schemaInfo = new SchemaInfo()
    }

    def 'Test GeoJSON schema generation for the QUERYABLES type'() {
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
        JsonSchemaObject jsonSchemaObject = schemaGenerator.getSchemaJson(featureSchema, collectionData, Optional.empty(), SchemaGeneratorFeature.SCHEMA_TYPE.QUERYABLES)
        then:
        Objects.nonNull(jsonSchemaObject)
        jsonSchemaObject.getSchema().get() == "https://json-schema.org/draft/2019-09/schema"
        jsonSchemaObject.getTitle().get() == "test-label"
        jsonSchemaObject.getDescription().get() == "foobar"
        JsonSchemaString date = jsonSchemaObject.getProperties().get("date") as JsonSchemaString
        date.getFormat().get() == "date-time,date"
    }

    def 'Test GeoJSON schema generation for the RETURNABLES type'() {
        given:
        FeatureSchema featureSchema = new ImmutableFeatureSchema.Builder()
                .name("test-name")
                .description("foobar")
                .putPropertyMap("property1", new ImmutableFeatureSchema.Builder()
                        .name("property1")
                        .type(SchemaBase.Type.OBJECT)
                        .putPropertyMap("nested1",new ImmutableFeatureSchema.Builder().name("nested1").type(SchemaBase.Type.STRING).build())
                        .putPropertyMap("nested2", new ImmutableFeatureSchema.Builder().name("nested2").type(SchemaBase.Type.FLOAT).build())
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
        JsonSchemaObject jsonSchemaObject = schemaGenerator.getSchemaJson(featureSchema, collectionData, Optional.empty(), SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES)
        then:
        Objects.nonNull(jsonSchemaObject)
        jsonSchemaObject.getSchema().get() == "https://json-schema.org/draft/2019-09/schema"
        jsonSchemaObject.getTitle().get() == "test-label"
        jsonSchemaObject.getDescription().get() == "foobar"
        jsonSchemaObject.getRequired() == ["type", "geometry", "properties"]
        Objects.nonNull(jsonSchemaObject.getProperties().get("id"))
        JsonSchemaRef geometry = jsonSchemaObject.getProperties().get("geometry") as JsonSchemaRef
        geometry.getRef() == "https://geojson.org/schema/Point.json"
        Objects.nonNull(jsonSchemaObject.getProperties().get("properties").getProperties().get("property1"))
    }

    def 'Test GeoJSON schema generation for the RETURNABLES_FLAT type'() {
        FeatureSchema featureSchema = new ImmutableFeatureSchema.Builder()
                .name("test-name")
                .description("foobar")
                .putPropertyMap("property1", new ImmutableFeatureSchema.Builder()
                        .name("property1")
                        .type(SchemaBase.Type.OBJECT)
                        .putPropertyMap("nested1",new ImmutableFeatureSchema.Builder().name("nested1").type(SchemaBase.Type.STRING).build())
                        .putPropertyMap("nested2", new ImmutableFeatureSchema.Builder().name("nested2").type(SchemaBase.Type.FLOAT).build())
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
        JsonSchemaObject jsonSchemaObject = schemaGenerator.getSchemaJson(featureSchema, collectionData, Optional.empty(), SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT)
        then:
        Objects.nonNull(jsonSchemaObject)
        jsonSchemaObject.getSchema().get() == "https://json-schema.org/draft/2019-09/schema"
        jsonSchemaObject.getTitle().get() == "test-label"
        jsonSchemaObject.getDescription().get() == "foobar"
        jsonSchemaObject.getRequired() == ["type", "geometry", "properties"]
        jsonSchemaObject.getProperties().get("id").getTitle().get() == "id"
        JsonSchemaRef geometry = jsonSchemaObject.getProperties().get("geometry") as JsonSchemaRef
        geometry.getRef() == "https://geojson.org/schema/Point.json"
        JsonSchemaString nested1 = jsonSchemaObject.getProperties().get("properties").getProperties().get("property1.nested1") as JsonSchemaString
        nested1.getTitle().get() == "property1 > nested1"
        JsonSchemaNumber nested2 = jsonSchemaObject.getProperties().get("properties").getProperties().get("property1.nested2") as JsonSchemaNumber
        nested2.getTitle().get() == "property1 > nested2"
    }

}
