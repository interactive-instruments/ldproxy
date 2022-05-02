/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain


import de.ii.ogcapi.foundation.infra.json.ClassSchemaCacheImpl
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Shared
import spock.lang.Specification

class ClassSchemaCacheImplSpec extends Specification {

    @Shared ClassSchemaCacheImpl schemaGenerator

    def setupSpec() {
        schemaGenerator = new ClassSchemaCacheImpl()
    }

    def 'Test Link schema'() {
        when:
        Schema schema = schemaGenerator.getSchema(Link.class)
        then:
        Objects.nonNull(schema)
        schema.getType() == "object"
        schema.getRequired()  == ["href"]
        schema.getProperties().get("title").getType() == "string"
        schema.getProperties().get("href").getType() == "string"
        schema.getProperties().get("href").getFormat() == "uri-reference"
        schema.getProperties().get("rel").getType() == "string"
        schema.getProperties().get("type").getType() == "string"
    }

    def 'Test ExternalDocumentation schema'() {
        when:
        Schema schema = schemaGenerator.getSchema(ExternalDocumentation.class)
        then:
        Objects.nonNull(schema)
        schema.getType() == "object"
        schema.getRequired()  == ["url"]
        schema.getProperties().get("url").getType() == "string"
        schema.getProperties().get("description").getType() == "string"
    }

}
