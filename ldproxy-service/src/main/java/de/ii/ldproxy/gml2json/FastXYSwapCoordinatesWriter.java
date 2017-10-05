/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.gml2json;

import de.ii.xsf.logging.XSFLogger;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;

/**
 *
 * @author zahnen
 */
public class FastXYSwapCoordinatesWriter extends DefaultCoordinatesWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(FastXYSwapCoordinatesWriter.class);
    private int[] xPos;
    private String xBuffer;

    public FastXYSwapCoordinatesWriter(CoordinateFormatter formatter, int srsDimension) {
        super(formatter, srsDimension);
        this.xPos = new int[2];
    }

    @Override
    protected void writeEnd() throws IOException {
        if (xBuffer != null && !xBuffer.isEmpty()) {
            formatValue(xBuffer);
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
            formatValue(xBuffer);
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
                formatRaw(chars, start, end);

                // write x
                flushXBuffer();
                
            } else {
                // write y
                formatValue(chars, start, end);

                // write x
                if (flushXBuffer()) {
                    formatRaw(chars, xPos[0], xPos[1]);
                } else {
                    formatValue(chars, xPos[0], xPos[1]);
                }
            }
        }
    }
}
