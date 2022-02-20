/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints
import de.ii.xtraplatform.features.domain.SchemaBase
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry
import spock.lang.Specification

class SchemaDeriverJsonSpec extends Specification {

    def schemaFlattener = new WithTransformationsApplied(ImmutableMap.of("*", new ImmutablePropertyTransformation.Builder().flatten("_").build()))

    def 'Queryables schema derivation, JSON Schema draft #version'() {

        given:
        List<String> queryables = ["geometry", "datetime", "objects.date"]
        SchemaDeriverJsonSchema schemaDeriver = new SchemaDeriverQueryables(version, Optional.empty(), "test-label", Optional.empty(), ImmutableList.of(), queryables)
        FeatureSchema flatSchema = FEATURE_SCHEMA.accept(schemaFlattener)

        when:
        JsonSchemaDocument document = flatSchema.accept(schemaDeriver) as JsonSchemaDocument

        then:
        document == expected

        where:
        version         || expected
        JsonSchemaDocument.VERSION.V201909 || EXPECTED_QUERYABLES
        JsonSchemaDocument.VERSION.V7      || EXPECTED_QUERYABLES_V7
    }

    def 'Returnables schema derivation, JSON Schema draft #version'() {
        given:
        def schemaDeriver = new SchemaDeriverReturnables(version, Optional.empty(), "foo", Optional.empty(), ImmutableList.of())

        when:
        JsonSchemaDocument document = FEATURE_SCHEMA.accept(schemaDeriver) as JsonSchemaDocument

        then:
        document == expected

        where:
        version         || expected
        JsonSchemaDocument.VERSION.V201909 || EXPECTED_RETURNABLES
        JsonSchemaDocument.VERSION.V7      || EXPECTED_RETURNABLES_V7
    }


    def 'Returnables flattened schema derivation, JSON Schema #version'() {
        given:
        def schemaDeriver = new SchemaDeriverReturnables(version, Optional.empty(), "foo", Optional.empty(), ImmutableList.of())
        FeatureSchema flatSchema = FEATURE_SCHEMA.accept(schemaFlattener)

        when:
        JsonSchemaDocument document = flatSchema.accept(schemaDeriver) as JsonSchemaDocument

        then:
        document == expected

        where:
        version         || expected
        JsonSchemaDocument.VERSION.V201909 || EXPECTED_RETURNABLES_FLAT
        JsonSchemaDocument.VERSION.V7      || EXPECTED_RETURNABLES_FLAT_V7
    }

    static final FeatureSchema FEATURE_SCHEMA =
            new ImmutableFeatureSchema.Builder()
                    .name("test-name")
                    .type(SchemaBase.Type.OBJECT)
                    .description("bar")
                    .addTransformations(new ImmutablePropertyTransformation.Builder().flatten(".").build())
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
                            .type(SchemaBase.Type.OBJECT)
                            .objectType("Link")
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
                            .role(SchemaBase.Role.PRIMARY_GEOMETRY) //TODO:
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
                                    .type(SchemaBase.Type.DATE)
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
                    .build()

    static JsonSchema EXPECTED_QUERYABLES =
            ImmutableJsonSchemaDocument.builder()
                    .title("test-label")
                    .description("bar")
                    .putProperties("geometry", GeoJsonSchema.MULTI_POLYGON)
                    .putProperties("datetime", ImmutableJsonSchemaString.builder()
                            .format("date-time")
                            .title("foo")
                            .description("bar")
                            .build())
                    .putProperties("objects.date", ImmutableJsonSchemaString.builder()
                            .title("foo > date")
                            .format("date")
                            .build())
                    .build();

    static JsonSchema EXPECTED_QUERYABLES_V7 =
            ImmutableJsonSchemaDocumentV7.builder()
                    .from(EXPECTED_QUERYABLES)
                    .build()

    static JsonSchema EXPECTED_RETURNABLES =
            ImmutableJsonSchemaDocument.builder()
                    .title("foo")
                    .description("bar")
                    .putProperties("type", ImmutableJsonSchemaString.builder()
                            .addEnums("Feature")
                            .build())
                    .putProperties("links", ImmutableJsonSchemaArray.builder()
                            .items(ImmutableJsonSchemaRef.builder()
                                    .objectType("Link")
                                    .build())
                            .build())
                    .putProperties("id", ImmutableJsonSchemaInteger.builder()
                            .title("foo")
                            .description("bar")
                            .build())
                    .putProperties("geometry", GeoJsonSchema.nullable(GeoJsonSchema.MULTI_POLYGON))
                    .putProperties("properties", ImmutableJsonSchemaObject.builder()
                            .addRequired("string")
                            .putProperties("string", ImmutableJsonSchemaString.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("link", ImmutableJsonSchemaRef.builder()
                                    .objectType("Link")
                                    .build())
                            .putProperties("links", ImmutableJsonSchemaArray.builder()
                                    .items(ImmutableJsonSchemaRef.builder()
                                            .objectType("Link")
                                            .build())
                                    .minItems(1)
                                    .maxItems(5)
                                    .build())
                            .putProperties("datetime", ImmutableJsonSchemaString.builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("endLifespanVersion", ImmutableJsonSchemaString.builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("boolean", ImmutableJsonSchemaBoolean.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("percent", ImmutableJsonSchemaNumber.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("strings", ImmutableJsonSchemaArray.builder()
                                    .items(ImmutableJsonSchemaString.builder()
                                            .title("foo")
                                            .description("bar")
                                            .build())
                                    .build())
                            .putProperties("objects", ImmutableJsonSchemaArray.builder()
                                    .items(ImmutableJsonSchemaRef.builder()
                                            .objectType(SchemaDeriverJsonSchema.getObjectType(getProperty(FEATURE_SCHEMA, "objects").get()))
                                            .build())
                                    .build())
                            .build())
                    .putDefinitions("Link", GeoJsonSchema.LINK_JSON)
                    .putDefinitions(SchemaDeriverJsonSchema.getObjectType(getProperty(FEATURE_SCHEMA, "objects").get()), ImmutableJsonSchemaObject.builder()
                            .title("foo")
                            .description("bar")
                            .putProperties("integer", ImmutableJsonSchemaInteger.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("date", ImmutableJsonSchemaString.builder()
                                    .format("date")
                                    .build())
                            .putProperties("object2", ImmutableJsonSchemaRef.builder()
                                    .objectType("Object2")
                                    .build())
                            .build())
                    .putDefinitions("Object2", ImmutableJsonSchemaObject.builder()
                            .putProperties("regex", ImmutableJsonSchemaString.builder()
                                    .pattern("'^_\\\\w+\$'")
                                    .build())
                            .putProperties("codelist", ImmutableJsonSchemaString.builder()
                                    .build())
                            .putProperties("enum", ImmutableJsonSchemaString.builder()
                                    .addEnums("foo", "bar")
                                    .build())
                            .putProperties("strings", ImmutableJsonSchemaArray.builder()
                                    .items(ImmutableJsonSchemaString.builder()
                                            .build())
                                    .build())
                            .build())
                    .addRequired("type", "geometry", "properties")
                    .build();

    static JsonSchema EXPECTED_RETURNABLES_V7 =
            ImmutableJsonSchemaDocumentV7.builder()
                    .from(EXPECTED_RETURNABLES)
                    .build()

    static JsonSchema EXPECTED_RETURNABLES_FLAT =
            ImmutableJsonSchemaDocument.builder()
                    .title("foo")
                    .description("bar")
                    .putProperties("type", ImmutableJsonSchemaString.builder()
                            .addEnums("Feature")
                            .build())
                    .putProperties("links", ImmutableJsonSchemaArray.builder()
                            .items(ImmutableJsonSchemaRef.builder()
                                    .objectType("Link")
                                    .build())
                            .build())
                    .putProperties("id", ImmutableJsonSchemaInteger.builder()
                            .title("foo")
                            .description("bar")
                            .build())
                    .putProperties("geometry", GeoJsonSchema.nullable(GeoJsonSchema.MULTI_POLYGON))
                    .putProperties("properties", ImmutableJsonSchemaObject.builder()
                            .addRequired("string")
                            .putProperties("string", ImmutableJsonSchemaString.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("link.title", ImmutableJsonSchemaString.builder()
                                    .title("link > foo")
                                    .description("bar")
                                    .build())
                            .putProperties("link.href", ImmutableJsonSchemaString.builder()
                                    .title("link > foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^links\\.\\d+.title\$", ImmutableJsonSchemaString.builder()
                                    .title("foo > foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^links\\.\\d+.href\$", ImmutableJsonSchemaString.builder()
                                    .title("foo > foo")
                                    .description("bar")
                                    .build())
                            .putProperties("datetime", ImmutableJsonSchemaString.builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("endLifespanVersion", ImmutableJsonSchemaString.builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("boolean", ImmutableJsonSchemaBoolean.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("percent", ImmutableJsonSchemaNumber.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^strings\\.\\d+\$", ImmutableJsonSchemaString.builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.integer\$", ImmutableJsonSchemaInteger.builder()
                                    .title("foo > foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.date\$", ImmutableJsonSchemaString.builder()
                                    .title("foo > date")
                                    .format("date")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.regex\$", ImmutableJsonSchemaString.builder()
                                    .title("foo > object2 > regex")
                                    .pattern("'^_\\\\w+\$'")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.codelist\$", ImmutableJsonSchemaString.builder()
                                    .title("foo > object2 > codelist")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.enum\$", ImmutableJsonSchemaString.builder()
                                    .title("foo > object2 > enum")
                                    .addEnums("foo", "bar")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.strings\\.\\d+\$", ImmutableJsonSchemaString.builder()
                                    .title("foo > object2 > strings")
                                    .build())
                            .build())
                    .addRequired("type", "geometry", "properties")
                    .build();

    static JsonSchema EXPECTED_RETURNABLES_FLAT_V7 =
            ImmutableJsonSchemaDocumentV7.builder()
                    .from(EXPECTED_RETURNABLES_FLAT)
                    .build()

    //TODO: move to SchemaBase
    static Optional<FeatureSchema> getProperty(FeatureSchema schema, String name) {
        return schema.getProperties().stream().filter(property -> Objects.equals(property.getName(), name)).findFirst();
    }
}
