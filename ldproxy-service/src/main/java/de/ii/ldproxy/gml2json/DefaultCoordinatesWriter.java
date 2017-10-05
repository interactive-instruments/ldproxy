/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.gml2json;

import de.ii.xsf.logging.XSFLogger;
import java.io.IOException;
import java.io.Writer;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class DefaultCoordinatesWriter extends Writer {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(DefaultCoordinatesWriter.class);
    protected CoordinateFormatter formatter;
    private boolean started;
    protected String chunkBoundaryBuffer;
    private int srsDimension;
    protected boolean lastWhite;
    protected int counter;
    private boolean endOfCoordinate;
    protected static final int OUTDIM = 2;

    protected boolean skip = false;

    public DefaultCoordinatesWriter(CoordinateFormatter formatter, int srsDimension) {
        this.started = false;
        this.lastWhite = true;
        this.counter = 0;
        this.formatter = formatter;
        this.srsDimension = srsDimension;
    }

    @Override
    public final void write(char[] chars, int i, int i1) throws IOException {
        
        boolean endOfChunk = false;
        int read = 0;

        if (!started) {
            started = true;
            writeStart();
        }

        // iterate over chunk
        for (int j = i; j < i + i1; j++) {
            endOfChunk = j == i + i1 - 1;

            // skip leading, trailing and back-to-back whitespaces
            if (isWhite(chars[j])) {
                if (!lastWhite) {
                    // we just found the end of a coordinate
                    lastWhite = true;
                    counter++;

                    writeValue(chars, j - read, read);

                    if (srsDimension > OUTDIM && isYValue()) {
                        skip = true;
                    }

                    // we just found the end of a coordinate pair
                    if (isCompleteTuple()) {
                        endOfCoordinate = true;
                        skip = false;
                    }
                    read = -1;
                } else {
                    read--;
                }
            } else {
                lastWhite = false;
            }

            // if we reach the end of the provided input chunk and the last character
            // is not a whitespace, append the chars since the last whitespace to chunkBoundaryBuffer
            if (endOfChunk) {
                writeChunkEnd(chars, j - read, read + 1, read);
            } else if (endOfCoordinate && !lastWhite) {
                writeSeparator();
                endOfCoordinate = false;
            }
            read++;
        }
    }

    @Override
    public final void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        if (!lastWhite) {
            counter++;
        }
        flushChunkBoundaryBuffer();

        writeEnd();

        formatter.close();
    }

    private boolean isWhite(char chr) {
        if (chr == ' ' || chr == '\n' || chr == '\t' || chr == '\r' || chr == ',') {
            return true;
        }
        return false;
    }

    protected boolean isXValue() {
        return counter % srsDimension == 1;
    }

    protected boolean isYValue() {
        return counter % srsDimension == 2 % srsDimension;
    }

    protected boolean isZValue() {
        return srsDimension == 3 && counter % 3 == 0;
    }

    protected boolean isCompleteTuple() {
        return (srsDimension == 2 && isYValue()) || (srsDimension == 3 && isZValue());
    }

    protected void writeStart() throws IOException {
        formatter.open();
    }

    protected void writeSeparator() throws IOException {
        formatter.separator();
    }

    protected void writeEnd() throws IOException {

    }

    protected void writeChunkEnd(char[] chars, int start, int end, int read) throws IOException {
        if (!skip && read > -1) {
            appendToChunkBoundaryBuffer(chars, start, end);
        }
    }

    protected boolean flushChunkBoundaryBuffer() throws IOException {
        boolean flushed = false;
        if (chunkBoundaryBuffer != null && !chunkBoundaryBuffer.isEmpty()) {
            formatValue(chunkBoundaryBuffer);
            flushed = true;
        }
        chunkBoundaryBuffer = null;
        return flushed;
    }

    protected void appendToChunkBoundaryBuffer(char[] chars, int start, int end) {        
        if (chunkBoundaryBuffer != null) {
            chunkBoundaryBuffer = chunkBoundaryBuffer.concat(String.copyValueOf(chars, start, end));
        } else {
            chunkBoundaryBuffer = String.copyValueOf(chars, start, end);
        }
    }

    protected void writeValue(char[] chars, int start, int end) throws IOException {
        // if chunkBoundaryBuffer is not empty, we just started with a new input chunk and need to write chunkBoundaryBuffer first
        if (flushChunkBoundaryBuffer()) {
            formatRaw(chars, start, end);
        } else {
            formatValue(chars, start, end);
        }
    }

    protected void formatValue(String buf) throws IOException {
        if (!skip) {
            formatter.value(buf);
        }
    }

    protected void formatValue(char[] chars, int i, int j) throws IOException {
        if (!skip) {
            formatter.value(chars, i, j);
        }
    }

    protected void formatRaw(char[] chars, int i, int j) throws IOException {
        if (!skip) {
            formatter.raw(chars, i, j);
        }
    }
}
