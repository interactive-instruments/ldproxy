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

import java.io.IOException;
import java.io.Writer;

/**
 * @author zahnen
 */
public class WktCoordinatesFormatter implements CoordinateFormatter {
    private Writer out;
    private boolean lastWasValue;

    public WktCoordinatesFormatter(Writer out) {
        this.out = out;
    }

    @Override
    public void open() throws IOException {
        out.append('(');
        lastWasValue = false;
    }

    @Override
    public void close() throws IOException {
        out.append(')');
    }

    protected void valueSeparator() throws IOException {
        out.append(' ');
    }

    @Override
    public void separator() throws IOException {
        out.append(',');
        lastWasValue = false;
    }

    @Override
    public void value(String value) throws IOException {
        if (lastWasValue) {
            valueSeparator();
        }
        out.write(value);
        lastWasValue = true;
    }

    @Override
    public void value(char[] chars, int i, int j) throws IOException {
        if (lastWasValue) {
            valueSeparator();
        }
        out.write(chars, i, j);
        lastWasValue = true;
    }

    @Override
    public void raw(char[] chars, int i, int j) throws IOException {
        out.write(chars, i, j);
    }
}
