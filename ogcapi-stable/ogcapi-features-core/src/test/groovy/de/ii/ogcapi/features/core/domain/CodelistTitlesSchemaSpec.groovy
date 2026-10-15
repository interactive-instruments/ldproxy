/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain

import de.ii.xtraplatform.codelists.domain.Codelist
import de.ii.xtraplatform.codelists.domain.ImmutableCodelist
import de.ii.xtraplatform.features.domain.FeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints
import de.ii.xtraplatform.features.domain.SchemaBase
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Specification

class CodelistTitlesSchemaSpec extends Specification {

    static final Map<String, Codelist> CODELISTS = [
            letters: new ImmutableCodelist.Builder().label("Letters").putEntries("A", "Alpha").putEntries("B", "Bravo").build(),
            numbers: new ImmutableCodelist.Builder().label("Numbers").putEntries("1", "One").putEntries("2", "Two").build()]

    static FeatureSchema feature() {
        new ImmutableFeatureSchema.Builder()
                .name("test")
                .type(SchemaBase.Type.OBJECT)
                .sourcePath("/test")
                .putPropertyMap("letter", new ImmutableFeatureSchema.Builder()
                        .name("letter").type(SchemaBase.Type.STRING).sourcePath("letter")
                        .constraints(new ImmutableSchemaConstraints.Builder().codelist("letters").build()).build())
                .putPropertyMap("number", new ImmutableFeatureSchema.Builder()
                        .name("number").type(SchemaBase.Type.INTEGER).sourcePath("number")
                        .constraints(new ImmutableSchemaConstraints.Builder().codelist("numbers").build()).build())
                .build()
    }

    def 'a response may contain the code or the title of a codelist value'() {
        when:
        Schema<?> schema = feature().accept(new SchemaDeriverOpenApiFeatures("test", Optional.empty(), CODELISTS))
        Map<String, Schema> properties = schema.getProperties().get("properties").getProperties()

        then:
        properties.get("letter").getEnum() == ["A", "B", "Alpha", "Bravo"]
        properties.get("letter").getType() == "string"
        properties.get("number").getEnum() == [1, 2, "One", "Two"]
        properties.get("number").getType() == null
    }

    def 'a query parameter only accepts the codes'() {
        when:
        Schema<?> schema = feature().accept(new SchemaDeriverOpenApiCollectionProperties("test", Optional.empty(), CODELISTS, ["letter", "number"]))

        then:
        schema.getProperties().get("letter").getEnum() == ["A", "B"]
        schema.getProperties().get("number").getEnum() == [1, 2]
        schema.getProperties().get("number").getType() == "integer"
    }
}
