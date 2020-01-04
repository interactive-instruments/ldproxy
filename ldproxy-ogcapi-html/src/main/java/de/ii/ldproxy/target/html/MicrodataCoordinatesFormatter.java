/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.xtraplatform.crs.api.CoordinateFormatter;

import java.io.IOException;
import java.io.Writer;

/**
 * @author zahnen
 */
public class MicrodataCoordinatesFormatter implements CoordinateFormatter {
    private Writer out;
    private boolean lastWasValue;
    private String lastValue;
    private int count = 0;

    public MicrodataCoordinatesFormatter(Writer out) {
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
        if (count % 2 == 0) {
            lastValue = value;
        } else {
            out.write(value);
            valueSeparator();
            out.write(lastValue);
            lastWasValue = true;
        }
        count++;

    }

    @Override
    public void value(char[] chars, int i, int j) throws IOException {
        value(new String(chars,i,j));
    }

    @Override
    public void raw(char[] chars, int i, int j) throws IOException {
        out.write(chars, i, j);
    }
}
