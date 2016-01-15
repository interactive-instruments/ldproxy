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
package de.ii.ldproxy.output.html;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.LazyStringCoordinateTuple;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author zahnen
 */
public class HtmlTransformingCoordinatesWriter extends HtmlCoordinatesWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(HtmlTransformingCoordinatesWriter.class);
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
            CoordinateTuple c = transformer.transform(coordinateBuffer);

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
        //json.writeRawValue(buf);
        if (isXValue()) {
            coordinateBuffer.setX(val);
        }
        if (isYValue()) {
            coordinateBuffer.setY(val);
        }
    }

    @Override
    protected void writeRawValue(char[] chars, int i, int j) throws IOException {
        //json.writeRawValue(chars, i, j);
        writeRawValue(String.copyValueOf(chars, i, j));
    }

    @Override
    protected void writeRaw(char[] chars, int i, int j) throws IOException {
        //json.writeRaw(chars, i, j);
        if (isXValue()) {
            coordinateBuffer.appendX(String.copyValueOf(chars, i, j));
        }
        if (isYValue()) {
            coordinateBuffer.appendY(String.copyValueOf(chars, i, j));
        }
    }
}
