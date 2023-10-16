/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied
import spock.lang.Specification

class SchemaDeriverJsonSpec extends Specification {

    def schemaFlattener = new WithTransformationsApplied(ImmutableMap.of("*", new ImmutablePropertyTransformation.Builder().flatten("_").build()))

    def 'Returnables schema derivation, JSON Schema draft #version'() {
        given:
        def schemaDeriver = new SchemaDeriverFeatures(version, Optional.empty(), "foo", Optional.empty(), ImmutableMap.of())

        when:
        JsonSchemaDocument document = SchemaDeriverFixtures.FEATURE_SCHEMA.accept(schemaDeriver) as JsonSchemaDocument

        then:
        document == expected

        where:
        version         || expected
        JsonSchemaDocument.VERSION.V202012 || EXPECTED_RETURNABLES
        JsonSchemaDocument.VERSION.V201909 || EXPECTED_RETURNABLES_V201909
        JsonSchemaDocument.VERSION.V7      || EXPECTED_RETURNABLES_V7
    }

    def 'Returnables flattened schema derivation, JSON Schema #version'() {
        given:
        def schemaDeriver = new SchemaDeriverFeatures(version, Optional.empty(), "foo", Optional.empty(), ImmutableMap.of())
        FeatureSchema flatSchema = SchemaDeriverFixtures.FEATURE_SCHEMA.accept(schemaFlattener)

        when:
        JsonSchemaDocument document = flatSchema.accept(schemaDeriver) as JsonSchemaDocument

        then:
        document == expected

        where:
        version         || expected
        JsonSchemaDocument.VERSION.V202012 || EXPECTED_RETURNABLES_FLAT
        JsonSchemaDocument.VERSION.V201909 || EXPECTED_RETURNABLES_FLAT_V201909
        JsonSchemaDocument.VERSION.V7      || EXPECTED_RETURNABLES_FLAT_V7
    }

    def 'Queryables schema derivation, JSON Schema draft #version'() {

        given:
        List<String> queryables = ["geometry", "datetime", "objects.date"]
        SchemaDeriverJsonSchema schemaDeriver = new SchemaDeriverCollectionProperties(version, Optional.empty(), "test-label", Optional.empty(), ImmutableMap.of(), queryables)
        FeatureSchema flatSchema = SchemaDeriverFixtures.FEATURE_SCHEMA.accept(schemaFlattener)

        when:
        JsonSchemaDocument document = flatSchema.accept(schemaDeriver) as JsonSchemaDocument

        then:
        document == expected

        where:
        version         || expected
        JsonSchemaDocument.VERSION.V202012 || EXPECTED_QUERYABLES
        JsonSchemaDocument.VERSION.V201909 || EXPECTED_QUERYABLES_V201909
        JsonSchemaDocument.VERSION.V7      || EXPECTED_QUERYABLES_V7
    }

    static JsonSchema EXPECTED_RETURNABLES =
            ImmutableJsonSchemaDocument.builder()
                    .schema(JsonSchemaDocument.VERSION.V202012.url())
                    .title("foo")
                    .description("bar")
                    .putProperties("id", new ImmutableJsonSchemaInteger.Builder()
                            .title("foo")
                            .description("bar")
                            .role("id")
                            .build())
                    .addRequired("string")
                    .putProperties("string", new ImmutableJsonSchemaString.Builder()
                            .title("foo")
                            .description("bar")
                            .build())
                    .putProperties("link", new ImmutableJsonSchemaRef.Builder()
                            .ref("#/\$defs/Link")
                            .build())
                    .putProperties("links", new ImmutableJsonSchemaArray.Builder()
                            .items(new ImmutableJsonSchemaRef.Builder()
                                    .ref("#/\$defs/Link")
                                    .build())
                            .minItems(1)
                            .maxItems(5)
                            .build())
                    .putProperties("geometry", new ImmutableJsonSchemaGeometry.Builder()
                            .from(JsonSchemaBuildingBlocks.MULTI_POLYGON)
                            .role("primary-geometry")
                            .build())
                    .putProperties("datetime", new ImmutableJsonSchemaString.Builder()
                            .format("date-time")
                            .title("foo")
                            .description("bar")
                            .role("primary-instant")
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
                                    .ref("#/\$defs/" + SchemaDeriverJsonSchema.getObjectType(getProperty(SchemaDeriverFixtures.FEATURE_SCHEMA, "objects").get()))
                                    .build())
                            .build())
                    .putDefinitions("Link", JsonSchemaBuildingBlocks.LINK_JSON)
                    .putDefinitions(SchemaDeriverJsonSchema.getObjectType(getProperty(SchemaDeriverFixtures.FEATURE_SCHEMA, "objects").get()), new ImmutableJsonSchemaObject.Builder()
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
                                    .ref("#/\$defs/Object2")
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
                    .build();

    static JsonSchema EXPECTED_RETURNABLES_V201909 =
            ImmutableJsonSchemaDocument.builder()
                    .from(EXPECTED_RETURNABLES)
                    .schema(JsonSchemaDocument.VERSION.V201909.url())
                    .build()

    static JsonSchema EXPECTED_RETURNABLES_V7 =
            ImmutableJsonSchemaDocumentV7.builder()
                    .from(EXPECTED_RETURNABLES)
                    .build()

    static JsonSchema EXPECTED_RETURNABLES_FLAT =
            ImmutableJsonSchemaDocument.builder()
                    .schema(JsonSchemaDocument.VERSION.V202012.url())
                    .title("foo")
                    .description("bar")
                    .putProperties("id", new ImmutableJsonSchemaInteger.Builder()
                            .title("foo")
                            .description("bar")
                            .role("id")
                            .build())
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
                    .putProperties("geometry", new ImmutableJsonSchemaGeometry.Builder()
                            .from(JsonSchemaBuildingBlocks.MULTI_POLYGON)
                            .role("primary-geometry")
                            .build())
                    .putProperties("datetime", new ImmutableJsonSchemaString.Builder()
                            .format("date-time")
                            .title("foo")
                            .description("bar")
                            .role("primary-instant")
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
                    .build();

    static JsonSchema EXPECTED_RETURNABLES_FLAT_V201909 =
            ImmutableJsonSchemaDocument.builder()
                    .from(EXPECTED_RETURNABLES_FLAT)
                    .schema(JsonSchemaDocument.VERSION.V201909.url())
                    .build()

    static JsonSchema EXPECTED_RETURNABLES_FLAT_V7 =
            new ImmutableJsonSchemaDocumentV7.Builder()
                    .from(EXPECTED_RETURNABLES_FLAT)
                    .build()

    static JsonSchema EXPECTED_QUERYABLES =
            ImmutableJsonSchemaDocument.builder()
                    .schema(JsonSchemaDocument.VERSION.V202012.url())
                    .title("test-label")
                    .description("bar")
                    .putProperties("geometry", new ImmutableJsonSchemaGeometry.Builder()
                            .format("geometry-multipolygon")
                            .role("primary-geometry")
                            .build())
                    .putProperties("datetime", new ImmutableJsonSchemaString.Builder()
                            .format("date-time")
                            .title("foo")
                            .description("bar")
                            .role("primary-instant")
                            .build())
                    .putProperties("objects.date", new ImmutableJsonSchemaArray.Builder()
                            .items(new ImmutableJsonSchemaString.Builder()
                                .title("foo > date")
                                .format("date")
                                .build())
                            .build())
                    .additionalProperties(new ImmutableJsonSchemaFalse.Builder().build())
                    .build();

    static JsonSchema EXPECTED_QUERYABLES_V201909 =
            ImmutableJsonSchemaDocument.builder()
                    .from(EXPECTED_QUERYABLES)
                    .schema(JsonSchemaDocument.VERSION.V201909.url())
                    .build()

    static JsonSchema EXPECTED_QUERYABLES_V7 =
            ImmutableJsonSchemaDocumentV7.builder()
                    .from(EXPECTED_QUERYABLES)
                    .build()

    //TODO: move to SchemaBase
    static Optional<FeatureSchema> getProperty(FeatureSchema schema, String name) {
        return schema.getProperties().stream().filter(property -> Objects.equals(property.getName(), name)).findFirst();
    }
}
