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
import de.ii.xsf.logging.XSFLogger;
import java.io.IOException;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class FastXYSwapCoordinatesWriter extends DefaultCoordinatesWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(FastXYSwapCoordinatesWriter.class);
    private int[] xPos;
    private String xBuffer;

    public FastXYSwapCoordinatesWriter(JsonGenerator json, int srsDimension) {
        super(json, srsDimension);
        this.xPos = new int[2];
    }

    @Override
    protected void writeEnd() throws IOException {
        if (xBuffer != null && !xBuffer.isEmpty()) {
            jsonWriteRawValue(xBuffer);
        }
    }
    
    @Override
    protected void writeChunkEnd(char[] chars, int start, int end, int read) throws IOException {
        if (read > -1 || isXValue()) {
            appendToChunkBoundaryBuffer(chars, start, end);
        }
    }

    @Override
    protected void appendToChunkBoundaryBuffer(char[] chars, int start, int end) {

        if (isXValue()) {
            super.appendToChunkBoundaryBuffer(chars, start, end);
            appendToXBuffer(chars, xPos[0], xPos[1]);
        }
        if (isYValue() && ! skip) {
            appendToXBuffer(chars, start, end);
        }
        if (isZValue()) {
            appendToXBuffer(chars, start, end);
        }
        xPos[0] = 0;
        xPos[1] = 0;
    }
    
    private void appendToXBuffer(char[] chars, int start, int len) {
        if (xBuffer != null && !xBuffer.isEmpty()) {
            xBuffer = xBuffer.concat(String.copyValueOf(chars, start, len));
        }
        else {
            xBuffer = String.copyValueOf(chars, start, len);
        }
    }
    
    private boolean flushXBuffer() throws IOException {
        boolean flushed = false;
        if (xBuffer != null && !xBuffer.isEmpty()) {
            jsonWriteRawValue(xBuffer);
            flushed = true;
        }
        xBuffer = null;
        return flushed;
    }

    @Override
    protected void writeValue(char[] chars, int start, int end) throws IOException {
        if (isXValue()) {
            xPos[0] = start;
            xPos[1] = end;
        }
        if (isYValue()) {
            // if chunkBoundaryBuffer is not empty, we just started with a new input chunk and need to write chunkBoundaryBuffer and xBuffer first
            if (flushChunkBoundaryBuffer()) {
                // write y
                jsonWriteRaw(chars, start, end);

                // write x
                flushXBuffer();
                
            } else {
                // write y
                jsonWriteRawValue(chars, start, end);

                // write x
                if (flushXBuffer()) {
                    jsonWriteRaw(chars, xPos[0], xPos[1]);
                } else {
                    jsonWriteRawValue(chars, xPos[0], xPos[1]);
                }
            }
        }
    }
}
