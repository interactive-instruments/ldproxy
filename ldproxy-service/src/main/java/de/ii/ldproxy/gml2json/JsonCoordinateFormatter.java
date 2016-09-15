/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
