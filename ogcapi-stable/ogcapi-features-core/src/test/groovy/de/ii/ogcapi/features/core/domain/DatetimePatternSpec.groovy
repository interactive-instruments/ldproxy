/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain

import spock.lang.Specification

import java.util.regex.Pattern

class DatetimePatternSpec extends Specification {

    def 'the pattern only accepts the seconds that the server can parse'() {
        given:
        Pattern pattern = Pattern.compile(AbstractQueryParameterDatetime.getCopyOfBaseSchema().getPattern())

        expect:
        pattern.matcher(value).find() == valid

        where:
        value                              || valid
        "2020-01-01T10:20:59Z"             || true
        "2020-01-01T10:20:59+07:14"        || true
        "2020-01-01T10:20:60Z"             || false
        "5076-05-24T20:02:60+07:14"        || false
        "2020-01-01T10:20:00Z/.."          || true
    }
}
