/*
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import de.ii.xtraplatform.store.app.entities.EntityRegistryImpl
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Shared
import spock.lang.Specification

class SchemaGeneratorFeatureOpenApiSpec extends Specification {

    @Shared SchemaGeneratorFeatureOpenApi schemaGenerator

    def setupSpec() {
        schemaGenerator = new SchemaGeneratorFeatureOpenApi(null, new EntityRegistryImpl(null), new SchemaInfoImpl())
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

    def 'Test Open API schema generation for the RETURNABLES type'() {
        given:
        FeatureSchema featureSchema = getSchemaObjectReturnables()
        FeatureTypeConfigurationOgcApi collectionData = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                .id("id")
                .label("foo")
                .build()
        when:
        Schema schema = schemaGenerator.getSchemaOpenApi(featureSchema, collectionData, SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES)
        then:
        Objects.nonNull(schema)
        schema.getRequired() == ["type", "geometry", "properties"]
        schema.getProperties().get("id").getType() == "integer"
        ObjectSchema properties = schema.getProperties().get("properties") as ObjectSchema
        properties.getRequired() == ["string"]
        properties.getProperties().get("string").getType() == "string"
        properties.getProperties().get("string").getTitle() == "foo"
        properties.getProperties().get("string").getDescription() == "bar"
        properties.getProperties().get("link").getType() == "object"
        properties.getProperties().get("link").getProperties().get("title").getType() == "string"
        properties.getProperties().get("link").getProperties().get("title").getTitle() == "foo"
        properties.getProperties().get("link").getProperties().get("title").getDescription() == "bar"
        properties.getProperties().get("link").getProperties().get("href").getType() == "string"
        properties.getProperties().get("link").getProperties().get("href").getTitle() == "foo"
        properties.getProperties().get("link").getProperties().get("href").getDescription() == "bar"
        properties.getProperties().get("links").getType() == "array"
        properties.getProperties().get("links").getMinItems() == 1
        properties.getProperties().get("links").getMaxItems() == 5
        (properties.getProperties().get("links") as ArraySchema).getItems().getTitle() == "foo"
        (properties.getProperties().get("links") as ArraySchema).getItems().getDescription() == "bar"
        properties.getProperties().get("datetime").getType() == "string"
        properties.getProperties().get("datetime").getFormat() == "date-time"
        properties.getProperties().get("datetime").getTitle() == "foo"
        properties.getProperties().get("datetime").getDescription() == "bar"
        properties.getProperties().get("endLifespanVersion").getType() == "string"
        properties.getProperties().get("endLifespanVersion").getFormat() == "date-time"
        properties.getProperties().get("endLifespanVersion").getTitle() == "foo"
        properties.getProperties().get("endLifespanVersion").getDescription() == "bar"
        properties.getProperties().get("boolean").getType() == "boolean"
        properties.getProperties().get("boolean").getTitle() == "foo"
        properties.getProperties().get("boolean").getDescription() == "bar"
        properties.getProperties().get("percent").getType() == "number"
        properties.getProperties().get("percent").getTitle() == "foo"
        properties.getProperties().get("percent").getDescription() == "bar"
        properties.getProperties().get("strings").getType() == "array"
        (properties.getProperties().get("strings") as ArraySchema).getItems().getTitle() == "foo"
        (properties.getProperties().get("strings") as ArraySchema).getItems().getDescription() == "bar"
        properties.getProperties().get("objects").getType() == "array"
        ArraySchema objects = properties.getProperties().get("objects") as ArraySchema
        objects.getItems().getTitle() == "foo"
        objects.getItems().getDescription() == "bar"
        objects.getItems().getProperties().get("integer").getType() == "integer"
        objects.getItems().getProperties().get("integer").getTitle() == "foo"
        objects.getItems().getProperties().get("integer").getDescription() == "bar"
        objects.getItems().getProperties().get("date").getType() == "string"
        objects.getItems().getProperties().get("date").getFormat() == "date-time"
        objects.getItems().getProperties().get("object2").getType() == "object"
        objects.getItems().getProperties().get("object2").getProperties().get("regex").getType() == "string"
        objects.getItems().getProperties().get("object2").getProperties().get("regex").getPattern() == "'^_\\\\w+\$'"
        objects.getItems().getProperties().get("object2").getProperties().get("codelist").getType() == "string"
        objects.getItems().getProperties().get("object2").getProperties().get("enum").getType() == "string"
        objects.getItems().getProperties().get("object2").getProperties().get("strings").getType() == "array"
        schema.getProperties().get("type").getType() == "string"
        schema.getProperties().get("links").getType() == "array"
        Objects.nonNull(schema.getProperties().get("geometry"))
    }

    def 'Test Open API schema generation for the RETURNABLES_FLAT type'() {
        FeatureSchema featureSchema = getSchemaObjectReturnables()
        FeatureTypeConfigurationOgcApi collectionData = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                .id("id")
                .label("foo")
                .build()
        when:
        Schema schema = schemaGenerator.getSchemaOpenApi(featureSchema, collectionData, SchemaGeneratorFeature.SCHEMA_TYPE.RETURNABLES_FLAT)
        then:
        Objects.nonNull(schema)
        schema.getRequired() == ["type", "geometry", "properties"]
        schema.getProperties().get("id").getType() == "integer"
        ObjectSchema properties = schema.getProperties().get("properties") as ObjectSchema
        properties.getRequired() == ["string"]
        properties.getProperties().get("string").getType() == "string"
        properties.getProperties().get("string").getTitle() == "foo"
        properties.getProperties().get("string").getDescription() == "bar"
        properties.getProperties().get("link.title").getType() == "string"
        properties.getProperties().get("link.title").getTitle() == "link > foo"
        properties.getProperties().get("link.title").getDescription() == "bar"
        properties.getProperties().get("link.href").getType() == "string"
        properties.getProperties().get("links.1.title").getType() == "string"
        properties.getProperties().get("links.1.title").getTitle() == "foo > foo"
        properties.getProperties().get("links.1.title").getDescription() == "bar"
        properties.getProperties().get("links.1.href").getType() == "string"
        properties.getProperties().get("links.1.href").getTitle() == "foo > foo"
        properties.getProperties().get("links.1.href").getDescription() == "bar"
        properties.getProperties().get("datetime").getType() == "string"
        properties.getProperties().get("datetime").getFormat() == "date-time"
        properties.getProperties().get("datetime").getTitle() == "foo"
        properties.getProperties().get("datetime").getDescription() == "bar"
        properties.getProperties().get("endLifespanVersion").getType() == "string"
        properties.getProperties().get("endLifespanVersion").getFormat() == "date-time"
        properties.getProperties().get("endLifespanVersion").getTitle() == "foo"
        properties.getProperties().get("endLifespanVersion").getDescription() == "bar"
        properties.getProperties().get("boolean").getType() == "boolean"
        properties.getProperties().get("boolean").getTitle() == "foo"
        properties.getProperties().get("boolean").getDescription() == "bar"
        properties.getProperties().get("percent").getType() == "number"
        properties.getProperties().get("percent").getTitle() == "foo"
        properties.getProperties().get("percent").getDescription() == "bar"
        properties.getProperties().get("strings.1").getType() == "string"
        properties.getProperties().get("strings.1").getTitle() == "foo"
        properties.getProperties().get("strings.1").getDescription() == "bar"
        properties.getProperties().get("objects.1.integer").getType() == "integer"
        properties.getProperties().get("objects.1.integer").getTitle() == "foo > foo"
        properties.getProperties().get("objects.1.integer").getDescription() == "bar"
        properties.getProperties().get("objects.1.date").getType() == "string"
        properties.getProperties().get("objects.1.date").getFormat() == "date-time"
        properties.getProperties().get("objects.1.object2.regex").getType() == "string"
        properties.getProperties().get("objects.1.object2.regex").getPattern() == "'^_\\\\w+\$'"
        properties.getProperties().get("objects.1.object2.codelist").getType() == "string"
        properties.getProperties().get("objects.1.object2.enum").getType() == "string"
        properties.getProperties().get("objects.1.object2.strings.1").getType() == "string"
        schema.getProperties().get("type").getType() == "string"
        schema.getProperties().get("links").getType() == "array"
        Objects.nonNull(schema.getProperties().get("geometry"))
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
