/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import akka.Done;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.target.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.xtraplatform.geometries.domain.CoordinatesWriterType;
import de.ii.xtraplatform.geometries.domain.CrsTransformer;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParserFlow;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
// TODO make an extension of FeatureTransformerGeoJson
public class StreamingGml2GeoJsonFlow implements GmlStreamParserFlow.GmlTransformerFlow {

    public static Flow<ByteString, ByteString, CompletionStage<Done>> transformer(final QName featureType, final FeatureTypeMapping featureTypeMapping, final boolean useFormattedJsonOutput) {
        return GmlStreamParserFlow.transform(featureType, featureTypeMapping, new StreamingGml2GeoJsonFlow(true, useFormattedJsonOutput));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamingGml2GeoJsonFlow.class);

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
    protected boolean useFormattedJsonOutput;
    protected Map<String, Integer> fieldCounter;
    protected boolean isFeatureCollection;
    protected double maxAllowableOffset;
    protected CrsTransformer crsTransformer;

    protected String outputFormat; // as constant somewhere
    protected FeatureTypeMapping featureTypeMapping; // reduceToOutputFormat


    //protected WfsProxyOnTheFlyMapping onTheFlyMapping;

    GEO_JSON_GEOMETRY_TYPE currentGeometryType;
    boolean currentGeometryNested;
    CoordinatesWriterType.Builder cwBuilder;
    StringBuilder stringBuilder;
    OutputStream outputStream;
    Consumer<Throwable> failStage;

    public StreamingGml2GeoJsonFlow(boolean isFeatureCollection, boolean useFormattedJsonOutput) {
        //this.jsonOut = jsonOut;
        //this.json = jsonOut;
        //this.jsonMapper = jsonMapper;
        this.isFeatureCollection = isFeatureCollection;
        this.useFormattedJsonOutput = useFormattedJsonOutput;
        //this.currentPath = new XMLPathTracker();
        //this.crsTransformer = crsTransformer;
        //this.onTheFlyMapping = new GeoJsonOnTheFlyMapping();
        //this.featureTypeMapping = featureTypeMapping;
        //this.outputFormat = outputFormat;
    }

    @Override
    public void initialize(Consumer<ByteString> push, Consumer<Throwable> failStage) {
        this.failStage = failStage;

        try {
            final JsonGenerator jsonGenerator = new JsonFactory().createGenerator(new OutputStream() {
                @Override
                public void write(int i) {

                }

                @Override
                public void write(byte[] bytes, int i, int i1) {
                    push.accept(ByteString.ByteStrings.fromArray(bytes, i, i1));
                }
            }).disable(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM);

            if (useFormattedJsonOutput) {
                jsonGenerator.useDefaultPrettyPrinter();
            }

            StreamingGml2GeoJsonFlow.this.json = jsonGenerator;
            StreamingGml2GeoJsonFlow.this.jsonOut = jsonGenerator;
        } catch (IOException e) {
            LOGGER.debug("STREAMING FAILED");
        }
    }

    @Override
    public String getTargetFormat() {
        return Gml2GeoJsonMappingProvider.MIME_TYPE;
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws IOException {
        if (isFeatureCollection) {
            json.writeStartObject();
            json.writeStringField("type", "FeatureCollection");

            //this.writeSRS();

            json.writeFieldName("features");
            json.writeStartArray();
        }
    }

    @Override
    public void onEnd() throws IOException {
        if (isFeatureCollection) {
            json.writeEndArray();
            json.writeEndObject();
        }
        json.close();
    }

    @Override
    public void onFeatureStart(TargetMapping mapping) throws IOException {
        json.writeStartObject();
        json.writeStringField("type", "Feature");
    }

    @Override
    public void onFeatureEnd() throws IOException {
        json.writeEndObject();
        json.writeEndObject();
    }

    //TODO: move to onFeatureStart
    //@Override
    public void onAttribute(TargetMapping mapping, String value) throws IOException {
        if (Objects.nonNull(mapping) && Objects.nonNull(value)) {

            final GeoJsonPropertyMapping propertyMapping = (GeoJsonPropertyMapping) mapping;

            if (propertyMapping.getType().equals(GEO_JSON_TYPE.ID)) {
                json.writeStringField(propertyMapping.getName(), value);

                // we buffer the attributes until the geometry is written
                // TODO: only if geometry exists
                startBuffering();

                json.writeObjectFieldStart("properties");
            }
        }
    }

    @Override
    public void onPropertyStart(TargetMapping mapping, List<Integer> multiplicities) throws IOException {
        if (Objects.nonNull(mapping)) {
            stringBuilder = new StringBuilder();

            final GeoJsonPropertyMapping propertyMapping = (GeoJsonPropertyMapping) mapping;

            json.writeFieldName(propertyMapping.getName());

            /*if (Objects.nonNull(value)) {
                writeValue(propertyMapping, value);
            }*/
        } else {
            stringBuilder = null;
        }
    }

    @Override
    public void onPropertyText(String text) {
        if (stringBuilder != null) stringBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws IOException {
        // TODO
        //writeValue(mapping, stringBuilder.toString());
        if (stringBuilder != null) json.writeString(stringBuilder.toString());
    }

    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws IOException {
        if (Objects.nonNull(mapping)) {
            stopBuffering();

            final GeoJsonGeometryMapping geometryMapping = (GeoJsonGeometryMapping) mapping;

            currentGeometryType = geometryMapping.getGeometryType();
            if (currentGeometryType == GEO_JSON_GEOMETRY_TYPE.GENERIC) {
                currentGeometryType = GEO_JSON_GEOMETRY_TYPE.forGmlType(type);
            }

            cwBuilder = CoordinatesWriterType.builder();
            cwBuilder.format(new JsonCoordinateFormatter(json));

            if (crsTransformer != null) {
                cwBuilder.transformer(crsTransformer);
            }

            if (dimension != null) {
                cwBuilder.dimension(dimension);
            }

            json.writeObjectFieldStart("geometry");
            json.writeStringField("type", currentGeometryType.toString());
            json.writeFieldName("coordinates");
        }
    }

    @Override
    public void onGeometryNestedStart() throws IOException {
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
    public void onGeometryCoordinates(String text) throws IOException {
        if (currentGeometryType == null) return;

        Writer coordinatesWriter = cwBuilder.build();
        // TODO: coalesce
        coordinatesWriter.write(text);
        coordinatesWriter.close();
    }

    @Override
    public void onGeometryNestedEnd() throws IOException {
        if (currentGeometryType == null) return;

        json.writeEndArray();
    }

    @Override
    public void onGeometryEnd() throws IOException {
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

    private TokenBuffer createJsonBuffer() throws IOException {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);

        if (useFormattedJsonOutput) {
            json.useDefaultPrettyPrinter();
        }
        return json;
    }
}
