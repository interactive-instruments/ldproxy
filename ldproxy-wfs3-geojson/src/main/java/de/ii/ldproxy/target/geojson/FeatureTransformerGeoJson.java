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
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.target.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.JsonCoordinateFormatter;
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Consumer;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;

/**
 * @author zahnen
 */
public class FeatureTransformerGeoJson implements FeatureTransformer, FeatureTransformer.OnTheFly {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerGeoJson.class);

    @Override
    public OnTheFlyMapping getOnTheFlyMapping() {
        return new GeoJsonOnTheFlyMapping();
    }

    public enum NESTED_OBJECTS {NEST, FLATTEN}

    public enum MULTIPLICITY {ARRAY, SUFFIX}

    private final JsonGenerator jsonOut;
    private final boolean isFeatureCollection;
    private final CrsTransformer crsTransformer;
    private final List<Wfs3Link> links;
    private final int pageSize;
    private JsonGenerator json;
    private TokenBuffer jsonBuffer;
    private double maxAllowableOffset;
    private final NESTED_OBJECTS nestedObjects;
    private final MULTIPLICITY multiplicity;


    GEO_JSON_GEOMETRY_TYPE currentGeometryType;
    boolean currentGeometryNested;
    CoordinatesWriterType.Builder cwBuilder;
    StringBuilder stringBuilder = new StringBuilder();
    OutputStream outputStream;
    Consumer<Throwable> failStage;
    boolean currentIsId;
    List<String> lastPath = new ArrayList<>();
    List<String> currentPath2 = new ArrayList<>();
    //Map<String, Integer> currentMultiplicities = new HashMap<>();
    String serviceUrl;
    GeoJsonPropertyMapping currentMapping;
    String currentFormatter;
    String currentFieldName;
    boolean currentFieldMulti;
    JsonNestingTracker nestingTracker = new JsonNestingTracker();

    public FeatureTransformerGeoJson(JsonGenerator jsonOut, boolean isFeatureCollection, CrsTransformer crsTransformer, List<Wfs3Link> links, int pageSize, String serviceUrl, double maxAllowableOffset, NESTED_OBJECTS nestedObjects, MULTIPLICITY multiplicity) {
        this.jsonOut = jsonOut;
        this.json = jsonOut;
        this.isFeatureCollection = isFeatureCollection;
        this.crsTransformer = crsTransformer;
        this.links = links;
        this.pageSize = pageSize;
        this.serviceUrl = serviceUrl;
        this.maxAllowableOffset = maxAllowableOffset;

        this.nestedObjects = nestedObjects;
        this.multiplicity = multiplicity;
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

            this.writeLinks(numberReturned.orElse(0) < pageSize);

            if (numberReturned.isPresent()) {
                json.writeNumberField("numberReturned", numberReturned.getAsLong());
            }
            if (numberMatched.isPresent()) {
                json.writeNumberField("numberMatched", numberMatched.getAsLong());
            }
            json.writeStringField("timeStamp", Instant.now()
                                                      .truncatedTo(ChronoUnit.SECONDS)
                                                      .toString());

            writeCrs();

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
            this.writeLinks(false);
            this.writeCrs();
        }

        startBuffering();

        json.writeObjectFieldStart("properties");
    }

    @Override
    public void onFeatureEnd() throws IOException {
        if (nestedObjects == NESTED_OBJECTS.NEST) {
            writePropertyName("", ImmutableList.of());
            //currentMultiplicities = new HashMap<>();
            nestingTracker = new JsonNestingTracker();
        }

        if (json == jsonBuffer)  {
            stopBuffering();
            flushBuffer();
        }

        json.writeEndObject();
        json.writeEndObject();
    }

    @Override
    public void onPropertyStart(TargetMapping mapping, List<Integer> multiplicities) throws IOException {
        if (Objects.nonNull(mapping)) {
            //stringBuilder = new StringBuilder();

            if (Objects.nonNull(currentMapping) && Objects.nonNull(currentMapping.getFormat()) && Objects.equals(currentMapping.getFormat(), ((GeoJsonPropertyMapping) mapping).getFormat())) {
                return;
            }
            this.currentMapping = (GeoJsonPropertyMapping) mapping;

            if (Objects.equals(currentMapping.getType(), GEO_JSON_TYPE.ID)) {
                jsonOut.writeFieldName(currentMapping.getName());
                currentIsId = true;
            } else {
                writePropertyName(currentMapping.getName(), multiplicities);
            }
        } else {
            //stringBuilder = null;
        }
    }

    private void writePropertyName(String name, List<Integer> multiplicities) throws IOException {
        if (nestedObjects == NESTED_OBJECTS.NEST) {
            List<String> path = Splitter.on('.')
                                        .omitEmptyStrings()
                                        .splitToList(name);
            if (path.isEmpty() && lastPath.isEmpty()) {
                return;
            }
            LOGGER.debug("PATH {} {}", lastPath, path);

            /*final int[] increasedMultiplicityLevel = {0};
            final int[] current = {0};
            final boolean[] increased = {false};
            path.stream()
                .filter(element -> element.contains("[") )
                //.map(element -> element.substring(element.indexOf("[") + 1, element.indexOf("]")))

                .forEach(element -> {
                    String multiplicity = element.substring(element.indexOf("[") + 1, element.indexOf("]"));
                    boolean isObject = !(path.indexOf(element) == path.size() - 1);

                    int currentMultiplicity = multiplicities.size() > current[0] ? multiplicities.get(current[0]) : 1;
                    currentMultiplicities.putIfAbsent(multiplicity, currentMultiplicity);
                    LOGGER.debug("{} {} {}", multiplicity, currentMultiplicity, currentMultiplicities.get(multiplicity));
                    if (!Objects.equals(currentMultiplicities.get(multiplicity), currentMultiplicity)) {
                        if (!increased[0]) {
                            increasedMultiplicityLevel[0] = current[0];
                            increased[0] = true;
                        }
                        currentMultiplicities.put(multiplicity, currentMultiplicity);
                    }
                    current[0]++;
                });

            // find index where lastPath and path start to differ
            int i;
            for (i = 0; i < lastPath.size() && i < path.size(); i++) {
                if (!Objects.equals(lastPath.get(i), path.get(i))) break;
            }

            //TODO: test cases
            if (increasedMultiplicityLevel[0] > 0 && increasedMultiplicityLevel[0] < i)  {
                i = increasedMultiplicityLevel[0];
            }*/

            nestingTracker.track(path, multiplicities);

            List<String> closeActions = nestingTracker.getCurrentCloseActions();
            LOGGER.debug("CLOSE {}", closeActions);

            for (int i = 0; i < closeActions.size(); i++) {
                if (i < closeActions.size()-1 || !currentFieldMulti) {
                    switch (closeActions.get(i)) {
                        case "OBJECT":
                            json.writeEndObject();
                            break;
                        case "ARRAY":
                            json.writeEndArray();
                            break;
                    }
                }
            }


            /*int differsAt = nestingTracker.differsAt();

            //close nested objects as well as arrays for multiplicities
            for (int j = lastPath.size() - 1; j >= differsAt; j--) {
                // omit if lastPath is array value that was never opened
                if (j < lastPath.size()-1 || !currentFieldMulti) {
                    boolean closeObject = j < lastPath.size() - 1;
                    boolean closeArray = lastPath.get(j)
                                              .contains("[") && !(closeObject && j == differsAt && j > 0);
                    closeArrayAndOrObject(lastPath.get(j), closeObject, closeArray);
                }
            }*/
            currentFieldMulti = false;

            int differsAt = nestingTracker.differsAt();
            List<List<String>> openActions = nestingTracker.getCurrentOpenActions();
            LOGGER.debug("OPEN {}", openActions);

            for (int i = 0; i < openActions.size(); i++) {
                String fieldName = path.get(differsAt + i);
                for (String action : openActions.get(i)) {
                    switch (action) {
                        case "OBJECT":
                            if (fieldName.contains("[")) {
                                json.writeStartObject();
                            } else {
                                json.writeObjectFieldStart(fieldName.substring(0, fieldName.indexOf("[")));
                            }
                            break;
                        case "ARRAY":
                            if (fieldName.contains("[")) {
                                json.writeArrayFieldStart(fieldName.substring(0, fieldName.indexOf("[")));
                            } else {
                                json.writeStartArray();
                            }
                            break;
                    }
                }
            }

            // open nested objects as well as arrays for multiplicities
            /*for (int j = differsAt; j < path.size() - 1; j++) {
                openArrayAndOrObject(path.get(j));
            }*/

            //TODO: multilevel
            // close and open on changed multiplicities
            /*for (int j = 0; j < increasedMultiplicityLevel[0]; j++) {
                LOGGER.debug("MULTI {}", j);
                json.writeEndObject();
                json.writeStartObject();
            }*/

            // write field name
            if (!path.isEmpty()) {
                String field = path.get(path.size() - 1);
                boolean isMulti = field.contains("[");
                int multi = 0;
                if (isMulti) {
                    String multiplicityKey = field.substring(field.indexOf("[") + 1, field.indexOf("]"));
                    field = field.substring(0, field.indexOf("["));
                    multi = nestingTracker.getCurrentMultiplicityLevel(multiplicityKey);
                }
                if (!isMulti || multi == 1) {
                    LOGGER.debug("FIELD {}", field);
                    //json.writeFieldName(field);
                    currentFieldName = field;
                }
                if (isMulti && multi == 1) {
                    //json.writeStartArray();
                    currentFieldMulti = true;
                }
            }

            lastPath = /*path.size() > 0 ? path.subList(0, path.size() - 1) :*/ path;
            //currentPath2 = path;
        } else {
            //json.writeFieldName(field);
            currentFieldName = name;
        }
    }

    private void openArrayAndOrObject(String name) throws IOException {
        LOGGER.debug("OPEN {}", name);
        if (name.contains("[")) {
            json.writeArrayFieldStart(name.substring(0, name.indexOf("[")));
            json.writeStartObject();
        } else {
            json.writeObjectFieldStart(name);
        }
    }

    private void closeArrayAndOrObject(String name, boolean isObject, boolean isMulti) throws IOException {
        LOGGER.debug("CLOSE {} isObject={} isMulti={}", name, isObject, isMulti);
        if (isObject) {
            json.writeEndObject();
        }
        if (isMulti) {
            json.writeEndArray();
        }
    }

    @Override
    public void onPropertyText(String text) {
        if (Objects.nonNull(currentMapping)) stringBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws IOException {
        // TODO
        //writeValue(mapping, stringBuilder.toString());
        if (stringBuilder.length() > 0) {
            if (currentMapping.getType() == GEO_JSON_TYPE.STRING && currentMapping.getFormat() != null && !currentMapping.getFormat()
                                                                                                                         .isEmpty()) {
                boolean more = false;
                if (currentFormatter == null) {
                    currentFormatter = currentMapping.getFormat()
                                                     .replace("{{serviceUrl}}", serviceUrl);
                }

                int subst = currentFormatter.indexOf("}}");
                if (subst > -1) {
                    currentFormatter = currentFormatter.substring(0, currentFormatter.indexOf("{{")) + stringBuilder.toString() + currentFormatter.substring(subst + 2);
                    more = currentFormatter.contains("}}");
                }

                if (!more) {
                    if (currentFieldName != null) {
                        json.writeFieldName(currentFieldName);
                        currentFieldName = null;
                        if (currentFieldMulti) {
                            json.writeStartArray();
                            currentFieldMulti = false;
                        }
                    }
                    json.writeString(currentFormatter);
                    currentFormatter = null;
                }
            } else if (currentIsId) {
                jsonOut.writeString(stringBuilder.toString());
                currentIsId = false;
            } else {
                if (currentFieldName != null) {
                    json.writeFieldName(currentFieldName);
                    currentFieldName = null;
                    if (currentFieldMulti) {
                        json.writeStartArray();
                        currentFieldMulti = false;
                    }
                }
                writeValue(stringBuilder.toString(), currentMapping.getType());
            }

            stringBuilder.setLength(0);
        } else {
            //json.writeString("NOTXT");
        }
    }

    private void writeValue(String value, GEO_JSON_TYPE type) throws IOException {
        switch (type) {

            case BOOLEAN:
                json.writeBoolean(value.toLowerCase()
                                       .equals("t") || value.toLowerCase()
                                                            .equals("true") || value.equals("1"));
                break;
            case NUMBER:
                try {
                    json.writeNumber(Long.parseLong(value));
                    break;
                } catch (NumberFormatException e) {
                    try {
                        json.writeNumber(Double.parseDouble(value));
                        break;
                    } catch (NumberFormatException e2) {
                        //ignore
                    }
                }
            default:
                json.writeString(value);
        }
    }

    @Override
    public void onGeometryStart(TargetMapping mapping, SimpleFeatureGeometry type, Integer dimension) throws
            IOException {
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

            if (maxAllowableOffset > 0) {
                int minPoints = currentGeometryType == GEO_JSON_GEOMETRY_TYPE.MULTI_POLYGON || currentGeometryType == GEO_JSON_GEOMETRY_TYPE.POLYGON ? 4 : 2;
                cwBuilder.simplifier(maxAllowableOffset, minPoints);
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
                    //json.writeStartArray();
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
                    //json.writeEndArray();
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

    private void writeLinks(boolean isLastPage) throws IOException {
        json.writeFieldName("links");
        json.writeStartArray();

        for (Wfs3Link link : links) {
            if (!(isLastPage && Objects.equals(link.getRel(), "next"))) {
                json.writeStartObject();
                json.writeStringField("href", link.getHref());
                json.writeStringField("rel", link.getRel());
                json.writeStringField("type", link.getType());
                json.writeStringField("title", link.getTitle());
                json.writeEndObject();
            }
        }

        json.writeEndArray();
    }

    private void writeCrs() throws IOException {
        if (Objects.nonNull(crsTransformer) && !Objects.equals(crsTransformer.getTargetCrs(), DEFAULT_CRS)) {
            json.writeStringField("crs", crsTransformer.getTargetCrs().getAsUri());
        }
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
            LOGGER.debug("BUFFER {}", jsonBuffer.toString());
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
