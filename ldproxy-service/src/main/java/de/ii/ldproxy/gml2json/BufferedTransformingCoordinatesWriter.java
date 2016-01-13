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
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.LazyStringCoordinateTuple;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author zahnen
 */
public class BufferedTransformingCoordinatesWriter extends DefaultCoordinatesWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(BufferedTransformingCoordinatesWriter.class);
    private static final int BUFFER_SIZE = 10000;
    private LazyStringCoordinateTuple tupleBuffer;
    private final double[] coordinateBuffer;
    private final List<double[]> reversePolygonBuffer;
    private final CrsTransformer transformer;
    private final boolean swap;
    private final boolean reversepolygon;

    private int zCounter = 0;

    public BufferedTransformingCoordinatesWriter(JsonGenerator json, int srsDimension, CrsTransformer transformer, boolean swap, boolean reversepolygon) {
        super(json, srsDimension);
        this.transformer = transformer;
        //this.tupleBuffer = new LazyStringCoordinateTuple();
        this.coordinateBuffer = new double[BUFFER_SIZE];
        this.swap = swap;
        this.reversepolygon = reversepolygon;
        this.reversePolygonBuffer = new ArrayList<>();
    }

    @Override
    protected void writeEnd() throws IOException {
        if (isCompleteTuple()) {
            writeCoordinates(true);
        }
    }

    @Override
    protected void writeSeparator() throws IOException {
        writeCoordinates(false);
    }

    private void writeCoordinates(boolean force) throws IOException {
        if (tupleBuffer != null) {
            if (tupleBuffer.hasX()) {
                coordinateBuffer[counter - 2 - zCounter] = tupleBuffer.getX();
            }
            if (tupleBuffer.hasY()) {
                coordinateBuffer[counter - 1 - zCounter] = tupleBuffer.getY();
            }
            tupleBuffer = null;
        }

        if (force || counter == BUFFER_SIZE) {

            double[] c = postProcessCoordinates(coordinateBuffer, (counter - zCounter) / 2);

            if (this.reversepolygon) {

                if (!force) { // force == end of coordinates
                    // add c to the buffer
                    reversePolygonBuffer.add(c);
                } else {

                    // the buffer is empty, just write out the current c
                    if (reversePolygonBuffer.isEmpty()) {

                        for (int i = 0; i < c.length; i += 2) {
                            json.writeRawValue(Double.toString(c[i]));
                            json.writeRawValue(Double.toString(c[i + 1]));
                            if (i < c.length - 2) {
                                super.writeSeparator();
                            }
                        }
                    } else { // the buffer must be written out in reverse order                   
                        for (int i0 = reversePolygonBuffer.size() - 1; i0 >= 0; i0--) {
                            double[] c0 = reversePolygonBuffer.get(i0);
                            for (int i = 0; i < c0.length; i += 2) {
                                json.writeRawValue(Double.toString(c0[i]));
                                json.writeRawValue(Double.toString(c0[i + 1]));
                                if (i < c0.length - 2) {
                                    super.writeSeparator();
                                }
                            }
                        }
                        reversePolygonBuffer.clear();
                    }
                }
            } else {

                int numCoo;
                if (c.length == coordinateBuffer.length) {
                    numCoo = (counter - zCounter);
                } else {
                    numCoo = c.length;
                }
                for (int i = 0; i < numCoo; i += 2) {
                    json.writeRawValue(Double.toString(c[i]));
                    json.writeRawValue(Double.toString(c[i + 1]));
                    if (i < numCoo - 2) {
                        super.writeSeparator();
                    }
                }
                counter = 0;
            }
        }
    }

    protected double[] postProcessCoordinates(double[] in, int numPts) {
        double[] out;
        if (transformer != null) {
            out = transformer.transform(in, numPts);
        } else {
            if (this.swap) {
                out = new double[numPts * 2];
                for (int i = 0; i < numPts * 2; i += 2) {
                    out[i] = in[i + 1];
                    out[i + 1] = in[i];
                }
            } else {
                out = in;
            }
        }
        // das Array das in den Buffer geht muss eine Kopie sein Fall: out = in;
        // Revere Arrays ... [#430]
        if (this.reversepolygon) {
            double[] out2 = new double[numPts * 2];
            for (int i = 0; i < numPts * 2; i++) {
                out2[numPts * 2 - i - 1] = out[i];
            }
            for (int i = 0; i < numPts * 2; i += 2) {
                out[i] = out2[i + 1];
                out[i + 1] = out2[i];
            }
        }

        // just to be shure not returning the full buffer ...
        if (out.length == numPts * 2) {
            return out;
        }
        // else: just the valid range
        return Arrays.copyOfRange(out, 0, numPts * 2);
    }

    @Override
    protected void jsonWriteRawValue(String val) throws IOException {
        if (tupleBuffer == null) {
            tupleBuffer = new LazyStringCoordinateTuple();
        }
        if (isXValue()) {
            tupleBuffer.setX(val);
        }
        if (isYValue()) {
            tupleBuffer.setY(val);
        }
    }

    @Override
    protected void jsonWriteRawValue(char[] chars, int i, int j) throws IOException {
        if (isZValue()) {
            zCounter++;
        } else {
            coordinateBuffer[counter - 1 - zCounter] = Double.parseDouble(String.copyValueOf(chars, i, j));
        }
    }

    @Override
    protected void jsonWriteRaw(char[] chars, int i, int j) throws IOException {
        if (isXValue()) {
            tupleBuffer.appendX(String.copyValueOf(chars, i, j));
        }
        if (isYValue()) {
            tupleBuffer.appendY(String.copyValueOf(chars, i, j));
        }
    }
}
