/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.gml;


import de.ii.xtraplatform.crs.api.CoordinateFormatter;

import java.io.IOException;
import java.io.Writer;

/**
 * @author zahnen
 */
public class CoordinatesFormatterGml implements CoordinateFormatter {
    private Writer out;
    private boolean lastWasValue;

    public CoordinatesFormatterGml(Writer out) {
        this.out = out;
    }

    @Override
    public void open() throws IOException {
        lastWasValue = false;
    }

    @Override
    public void close() throws IOException {
    }

    protected void valueSeparator() throws IOException {
        out.append(' ');
    }

    @Override
    public void separator() throws IOException {
        out.append(' ');
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
