/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.gml2json;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 * @author zahnen
 */
public class JsonCoordinateFormatter implements CoordinateFormatter {
    protected JsonGenerator jsonOut;

    public JsonCoordinateFormatter(JsonGenerator jsonOut) {
        this.jsonOut = jsonOut;
    }

    @Override
    public void open() throws IOException {
        jsonOut.writeStartArray();
    }

    @Override
    public void close() throws IOException {
        jsonOut.writeEndArray();
    }

    @Override
    public void separator() throws IOException {
        jsonOut.writeEndArray();
        jsonOut.writeStartArray();
    }

    @Override
    public void value(String value) throws IOException {
        jsonOut.writeRawValue(value);
    }

    @Override
    public void value(char[] chars, int i, int j) throws IOException {
        jsonOut.writeRawValue(chars, i, j);
    }

    @Override
    public void raw(char[] chars, int i, int j) throws IOException {
        jsonOut.writeRaw(chars, i, j);
    }
}
