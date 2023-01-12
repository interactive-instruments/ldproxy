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
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaArray
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaBoolean
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaDocument
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaDocumentV7
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaInteger
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaNumber
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaObject
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaRef
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaString
import de.ii.ogcapi.features.core.domain.JsonSchema
import de.ii.ogcapi.features.core.domain.JsonSchemaBuildingBlocks
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument
import de.ii.ogcapi.features.core.domain.SchemaDeriverJsonSchema
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

    static JsonSchema EXPECTED_RETURNABLES =
            ImmutableJsonSchemaDocument.builder()
                    .title("foo")
                    .description("bar")
                    .putProperties("type", new ImmutableJsonSchemaString.Builder()
                            .addEnums("Feature")
                            .build())
                    .putProperties("links", new ImmutableJsonSchemaArray.Builder()
                            .items(new ImmutableJsonSchemaRef.Builder()
                                    .objectType("Link")
                                    .build())
                            .build())
                    .putProperties("id", new ImmutableJsonSchemaInteger.Builder()
                            .title("foo")
                            .description("bar")
                            .build())
                    .putProperties("geometry", JsonSchemaBuildingBlocks.nullable(JsonSchemaBuildingBlocks.MULTI_POLYGON))
                    .putProperties("properties", new ImmutableJsonSchemaObject.Builder()
                            .addRequired("string")
                            .putProperties("string", new ImmutableJsonSchemaString.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("link", new ImmutableJsonSchemaRef.Builder()
                                    .objectType("Link")
                                    .build())
                            .putProperties("links", new ImmutableJsonSchemaArray.Builder()
                                    .items(new ImmutableJsonSchemaRef.Builder()
                                            .objectType("Link")
                                            .build())
                                    .minItems(1)
                                    .maxItems(5)
                                    .build())
                            .putProperties("datetime", new ImmutableJsonSchemaString.Builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("endLifespanVersion", new ImmutableJsonSchemaString.Builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("boolean", new ImmutableJsonSchemaBoolean.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("percent", new ImmutableJsonSchemaNumber.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("strings", new ImmutableJsonSchemaArray.Builder()
                                    .items(new ImmutableJsonSchemaString.Builder()
                                            .title("foo")
                                            .description("bar")
                                            .build())
                                    .build())
                            .putProperties("objects", new ImmutableJsonSchemaArray.Builder()
                                    .items(new ImmutableJsonSchemaRef.Builder()
                                            .objectType(SchemaDeriverJsonSchema.getObjectType(getProperty(FEATURE_SCHEMA, "objects").get()))
                                            .build())
                                    .build())
                            .build())
                    .putDefinitions("Link", JsonSchemaBuildingBlocks.LINK_JSON)
                    .putDefinitions(SchemaDeriverJsonSchema.getObjectType(getProperty(FEATURE_SCHEMA, "objects").get()), new ImmutableJsonSchemaObject.Builder()
                            .title("foo")
                            .description("bar")
                            .putProperties("integer", new ImmutableJsonSchemaInteger.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("date", new ImmutableJsonSchemaString.Builder()
                                    .format("date")
                                    .build())
                            .putProperties("object2", new ImmutableJsonSchemaRef.Builder()
                                    .objectType("Object2")
                                    .build())
                            .build())
                    .putDefinitions("Object2", new ImmutableJsonSchemaObject.Builder()
                            .putProperties("regex", new ImmutableJsonSchemaString.Builder()
                                    .pattern("'^_\\\\w+\$'")
                                    .build())
                            .putProperties("codelist", new ImmutableJsonSchemaString.Builder()
                                    .build())
                            .putProperties("enum", new ImmutableJsonSchemaString.Builder()
                                    .addEnums("foo", "bar")
                                    .build())
                            .putProperties("strings", new ImmutableJsonSchemaArray.Builder()
                                    .items(new ImmutableJsonSchemaString.Builder()
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
                    .putProperties("type", new ImmutableJsonSchemaString.Builder()
                            .addEnums("Feature")
                            .build())
                    .putProperties("links", new ImmutableJsonSchemaArray.Builder()
                            .items(new ImmutableJsonSchemaRef.Builder()
                                    .objectType("Link")
                                    .build())
                            .build())
                    .putProperties("id", new ImmutableJsonSchemaInteger.Builder()
                            .title("foo")
                            .description("bar")
                            .build())
                    .putProperties("geometry", JsonSchemaBuildingBlocks.nullable(JsonSchemaBuildingBlocks.MULTI_POLYGON))
                    .putProperties("properties", new ImmutableJsonSchemaObject.Builder()
                            .addRequired("string")
                            .putProperties("string", new ImmutableJsonSchemaString.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("link.title", new ImmutableJsonSchemaString.Builder()
                                    .title("link > foo")
                                    .description("bar")
                                    .build())
                            .putProperties("link.href", new ImmutableJsonSchemaString.Builder()
                                    .title("link > foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^links\\.\\d+.title\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo > foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^links\\.\\d+.href\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo > foo")
                                    .description("bar")
                                    .build())
                            .putProperties("datetime", new ImmutableJsonSchemaString.Builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("endLifespanVersion", new ImmutableJsonSchemaString.Builder()
                                    .format("date-time")
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("boolean", new ImmutableJsonSchemaBoolean.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putProperties("percent", new ImmutableJsonSchemaNumber.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^strings\\.\\d+\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.integer\$", new ImmutableJsonSchemaInteger.Builder()
                                    .title("foo > foo")
                                    .description("bar")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.date\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo > date")
                                    .format("date")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.regex\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo > object2 > regex")
                                    .pattern("'^_\\\\w+\$'")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.codelist\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo > object2 > codelist")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.enum\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo > object2 > enum")
                                    .addEnums("foo", "bar")
                                    .build())
                            .putPatternProperties("^objects\\.\\d+.object2.strings\\.\\d+\$", new ImmutableJsonSchemaString.Builder()
                                    .title("foo > object2 > strings")
                                    .build())
                            .build())
                    .putDefinitions("Link", JsonSchemaBuildingBlocks.LINK_JSON)
                    .addRequired("type", "geometry", "properties")
                    .build();

    static JsonSchema EXPECTED_RETURNABLES_FLAT_V7 =
            new ImmutableJsonSchemaDocumentV7.Builder()
                    .from(EXPECTED_RETURNABLES_FLAT)
                    .build()

    //TODO: move to SchemaBase
    static Optional<FeatureSchema> getProperty(FeatureSchema schema, String name) {
        return schema.getProperties().stream().filter(property -> Objects.equals(property.getName(), name)).findFirst();
    }
}
