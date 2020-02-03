/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.xtraplatform.geometries.domain.CoordinateTuple;
import de.ii.xtraplatform.geometries.domain.CrsTransformer;
import de.ii.xtraplatform.geometries.domain.LazyStringCoordinateTuple;

import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author zahnen
 */
public class HtmlTransformingCoordinatesWriter extends HtmlCoordinatesWriter {

    private LazyStringCoordinateTuple coordinateBuffer;
    private CrsTransformer transformer;

    public HtmlTransformingCoordinatesWriter(Writer output, int srsDimension, CrsTransformer transformer) {
        super(output, srsDimension);
        this.transformer = transformer;
        this.coordinateBuffer = new LazyStringCoordinateTuple();
    }

    @Override
    protected void writeEnd() throws IOException {
        if (isCompleteTuple()) {
            writeCoordinates();
        }
    }

    @Override
    protected void writeSeparator() throws IOException {
        writeCoordinates();

        super.writeSeparator();
    }

    private void writeCoordinates() throws IOException {       
        if (transformer != null) {
            CoordinateTuple c = transformer.transform(coordinateBuffer, false);

            output.write(c.getXasString());
            output.write(",");
            output.write(c.getYasString());
        } else {
            output.write(coordinateBuffer.getXasString());
            output.write(",");
            output.write(coordinateBuffer.getYasString());
        }
    }

    @Override
    protected void writeRawValue(String val) throws IOException {
        //jsonOut.writeRawValue(buf);
        if (isXValue()) {
            coordinateBuffer.setX(val);
        }
        if (isYValue()) {
            coordinateBuffer.setY(val);
        }
    }

    @Override
    protected void writeRawValue(char[] chars, int i, int j) throws IOException {
        //jsonOut.writeRawValue(chars, i, j);
        writeRawValue(String.copyValueOf(chars, i, j));
    }

    @Override
    protected void writeRaw(char[] chars, int i, int j) throws IOException {
        //jsonOut.writeRaw(chars, i, j);
        if (isXValue()) {
            coordinateBuffer.appendX(String.copyValueOf(chars, i, j));
        }
        if (isYValue()) {
            coordinateBuffer.appendY(String.copyValueOf(chars, i, j));
        }
    }
}
