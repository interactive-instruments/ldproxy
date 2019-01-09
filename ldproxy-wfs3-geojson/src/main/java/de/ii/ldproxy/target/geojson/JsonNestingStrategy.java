/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 * @author zahnen
 *
 *
 *NEST+ARRAY
 * //nested objects + object arrays + value arrays
 * "biotoptyp" : [ {
 *   "typ" : "136273",
 *   "zusatzbezeichnung" : [ {
 *     "zusatzcode" : [ "137946" ],
 *     "bemerkung" : "110 cm Brusthöhendurchmesser, vital, hoch gewachsen"
 *   }, {
 *     "zusatzcode" : [ "167424", "137946" ]
 *   } ]
 * } ],
 *
 * FLATTEN+SUFFIX
 * //flatten totally
 * "biotoptyp.1.typ" : "136273",
 * "biotoptyp.1.zusatzbezeichnung.1.zusatzcode.1" : "137946",
 * "biotoptyp.1.zusatzbezeichnung.1.bemerkung" : "110 cm Brusthöhendurchmesser, vital, hoch gewachsen",
 * "biotoptyp.1.zusatzbezeichnung.2.zusatzcode.1" : "167424"
 * "biotoptyp.1.zusatzbezeichnung.2.zusatzcode.2" : "137946"
 *
 * FLATTEN+ARRAY
 * //flatten + value arrays
 * "biotoptyp.1.typ" : "136273",
 * "biotoptyp.1.zusatzbezeichnung.1.zusatzcode" : [ "137946" ],
 * "biotoptyp.1.zusatzbezeichnung.1.bemerkung" : "110 cm Brusthöhendurchmesser, vital, hoch gewachsen",
 * "biotoptyp.1.zusatzbezeichnung.2.zusatzcode" : [ "167424", "137946" ]
 *
 * NEST+SUFFIX
 * //nested objects + flatten object arrays + value arrays
 * "biotoptyp.1" : {
 *   "typ" : "136273",
 *   "zusatzbezeichnung.1" : {
 *     "zusatzcode" : [ "137946" ],
 *     "bemerkung" : "110 cm Brusthöhendurchmesser, vital, hoch gewachsen"
 *   },
 *   "zusatzbezeichnung.2" : {
 *     "zusatzcode" : [ "167424", "137946" ]
 *   }
 * },
 *
 *
 */
public interface JsonNestingStrategy {

    void openObjectInArray(JsonGenerator json, String key) throws IOException;

    void openArray(JsonGenerator json) throws IOException;

    void openObject(JsonGenerator json, String key) throws IOException;

    void openArray(JsonGenerator json, String key) throws IOException;

    void closeObject(JsonGenerator json) throws IOException;

    void closeArray(JsonGenerator json) throws IOException;

    default void open(JsonGenerator json) throws IOException {

    }
}
