/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson.MULTIPLICITY;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson.NESTED_OBJECTS;
import de.ii.ldproxy.target.geojson.GeoJsonMapping.GEO_JSON_TYPE;
import de.ii.ldproxy.wfs3.templates.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterProperties implements GeoJsonWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonWriterProperties.class);

    private final JsonNestingStrategy nestingStrategy;
    private final StringBuilder stringBuilder = new StringBuilder();
    private List<String> lastPath = new ArrayList<>();
    private Map<String, Integer> currentMultiplicities = new HashMap<>();
    private String currentFormatter;
    private String currentFieldName;
    private boolean currentFieldMulti;
    private boolean currentStarted;
    private JsonNestingTracker nestingTracker;

    public GeoJsonWriterProperties() {
        this.nestingStrategy = new JsonNestingStrategyNestArray();
        this.nestingTracker = new JsonNestingTracker(nestingStrategy);
    }

    @Override
    public GeoJsonWriterProperties create() {
        return new GeoJsonWriterProperties();
    }

    @Override
    public int getSortPriority() {
        return 40;
    }

    /*private void reset() {
        this.stringBuilder.setLength(0);
        this.lastPath = new ArrayList<>();
        this.currentMultiplicities = new HashMap<>();
        this.currentFormatter = null;
        this.currentFieldName = null;
        this.currentFieldMulti = false;
        this.currentStarted = false;
    }*/

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        //reset();

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        writePropertyName(transformationContext.getJson(), "", ImmutableList.of(), NESTED_OBJECTS.NEST);
        this.currentMultiplicities = new HashMap<>();
        this.nestingTracker = new JsonNestingTracker(nestingStrategy);


        if (currentStarted) {
            this.currentStarted = false;

            // end of "properties"
            transformationContext.getJson()
                                 .writeEndObject();
        }



        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        //TODO if NESTED_OBJECT -> write to buffer until onFeatureEnd, somehow catch id and save to map
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (!transformationContext.getState()
                                  .getCurrentMapping()
                                  .isPresent()
                || transformationContext.getState()
                                        .getCurrentMapping()
                                        .get()
                                        .getType() == GEO_JSON_TYPE.ID
                || !transformationContext.getState()
                                         .getCurrentValue()
                                         .isPresent()) return;

        final GeoJsonPropertyMapping currentMapping = (GeoJsonPropertyMapping) transformationContext.getState()
                                                                                                    .getCurrentMapping()
                                                                                                    .get();
        String currentValue = transformationContext.getState()
                                                   .getCurrentValue()
                                                   .get();
        JsonGenerator json = transformationContext.getJson();
        //TODO
        //JsonGenerator jsonOut = transformationContext.getJsonGenerator();

        NESTED_OBJECTS nestedObjectStrategy = transformationContext.getGeoJsonConfig()
                                                                   .getNestedObjectStrategy();
        MULTIPLICITY multiplicityStrategy = transformationContext.getGeoJsonConfig()
                                                                 .getMultiplicityStrategy();
        List<Integer> multiplicities = transformationContext.getState()
                                                            .getCurrentMultiplicity();

        if (!currentStarted) {
            this.currentStarted = true;

            transformationContext.getJson()
                                 .writeObjectFieldStart("properties");
        }

        //TODO if REFERENCE and EMBED
        // - extract id, write buffer from map


        writePropertyName(json, currentMapping.getName(), multiplicities, nestedObjectStrategy);


        if (currentMapping.getType() == GEO_JSON_TYPE.STRING && currentMapping.getFormat() != null && !currentMapping.getFormat()
                                                                                                                     .isEmpty()) {
            //TODO serviceUrl to StringTemplateFilters, additionalSubstitutions
            boolean more = false;
            String formattedValue = "";
            if (currentFormatter == null) {

                formattedValue = StringTemplateFilters.applyTemplate(currentMapping.getFormat(), currentValue);

                formattedValue = formattedValue
                        .replace("{{serviceUrl}}", transformationContext.getServiceUrl());

                int subst = formattedValue.indexOf("}}");
                if (subst > -1) {
                    formattedValue = formattedValue.substring(0, formattedValue.indexOf("{{")) + currentValue + formattedValue.substring(subst + 2);
                    more = formattedValue.contains("}}");
                }
            } else {
                int subst = currentFormatter.indexOf("}}");
                if (subst > -1) {
                    formattedValue = currentFormatter.substring(0, currentFormatter.indexOf("{{")) + currentValue + currentFormatter.substring(subst + 2);
                    more = formattedValue.contains("}}");
                }
            }
            if (more) {
                this.currentFormatter = formattedValue;
                return;
            } /*else {
                currentFormatter = null;
            }


            boolean more = false;
            if (currentFormatter == null) {
                currentFormatter = currentMapping.getFormat()
                                                 .replace("{{serviceUrl}}", transformationContext.getServiceUrl());
            }

            //TODO: shouldn't we replace {{currentFieldName}} with current value?
            int subst = currentFormatter.indexOf("}}");
            if (subst > -1) {
                currentFormatter = currentFormatter.substring(0, currentFormatter.indexOf("{{")) + currentValue + currentFormatter.substring(subst + 2);
                more = currentFormatter.contains("}}");
            }

            if (more) {
                return;
            }*/ else {
                if (currentFieldName != null) {
                    json.writeFieldName(currentFieldName);
                    currentFieldName = null;
                    if (currentFieldMulti) {
                        json.writeStartArray();
                        currentFieldMulti = false;
                    }
                }
                json.writeString(formattedValue);
                currentFormatter = null;
            }
        } else {
            if (currentFieldName != null) {
                json.writeFieldName(currentFieldName);
                currentFieldName = null;
                if (currentFieldMulti) {
                    json.writeStartArray();
                    currentFieldMulti = false;
                }
            }
            writeValue(json, currentValue, currentMapping.getType());
        }
    }

    private void writePropertyName(JsonGenerator json, String name, List<Integer> multiplicities, NESTED_OBJECTS nestedObjectStrategy) throws IOException {
        if (nestedObjectStrategy == NESTED_OBJECTS.NEST) {
            List<String> path = Splitter.on('.')
                                        .omitEmptyStrings()
                                        .splitToList(Strings.nullToEmpty(name));
            if (path.isEmpty() && lastPath.isEmpty()) {
                return;
            }
            LOGGER.debug("PATH {} {}", lastPath, path);

            /*final int[] increasedMultiplicityLevel = {0};
            final int[] current = {0};
            path.stream()
                .filter(element -> element.contains("[")) //&& !(path.indexOf(element) == path.size() - 1)
                //.map(element -> element.substring(element.indexOf("[") + 1, element.indexOf("]")))

                .forEach(element -> {
                    String multiplicity = element.substring(element.indexOf("[") + 1, element.indexOf("]"));
                    boolean isObject = !(path.indexOf(element) == path.size() - 1);

                    int currentMultiplicity = multiplicities.size() > current[0] ? multiplicities.get(current[0]) : 1;
                    currentMultiplicities.putIfAbsent(multiplicity, currentMultiplicity);
                    LOGGER.debug("{} {} {}", multiplicity, currentMultiplicity, currentMultiplicities.get(multiplicity));
                    if (!Objects.equals(currentMultiplicities.get(multiplicity), currentMultiplicity)) {
                        if (isObject) {
                            increasedMultiplicityLevel[0]++;
                        }
                        currentMultiplicities.put(multiplicity, currentMultiplicity);
                    }
                    current[0]++;
                });*/

            boolean doNotCloseValueArray = currentFieldMulti;
            nestingTracker.track(path, multiplicities, json, doNotCloseValueArray);
            currentFieldMulti = false;

            // find index where lastPath and path start to differ
            /*int i;
            for (i = 0; i < lastPath.size() && i < path.size(); i++) {
                if (!Objects.equals(lastPath.get(i), path.get(i))) break;
            }

            //close nested objects as well as arrays for multiplicities
            for (int j = lastPath.size() - 1; j >= i; j--) {
                closeArrayAndOrObject(json, lastPath.get(j), j < lastPath.size() - 1, lastPath.get(j)
                                                                                              .contains("["));
            }

            // open nested objects as well as arrays for multiplicities
            for (int j = i; j < path.size() - 1; j++) {
                openArrayAndOrObject(json, path.get(j));
            }

            //TODO: multilevel
            // close and open on changed multiplicities
            for (int j = 0; j < increasedMultiplicityLevel[0]; j++) {
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

            lastPath = path; //path.size() > 0 ? path.subList(0, path.size() - 1) : path;
            //currentPath2 = path;
        } else {
            //json.writeFieldName(field);
            currentFieldName = name;
        }
    }

    private void openArrayAndOrObject(JsonGenerator json, String name) throws IOException {
        LOGGER.debug("OPEN {}", name);
        if (name.contains("[")) {
            json.writeArrayFieldStart(name.substring(0, name.indexOf("[")));
            json.writeStartObject();
        } else {
            json.writeObjectFieldStart(name);
        }
    }

    private void closeArrayAndOrObject(JsonGenerator json, String name, boolean isObject, boolean isMulti) throws IOException {
        LOGGER.debug("CLOSE {} isObject={} isMulti={}", name, isObject, isMulti);
        if (isObject) {
            json.writeEndObject();
        }
        if (isMulti) {
            json.writeEndArray();
        }
    }

    private void writeValue(JsonGenerator json, String value, GEO_JSON_TYPE type) throws IOException {
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
}
