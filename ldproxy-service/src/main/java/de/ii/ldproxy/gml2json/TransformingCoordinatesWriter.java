package de.ii.ldproxy.gml2json;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.xsf.logging.XSFLogger;
import java.io.IOException;

import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.LazyStringCoordinateTuple;
import org.forgerock.i18n.slf4j.LocalizedLogger;

/**
 *
 * @author zahnen
 */
public class TransformingCoordinatesWriter extends DefaultCoordinatesWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(TransformingCoordinatesWriter.class);
    private LazyStringCoordinateTuple coordinateBuffer;
    private CrsTransformer transformer;

    public TransformingCoordinatesWriter(JsonGenerator json, int srsDimension, CrsTransformer transformer) {
        super(json, srsDimension);
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

            json.writeRawValue(c.getXasString());
            json.writeRawValue(c.getYasString());
        } else {
            json.writeRawValue(coordinateBuffer.getXasString());
            json.writeRawValue(coordinateBuffer.getYasString());
        }
    }

    @Override
    protected void jsonWriteRawValue(String val) throws IOException {
        //json.writeRawValue(buf);
        if (isXValue()) {
            coordinateBuffer.setX(val);
        }
        if (isYValue()) {
            coordinateBuffer.setY(val);
        }
    }

    @Override
    protected void jsonWriteRawValue(char[] chars, int i, int j) throws IOException {
        //json.writeRawValue(chars, i, j);
        jsonWriteRawValue(String.copyValueOf(chars, i, j));
    }

    @Override
    protected void jsonWriteRaw(char[] chars, int i, int j) throws IOException {
        //json.writeRaw(chars, i, j);
        if (isXValue()) {
            coordinateBuffer.appendX(String.copyValueOf(chars, i, j));
        }
        if (isYValue()) {
            coordinateBuffer.appendY(String.copyValueOf(chars, i, j));
        }
    }
}
