/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app

import de.ii.ogcapi.foundation.domain.ExtensionRegistry
import de.ii.ogcapi.foundation.infra.json.ClassSchemaCacheImpl
import de.ii.ogcapi.styles.domain.MbStyleStylesheet
import io.swagger.v3.oas.models.media.Schema
import spock.lang.Specification

class StyleFormatMbStyleSchemaSpec extends Specification {

    def 'a layer property value may be any JSON value'() {
        given:
        ClassSchemaCacheImpl classSchemaCache = new ClassSchemaCacheImpl()
        new StyleFormatMbStyle(Stub(ExtensionRegistry), classSchemaCache)

        when:
        Schema<?> expression = classSchemaCache.getReferencedSchemas(MbStyleStylesheet).get("MbStyleExpression")

        then:
        expression != null
        expression.getType() == null
        expression.getProperties() == null
        expression.getNullable()
    }
}
