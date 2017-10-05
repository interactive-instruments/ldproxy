/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.gml2json;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.LazyStringCoordinateTuple;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;

/**
 *
 * @author zahnen
 */
public class TransformingCoordinatesWriter extends DefaultCoordinatesWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(TransformingCoordinatesWriter.class);
    private LazyStringCoordinateTuple coordinateBuffer;
    private CrsTransformer transformer;

    public TransformingCoordinatesWriter(CoordinateFormatter formatter, int srsDimension, CrsTransformer transformer) {
        super(formatter, srsDimension);
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
            CoordinateTuple c = transformer.transform(coordinateBuffer);

            formatter.value(c.getXasString());
            formatter.value(c.getYasString());
        } else {
            formatter.value(coordinateBuffer.getXasString());
            formatter.value(coordinateBuffer.getYasString());
        }
    }

    @Override
    protected void formatValue(String val) throws IOException {
        //jsonOut.writeRawValue(buf);
        if (isXValue()) {
            coordinateBuffer.setX(val);
        }
        if (isYValue()) {
            coordinateBuffer.setY(val);
        }
    }

    @Override
    protected void formatValue(char[] chars, int i, int j) throws IOException {
        //jsonOut.writeRawValue(chars, i, j);
        formatValue(String.copyValueOf(chars, i, j));
    }

    @Override
    protected void formatRaw(char[] chars, int i, int j) throws IOException {
        //jsonOut.writeRaw(chars, i, j);
        if (isXValue()) {
            coordinateBuffer.appendX(String.copyValueOf(chars, i, j));
        }
        if (isYValue()) {
            coordinateBuffer.appendY(String.copyValueOf(chars, i, j));
        }
    }
}
