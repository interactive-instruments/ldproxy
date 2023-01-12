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
import de.ii.ogcapi.features.core.domain.JsonSchema
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument
import de.ii.ogcapi.features.core.domain.SchemaDeriverJsonSchema
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied
import spock.lang.Specification

class SchemaDeriverJsonSpec extends Specification {

    def schemaFlattener = new WithTransformationsApplied(ImmutableMap.of("*", new ImmutablePropertyTransformation.Builder().flatten("_").build()))

    def 'Queryables schema derivation, JSON Schema draft #version'() {

        given:
        List<String> queryables = ["geometry", "datetime", "objects.date"]
        SchemaDeriverJsonSchema schemaDeriver = new SchemaDeriverCollectionProperties(version, Optional.empty(), "test-label", Optional.empty(), ImmutableList.of(), queryables)
        FeatureSchema flatSchema = SchemaDeriverFixtures.FEATURE_SCHEMA.accept(schemaFlattener)

        when:
        JsonSchemaDocument document = flatSchema.accept(schemaDeriver) as JsonSchemaDocument

        then:
        document == expected

        where:
        version         || expected
        JsonSchemaDocument.VERSION.V201909 || EXPECTED_QUERYABLES
        JsonSchemaDocument.VERSION.V7      || EXPECTED_QUERYABLES_V7
    }

    static JsonSchema EXPECTED_QUERYABLES =
            ImmutableJsonSchemaDocument.builder()
                    .title("test-label")
                    .description("bar")
                    .putProperties("geometry", new ImmutableJsonSchemaRefExternal.Builder()
                            .ref("https://geojson.org/schema/MultiPolygon.json")
                            .build())
                    .putProperties("datetime", new ImmutableJsonSchemaString.Builder()
                            .format("date-time")
                            .title("foo")
                            .description("bar")
                            .build())
                    .putProperties("objects.date", new ImmutableJsonSchemaString.Builder()
                            .title("foo > date")
                            .format("date")
                            .build())
                    .additionalProperties(new ImmutableJsonSchemaFalse.Builder().build())
                    .build();

    static JsonSchema EXPECTED_QUERYABLES_V7 =
            ImmutableJsonSchemaDocumentV7.builder()
                    .from(EXPECTED_QUERYABLES)
                    .build()

    //TODO: move to SchemaBase
    static Optional<FeatureSchema> getProperty(FeatureSchema schema, String name) {
        return schema.getProperties().stream().filter(property -> Objects.equals(property.getName(), name)).findFirst();
    }
}
