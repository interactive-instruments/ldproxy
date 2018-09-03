package de.ii.ldproxy.target.geojson;

import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.target.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.ogc.wfs.proxy.WfsProxyOnTheFlyMapping;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.EventBasedStreamingFeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
public class GeoJsonFeatureWriter extends AbstractStreamingFeatureWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonFeatureWriter.class);

    protected JsonGenerator json;
    protected JsonGenerator jsonOut;
    protected TokenBuffer jsonBuffer;
    protected ObjectMapper jsonMapper;
    //protected WFS2GSFSLayer layer;
    protected XMLPathTracker currentPath;
    protected EpsgCrs outSRS;
    protected List<String> outFields;
    protected boolean geometryWithSR;
    protected boolean returnGeometry;
    protected Map<String, Integer> fieldCounter;
    protected boolean isFeatureCollection;
    protected double maxAllowableOffset;
    protected CrsTransformer crsTransformer;

    protected String outputFormat; // as constant somewhere
    protected FeatureTypeMapping featureTypeMapping; // reduceToOutputFormat

    public static Flow<EventBasedStreamingFeatureTransformer.TransformEvent, ByteString, Consumer<OutputStream>> writer(JsonGenerator jsonOut) {

        return Flow.fromGraph(new GeoJsonFeatureWriter(jsonOut, true));
    }

    protected WfsProxyOnTheFlyMapping onTheFlyMapping;

    GEO_JSON_GEOMETRY_TYPE currentGeometryType;
    boolean currentGeometryNested;
    CoordinatesWriterType.Builder cwBuilder;
    StringBuilder stringBuilder;
    OutputStream outputStream;

    public GeoJsonFeatureWriter(JsonGenerator jsonOut, boolean isFeatureCollection) {
        this.jsonOut = jsonOut;
        this.json = jsonOut;
        //this.jsonMapper = jsonMapper;
        this.isFeatureCollection = isFeatureCollection;
        //this.currentPath = new XMLPathTracker();
        //this.crsTransformer = crsTransformer;
        //this.onTheFlyMapping = new GeoJsonOnTheFlyMapping();
        //this.featureTypeMapping = featureTypeMapping;
        //this.outputFormat = outputFormat;
    }

    /*@Override
    protected StreamingOutput getStreamingOutput() {
        LOGGER.debug("STREAMING");
        return outputStream -> {
            LOGGER.debug("STREAMING2");
            final JsonGenerator jsonGenerator = new JsonFactory().createGenerator(outputStream);
            GeoJsonFeatureWriter.this.json = jsonGenerator;
            GeoJsonFeatureWriter.this.jsonOut = jsonGenerator;
        };
    }*/

    /*@Override
    protected void setOutputStream(OutputStream outputStream) {
        LOGGER.debug("STREAMING");
        try {
            final JsonGenerator jsonGenerator = new JsonFactory().createGenerator(outputStream).useDefaultPrettyPrinter().disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);
            GeoJsonFeatureWriter.this.json = jsonGenerator;
            GeoJsonFeatureWriter.this.jsonOut = jsonGenerator;
            GeoJsonFeatureWriter.this.outputStream = outputStream;
            LOGGER.debug("STREAMING2");
        } catch (IOException e) {
            LOGGER.debug("STREAMING FAILED");
        }
    }*/

    @Override
    protected void initalize(OutputStream outputStream, Consumer<ByteString> push) {
        LOGGER.debug("STREAMING");
        try {
            final JsonGenerator jsonGenerator = new JsonFactory().createGenerator(new OutputStream() {
                @Override
                public void write(int i) throws IOException {

                }

                @Override
                public void write(byte[] bytes, int i, int i1) throws IOException {
                    push.accept(ByteString.ByteStrings.fromArray(bytes, i, i1));
                }
            }).useDefaultPrettyPrinter().disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);

            GeoJsonFeatureWriter.this.json = jsonGenerator;
            GeoJsonFeatureWriter.this.jsonOut = jsonGenerator;
            GeoJsonFeatureWriter.this.outputStream = outputStream;
            LOGGER.debug("STREAMING2");
        } catch (IOException e) {
            LOGGER.debug("STREAMING FAILED");
        }
    }

    @Override
    protected void writeEvent(final EventBasedStreamingFeatureTransformer.TransformEvent transformEvent, Runnable onComplete, ExecutionContext executionContext) throws IOException {
        //LOGGER.debug(transformEvent.toString());
        //System.out.println(transformEvent.toString());

        new EventBasedStreamingFeatureTransformer.TransformEventHandler(transformEvent) {

            @Override
            protected void onStart(EventBasedStreamingFeatureTransformer.TransformStart transformStart) throws IOException {
                if (isFeatureCollection) {
                    json.writeStartObject();
                    json.writeStringField("type", "FeatureCollection");

                    //this.writeSRS();

                    json.writeFieldName("features");
                    json.writeStartArray();
                }
            }

            @Override
            protected void onEnd(EventBasedStreamingFeatureTransformer.TransformEnd transformEnd) throws IOException {
                if (isFeatureCollection) {
                    json.writeEndArray();
                    json.writeEndObject();
                }
                //json.close();
                json.flush();

                //Futures.future((Callable<Void>) () -> {
                    //json.close();
                    LOGGER.debug("CLOSE STREAM");
                    outputStream.flush();
                    LOGGER.debug("CLOSED STREAM");
                  //  return null;
                //}, executionContext);
                        /*.onComplete(v1 -> {
                            onComplete.run();
                            return null;
                        }, executionContext);*/


            }

            @Override
            protected void onFeatureStart(EventBasedStreamingFeatureTransformer.TransformFeatureStart transformFeatureStart) throws IOException {
                json.writeStartObject();
                json.writeStringField("type", "Feature");
            }

            @Override
            protected void onFeatureEnd(EventBasedStreamingFeatureTransformer.TransformFeatureEnd transformFeatureEnd) throws IOException {
                json.writeEndObject();
                json.writeEndObject();
            }

            @Override
            protected void onAttribute(EventBasedStreamingFeatureTransformer.TransformAttribute transformAttribute) throws IOException {
                if (transformAttribute.mapping.isPresent() && transformAttribute.value.isPresent()) {

                    final GeoJsonPropertyMapping mapping = (GeoJsonPropertyMapping) transformAttribute.mapping.get();

                    if (mapping.getType().equals(GEO_JSON_TYPE.ID)) {
                        json.writeStringField(mapping.getName(), transformAttribute.value.orElse(""));

                        // we buffer the attributes until the geometry is written
                        // TODO: only if geometry exists
                        startBuffering();

                        json.writeObjectFieldStart("properties");
                    }
                }
            }

            @Override
            protected void onPropertyStart(EventBasedStreamingFeatureTransformer.TransformProperty transformProperty) throws IOException {
                if (transformProperty.mapping.isPresent()) {
                    stringBuilder = new StringBuilder();

                    final GeoJsonPropertyMapping mapping = (GeoJsonPropertyMapping) transformProperty.mapping.get();

                    json.writeFieldName(mapping.getName());

                    if (transformProperty.value.isPresent()) {
                        writeValue(mapping, transformProperty.value.get());
                    }
                } else {
                    stringBuilder = null;
                }
            }

            @Override
            protected void onPropertyText(EventBasedStreamingFeatureTransformer.TransformPropertyText transformPropertyText) throws IOException {
                if (stringBuilder != null) stringBuilder.append(transformPropertyText.text);
            }

            @Override
            protected void onPropertyEnd(EventBasedStreamingFeatureTransformer.TransformPropertyEnd transformPropertyEnd) throws IOException {
                //writeValue(mapping, stringBuilder.toString());
                if (stringBuilder != null) json.writeString(stringBuilder.toString());
            }

            @Override
            protected void onGeometryStart(EventBasedStreamingFeatureTransformer.TransformGeometry transformGeometry) throws IOException {
                if (transformGeometry.mapping.isPresent()) {
                    stopBuffering();

                    final GeoJsonGeometryMapping mapping = (GeoJsonGeometryMapping) transformGeometry.mapping.get();

                    currentGeometryType = mapping.getGeometryType();
                    if (currentGeometryType == GEO_JSON_GEOMETRY_TYPE.GENERIC) {
                        currentGeometryType = GEO_JSON_GEOMETRY_TYPE.forGmlType(transformGeometry.type.toSimpleFeatureGeometry());
                    }

                    cwBuilder = CoordinatesWriterType.builder();
                    cwBuilder.format(new JsonCoordinateFormatter(json));

                    if (crsTransformer != null) {
                        cwBuilder.transformer(crsTransformer);
                    }

                    if (transformGeometry.dimension != null) {
                        cwBuilder.dimension(transformGeometry.dimension);
                    }

                    json.writeObjectFieldStart("geometry");
                    json.writeStringField("type", currentGeometryType.toString());
                    json.writeFieldName("coordinates");
                }
            }

            @Override
            protected void onGeometryNestedStart(EventBasedStreamingFeatureTransformer.TransformGeometryNestedStart transformGeometryNestedStart) throws IOException {
                if (currentGeometryType == null) return;

                if (!currentGeometryNested) {
                    // TODO
                    switch (currentGeometryType) {
                        case MULTI_POLYGON:
                            json.writeStartArray();
                        case POLYGON:
                        case MULTI_LINE_STRING:
                            json.writeStartArray();
                    }

                    currentGeometryNested = true;
                }
                json.writeStartArray();
            }

            @Override
            protected void onGeometryCoordinates(EventBasedStreamingFeatureTransformer.TransformGeometryCoordinates transformGeometryCoordinates) throws IOException {
                if (currentGeometryType == null) return;

                Writer coordinatesWriter = cwBuilder.build();
                // TODO: coalesce
                coordinatesWriter.write(transformGeometryCoordinates.text);
                coordinatesWriter.close();
            }

            @Override
            protected void onGeometryNestedEnd(EventBasedStreamingFeatureTransformer.TransformGeometryNestedEnd transformGeometryNestedEnd) throws IOException {
                if (currentGeometryType == null) return;

                json.writeEndArray();
            }

            @Override
            protected void onGeometryEnd(EventBasedStreamingFeatureTransformer.TransformGeometryEnd transformGeometryEnd) throws IOException {
                if (currentGeometryType == null) return;

                if (currentGeometryNested) {
                    // TODO
                    switch (currentGeometryType) {
                        case MULTI_POLYGON:
                            json.writeEndArray();
                        case POLYGON:
                        case MULTI_LINE_STRING:
                            json.writeEndArray();
                    }
                }

                json.writeEndObject();
                flushBuffer();

                currentGeometryType = null;
                currentGeometryNested = false;
                cwBuilder = null;
            }

            private void writeValue(TargetMapping mapping, String value) throws IOException {
                json.writeString(value);
                /*switch (((GeoJsonPropertyMapping)mapping).getType()) {
                            case STRING:
                                json.writeStringField(mapping.getName(), transformProperty.value.get());
                                break;
                            case NUMBER:
                                // TODO: howto recognize int or double
                                try {
                                    json.writeNumberField(mapping.getName(), Long.parseLong(transformProperty.value.get()));
                                } catch (NumberFormatException ex) {
                                    json.writeNumberField(mapping.getName(), Double.parseDouble(transformProperty.value.get()));
                                }
                                break;
                        }*/
            }
        };
    }

    protected final void startBuffering() throws IOException {
        jsonOut.flush();
        this.jsonBuffer = createJsonBuffer();
        this.json = jsonBuffer;
    }

    protected final void stopBuffering() throws IOException {
        if (jsonBuffer != null) {
            jsonBuffer.close();
        }
        this.json = jsonOut;
    }

    protected final void flushBuffer() throws IOException {
        if (jsonBuffer != null) {
            jsonBuffer.serialize(jsonOut);
            jsonBuffer = null;
        }
    }

    // TODO
    private TokenBuffer createJsonBuffer() throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);

        //if (useFormattedJsonOutput) {
        json.useDefaultPrettyPrinter();
        //}
        return json;
    }
}
