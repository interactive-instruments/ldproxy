/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.gml2json.CoordinatesWriterType;
import de.ii.ldproxy.gml2json.JsonCoordinateFormatter;
import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping;
import de.ii.ldproxy.output.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.output.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.ldproxy.output.geojson.GeoJsonPropertyMapping;
import de.ii.ldproxy.output.geojson.Gml2GeoJsonMappingProvider;
import de.ii.ldproxy.wfs3.Wfs3Link;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.GmlFeatureTypeAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
public class FeatureTransformerGeoJson implements FeatureTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGeoJson.class);

    private final JsonGenerator jsonOut;
    private final boolean isFeatureCollection;
    private final CrsTransformer crsTransformer;
    private final List<Wfs3Link> links;
    private final int pageSize;
    private JsonGenerator json;
    private TokenBuffer jsonBuffer;
    private double maxAllowableOffset;


    GEO_JSON_GEOMETRY_TYPE currentGeometryType;
    boolean currentGeometryNested;
    CoordinatesWriterType.Builder cwBuilder;
    StringBuilder stringBuilder;
    OutputStream outputStream;
    Consumer<Throwable> failStage;

    public FeatureTransformerGeoJson(JsonGenerator jsonOut, boolean isFeatureCollection, CrsTransformer crsTransformer, List<Wfs3Link> links, int pageSize) {
        this.jsonOut = jsonOut;
        this.json = jsonOut;
        this.isFeatureCollection = isFeatureCollection;
        this.crsTransformer = crsTransformer;
        this.links = links;
        this.pageSize = pageSize;
    }



    @Override
    public String getTargetFormat() {
        return Gml2GeoJsonMappingProvider.MIME_TYPE;
    }

    @Override
    public void onStart(OptionalInt numberReturned, OptionalInt numberMatched) throws IOException {
        if (isFeatureCollection) {
            json.writeStartObject();
            json.writeStringField("type", "FeatureCollection");

            //this.writeSRS();

            this.writeLinks(numberReturned.orElse(0) < pageSize);

            if (numberReturned.isPresent()) {
                json.writeNumberField("numberReturned", numberReturned.getAsInt());
            }
            if (numberMatched.isPresent()) {
                json.writeNumberField("numberMatched", numberMatched.getAsInt());
            }
            json.writeStringField("timeStamp", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());

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

        if (!isFeatureCollection) {
            //this.writeSRS();
            this.writeLinks(false);
        }
    }

    @Override
    public void onFeatureEnd() throws IOException {
        json.writeEndObject();
        json.writeEndObject();
    }

    //TODO: move to onFeatureStart
    @Override
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
    public void onPropertyStart(TargetMapping mapping) throws IOException {
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
        if (stringBuilder != null) {
            json.writeString(stringBuilder.toString());
        } else {
            json.writeString("NOTXT");
        }
    }

    @Override
    public void onGeometryStart(TargetMapping mapping, GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE type, Integer dimension) throws IOException {
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

    private void writeLinks(boolean isLastPage) throws IOException {
        json.writeFieldName("links");
        json.writeStartArray();

        for (Wfs3Link link : links) {
            if (!(isLastPage && link.rel.equals("next"))) {
                json.writeStartObject();
                json.writeStringField("href", link.href);
                json.writeStringField("rel", link.rel);
                json.writeStringField("type", link.type);
                json.writeStringField("title", link.title);
                json.writeEndObject();
            }
        }

        json.writeEndArray();
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
