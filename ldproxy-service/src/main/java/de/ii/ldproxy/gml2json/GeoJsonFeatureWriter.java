package de.ii.ldproxy.gml2json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping;
import de.ii.ogc.wfs.proxy.WfsProxyFeatureTypeMapping;
import de.ii.xsf.logging.XSFLogger;
import org.codehaus.staxmate.in.SMInputCursor;
import org.forgerock.i18n.slf4j.LocalizedLogger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fischer
 */
public class GeoJsonFeatureWriter extends AbstractFeatureWriter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GeoJsonFeatureWriter.class);
           
    public GeoJsonFeatureWriter(JsonGenerator jsonOut, ObjectMapper jsonMapper, boolean isFeatureCollection, WfsProxyFeatureTypeMapping featureTypeMapping, String outputFormat) {
        super(jsonOut, jsonMapper, isFeatureCollection);
        this.featureTypeMapping = featureTypeMapping;
        this.outputFormat = outputFormat;
    }

    private void writeSRS() throws IOException {
        json.writeFieldName("srs");
        json.writeStartObject();
        json.writeStringField("type", "name");
        json.writeFieldName("properties");
        json.writeStartObject();
        json.writeStringField("type", outSRS.getAsUrn());
        json.writeEndObject();
        json.writeEndObject();
    }

    @Override
    protected void writeGeometryHeader(GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE type) throws IOException {
        json.writeStringField("type", type.toString());
        json.writeFieldName("coordinates");
    }

    @Override
    protected void writePointGeometry(String x, String y) throws IOException {
        json.writeStringField("type", "Point");
        json.writeFieldName("coordinates");
        json.writeStartArray();
        json.writeRaw(x);
        json.writeRaw(" ,");
        json.writeRaw(y);
        json.writeEndArray();
    }

    @Override
    protected void writeFeatureEnd() throws IOException {
        json.writeEndObject();
        json.writeEndObject();
    }

    @Override
    protected void writeEnd() throws IOException {
        if (isFeatureCollection) {
            json.writeEndArray();
            json.writeEndObject();
        }
        json.close();
    }

    @Override
    protected void writeStart(SMInputCursor rootFuture) throws IOException {
        if (isFeatureCollection) {
            json.writeStartObject();
            json.writeStringField("type", "FeatureCollection");

            //this.writeSRS();

            json.writeFieldName("features");
            json.writeStartArray();
        }
    }
    
    @Override
    protected void writeStart(Future<SMInputCursor> rootFuture) throws IOException {
        try {
            this.writeStart(rootFuture.get());
        } catch (InterruptedException ex) {
            Logger.getLogger(GeoJsonFeatureWriter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(GeoJsonFeatureWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    


    @Override
    protected void writeFeatureStart() throws IOException {
        json.writeStartObject();
        json.writeStringField("type", "Feature");

        if (!isFeatureCollection) {
            //this.writeSRS();
        }



        //json.writeObjectFieldStart("properties");
    }

    @Override
    protected void writeStartProperties() throws IOException {
        // we buffer the attributes until the geometry is written
        // TODO: only if geometry exists
        startBuffering();

        json.writeObjectFieldStart("properties");
    }


}
