package de.ii.ldproxy.ogcapi.features.geojson.domain

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCollectionQueryables
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaGeneratorFeature
import de.ii.ldproxy.ogcapi.features.core.domain.SchemaInfo
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import de.ii.xtraplatform.store.app.entities.EntityRegistryImpl
import spock.lang.Shared
import spock.lang.Specification

class SchemaGeneratorFeatureGeoJsonSpec extends Specification {

    @Shared SchemaGeneratorFeatureGeoJson schemaGenerator

    def setupSpec() {
        schemaGenerator = new SchemaGeneratorFeatureGeoJson()
        schemaGenerator.schemaInfo = new SchemaInfo()
        schemaGenerator.entityRegistry = new EntityRegistryImpl(null)
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
        FeatureSchema featureSchema = getSchemaObjectReturnables()
        FeatureTypeConfigurationOgcApi collectionData = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                .id("id")
                .label("foo")
                .build()
        when:
        JsonSchemaObject jsonSchemaObject = schemaGenerator.getSchemaJson(featureSchema, collectionData, Optional.empty(), SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES)
        then:
        Objects.nonNull(jsonSchemaObject)
        jsonSchemaObject.getSchema().get() == "https://json-schema.org/draft/2019-09/schema"
        jsonSchemaObject.getTitle().get() == "foo"
        jsonSchemaObject.getDescription().get() == "bar"
        jsonSchemaObject.getRequired() == ["type", "geometry", "properties"]
        jsonSchemaObject.getProperties().get("id").getTitle().get() == "foo"
        jsonSchemaObject.getProperties().get("id").getDescription().get() == "bar"
        JsonSchemaRef geometry = jsonSchemaObject.getProperties().get("geometry") as JsonSchemaRef
        geometry.getRef() == "https://geojson.org/schema/MultiPolygon.json"
        JsonSchemaObject properties = jsonSchemaObject.getProperties().get("properties") as JsonSchemaObject
        properties.getRequired() == ["string"]
        properties.getProperties().get("boolean").getTitle().get() == "foo"
        properties.getProperties().get("boolean").getDescription().get() == "bar"
        properties.getProperties().get("datetime").getTitle().get() == "foo"
        properties.getProperties().get("datetime").getDescription().get() == "bar"
        (properties.getProperties().get("datetime") as JsonSchemaString).getFormat().get() == "date-time,date"
        properties.getProperties().get("endLifespanVersion").getTitle().get() == "foo"
        properties.getProperties().get("endLifespanVersion").getDescription().get() == "bar"
        (properties.getProperties().get("links") as JsonSchemaArray).getMinItems().get() == 1
        (properties.getProperties().get("links") as JsonSchemaArray).getMaxItems().get() == 5
        properties.getProperties().get("percent").getTitle().get() == "foo"
        properties.getProperties().get("percent").getDescription().get() == "bar"
        properties.getProperties().get("string").getTitle().get() == "foo"
        properties.getProperties().get("string").getDescription().get() == "bar"
        (properties.getProperties().get("strings") as JsonSchemaArray).getItems().getTitle().get() == "foo"
        (properties.getProperties().get("strings") as JsonSchemaArray).getItems().getDescription().get() == "bar"
        jsonSchemaObject.getDefs().get().get("Object1").getTitle().get() == "foo"
        jsonSchemaObject.getDefs().get().get("Object1").getDescription().get() == "bar"
        (jsonSchemaObject.getDefs().get().get("Object1").getProperties().get("date") as JsonSchemaString).getFormat().get() == "date-time,date"
        (jsonSchemaObject.getDefs().get().get("Object1").getProperties().get("integer") as JsonSchemaInteger).getTitle().get() == "foo"
        (jsonSchemaObject.getDefs().get().get("Object1").getProperties().get("integer") as JsonSchemaInteger).getDescription().get() == "bar"
        jsonSchemaObject.getDefs().get().get("Object1").getProperties().get("object2") != null
        (jsonSchemaObject.getDefs().get().get("Object2").getProperties().get("enum") as JsonSchemaString).getEnums() == ["foo", "bar"]
        (jsonSchemaObject.getDefs().get().get("Object2").getProperties().get("regex") as JsonSchemaString).getPattern().get() == "'^_\\\\w+\$'"
        jsonSchemaObject.getDefs().get().get("Object2").getProperties().get("codelist") != null
        jsonSchemaObject.getDefs().get().get("Object2").getProperties().get("strings") != null

    }

    def 'Test GeoJSON schema generation for the RETURNABLES_FLAT type'() {
        FeatureSchema featureSchema = getSchemaObjectReturnables()
        FeatureTypeConfigurationOgcApi collectionData = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                .id("id")
                .label("foo")
                .build()
        when:
        JsonSchemaObject jsonSchemaObject = schemaGenerator.getSchemaJson(featureSchema, collectionData, Optional.empty(), SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT)
        then:
        Objects.nonNull(jsonSchemaObject)
        jsonSchemaObject.getSchema().get() == "https://json-schema.org/draft/2019-09/schema"
        jsonSchemaObject.getTitle().get() == "foo"
        jsonSchemaObject.getDescription().get() == "bar"
        jsonSchemaObject.getRequired() == ["type", "geometry", "properties"]
        jsonSchemaObject.getProperties().get("id").getTitle().get() == "foo"
        jsonSchemaObject.getProperties().get("id").getDescription().get() == "bar"
        JsonSchemaRef geometry = jsonSchemaObject.getProperties().get("geometry") as JsonSchemaRef
        geometry.getRef() == "https://geojson.org/schema/MultiPolygon.json"
        JsonSchemaObject properties = jsonSchemaObject.getProperties().get("properties") as JsonSchemaObject
        properties.getRequired() == ["string"]
        properties.getProperties().get("boolean").getTitle().get() == "foo"
        properties.getProperties().get("boolean").getDescription().get() == "bar"
        properties.getProperties().get("datetime").getTitle().get() == "foo"
        properties.getProperties().get("datetime").getDescription().get() == "bar"
        (properties.getProperties().get("datetime") as JsonSchemaString).getFormat().get() == "date-time,date"
        properties.getProperties().get("endLifespanVersion").getTitle().get() == "foo"
        properties.getProperties().get("endLifespanVersion").getDescription().get() == "bar"
        properties.getProperties().get("link.href").getTitle().get() == "link > foo"
        properties.getProperties().get("link.href").getDescription().get() == "bar"
        properties.getProperties().get("link.title").getTitle().get() == "link > foo"
        properties.getProperties().get("link.title").getDescription().get() == "bar"
        properties.getProperties().get("datetime").getDescription().get() == "bar"
        properties.getProperties().get("percent").getTitle().get() == "foo"
        properties.getProperties().get("percent").getDescription().get() == "bar"
        properties.getProperties().get("string").getTitle().get() == "foo"
        properties.getProperties().get("string").getDescription().get() == "bar"
        properties.getPatternProperties().get("^links\\.\\d+.href\$").getTitle().get() == "foo > foo"
        properties.getPatternProperties().get("^links\\.\\d+.href\$").getDescription().get() == "bar"
        properties.getPatternProperties().get("^links\\.\\d+.title\$").getTitle().get() == "foo > foo"
        properties.getPatternProperties().get("^links\\.\\d+.title\$").getDescription().get() == "bar"
        properties.getPatternProperties().get("^objects\\.\\d+.date\$").getTitle().get() == "foo > date"
        (properties.getPatternProperties().get("^objects\\.\\d+.date\$") as JsonSchemaString).getFormat().get() == "date-time,date"
        properties.getPatternProperties().get("^objects\\.\\d+.integer\$").getTitle().get() == "foo > foo"
        properties.getPatternProperties().get("^objects\\.\\d+.integer\$").getDescription().get() == "bar"
        properties.getPatternProperties().get("^objects\\.\\d+.object2.codelist\$").getTitle().get() == "foo > object2 > codelist"
        properties.getPatternProperties().get("^objects\\.\\d+.object2.enum\$").getTitle().get() == "foo > object2 > enum"
        (properties.getPatternProperties().get("^objects\\.\\d+.object2.enum\$") as JsonSchemaString).getEnums() == ["foo", "bar"]
        properties.getPatternProperties().get("^objects\\.\\d+.object2.regex\$").getTitle().get() == "foo > object2 > regex"
        (properties.getPatternProperties().get("^objects\\.\\d+.object2.regex\$") as JsonSchemaString).getPattern().get() == "'^_\\\\w+\$'"
        properties.getPatternProperties().get("^strings\\.\\d+\$").getTitle().get() == "foo"
        properties.getPatternProperties().get("^strings\\.\\d+\$").getDescription().get() == "bar"

    }

    FeatureSchema getSchemaObjectReturnables() {
        return new ImmutableFeatureSchema.Builder()
                .name("test-name")
                .description("bar")
                .putPropertyMap("ID", new ImmutableFeatureSchema.Builder()
                        .name("id")
                        .type(SchemaBase.Type.INTEGER)
                        .role(SchemaBase.Role.ID)
                        .label("foo")
                        .description("bar")
                        .build())
                .putPropertyMap("string", new ImmutableFeatureSchema.Builder()
                        .name("string")
                        .type(SchemaBase.Type.STRING)
                        .label("foo")
                        .description("bar")
                        .constraints(new ImmutableSchemaConstraints.Builder().required(true).build())
                        .build())
                .putPropertyMap("link", new ImmutableFeatureSchema.Builder()
                        .name("link")
                        .putPropertyMap("title", new ImmutableFeatureSchema.Builder()
                                .name("title")
                                .description("bar")
                                .label("foo")
                                .type(SchemaBase.Type.STRING)
                                .build())
                        .putPropertyMap("href", new ImmutableFeatureSchema.Builder()
                                .name("href")
                                .description("bar")
                                .label("foo")
                                .type(SchemaBase.Type.STRING)
                                .build())
                        .type(SchemaBase.Type.OBJECT)
                        .build())
                .putPropertyMap("links", new ImmutableFeatureSchema.Builder()
                        .name("links")
                        .type(SchemaBase.Type.OBJECT_ARRAY)
                        .objectType("Link")
                        .label("foo")
                        .description("bar")
                        .constraints(new ImmutableSchemaConstraints.Builder().minOccurrence(1).maxOccurrence(5).build())
                        .putPropertyMap("title", new ImmutableFeatureSchema.Builder()
                                .name("title")
                                .description("bar")
                                .label("foo")
                                .type(SchemaBase.Type.STRING)
                                .build())
                        .putPropertyMap("href", new ImmutableFeatureSchema.Builder()
                                .name("href")
                                .description("bar")
                                .label("foo")
                                .type(SchemaBase.Type.STRING)
                                .build()))
                .putPropertyMap("geometry", new ImmutableFeatureSchema.Builder()
                        .name("geometry")
                        .type(SchemaBase.Type.GEOMETRY)
                        .geometryType(SimpleFeatureGeometry.MULTI_POLYGON)
                        .label("foo")
                        .description("bar")
                        .build())
                .putPropertyMap("datetime", new ImmutableFeatureSchema.Builder()
                        .name("datetime")
                        .type(SchemaBase.Type.DATETIME)
                        .label("foo")
                        .description("bar")
                        .build())
                .putPropertyMap("endLifespanVersion", new ImmutableFeatureSchema.Builder()
                        .name("endLifespanVersion")
                        .type(SchemaBase.Type.DATETIME)
                        .label("foo")
                        .description("bar")
                        .build())
                .putPropertyMap("boolean", new ImmutableFeatureSchema.Builder()
                        .name("boolean")
                        .type(SchemaBase.Type.BOOLEAN)
                        .label("foo")
                        .description("bar")
                        .build())
                .putPropertyMap("percent", new ImmutableFeatureSchema.Builder()
                        .name("percent")
                        .type(SchemaBase.Type.FLOAT)
                        .label("foo")
                        .description("bar")
                        .constraints(new ImmutableSchemaConstraints.Builder().minOccurrence(0).maxOccurrence(100).build())
                        .build())
                .putPropertyMap("strings", new ImmutableFeatureSchema.Builder()
                        .name("strings")
                        .type(SchemaBase.Type.VALUE_ARRAY)
                        .valueType(SchemaBase.Type.STRING)
                        .label("foo")
                        .description("bar")
                        .build())
                .putPropertyMap("objects", new ImmutableFeatureSchema.Builder()
                        .name("objects")
                        .type(SchemaBase.Type.OBJECT_ARRAY)
                        .objectType("Object1")
                        .label("foo")
                        .description("bar")
                        .putPropertyMap("integer", new ImmutableFeatureSchema.Builder()
                                .name("integer")
                                .type(SchemaBase.Type.INTEGER)
                                .label("foo")
                                .description("bar")
                                .build())
                        .putPropertyMap("date", new ImmutableFeatureSchema.Builder()
                                .name("date")
                                .type(SchemaBase.Type.DATETIME)
                                .build())
                        .putPropertyMap("object2", new ImmutableFeatureSchema.Builder()
                                .name("object2")
                                .type(SchemaBase.Type.OBJECT)
                                .objectType("Object2")
                                .putPropertyMap("regex", new ImmutableFeatureSchema.Builder()
                                        .name("regex")
                                        .type(SchemaBase.Type.STRING)
                                        .constraints(new ImmutableSchemaConstraints.Builder().regex("'^_\\\\w+\$'").build())
                                        .build())
                                .putPropertyMap("codelist", new ImmutableFeatureSchema.Builder()
                                        .name("codelist")
                                        .type(SchemaBase.Type.STRING)
                                        .constraints(new ImmutableSchemaConstraints.Builder().codelist("mycodelist").build())
                                        .build())
                                .putPropertyMap("enum", new ImmutableFeatureSchema.Builder()
                                        .name("enum")
                                        .type(SchemaBase.Type.STRING)
                                        .constraints(new ImmutableSchemaConstraints.Builder().enumValues(Arrays.asList("foo", "bar")).build())
                                        .build())
                                .putPropertyMap("strings", new ImmutableFeatureSchema.Builder()
                                        .name("strings")
                                        .type(SchemaBase.Type.VALUE_ARRAY)
                                        .valueType(SchemaBase.Type.STRING)
                                        .build())
                                .build())
                        .build())
                .type(SchemaBase.Type.OBJECT)
                .build()
    }
}
