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
