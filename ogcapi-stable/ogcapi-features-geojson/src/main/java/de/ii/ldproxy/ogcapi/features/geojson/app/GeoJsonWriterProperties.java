/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    private JsonNestingStrategy nestingStrategy;
    private final StringBuilder stringBuilder = new StringBuilder();
    private List<String> lastPath = new ArrayList<>();
    private Map<String, Integer> currentMultiplicities = new HashMap<>();
    private String currentFormatter;
    private String currentFieldName;
    private boolean currentFieldMulti;
    private boolean currentStarted;
    private JsonNestingTracker nestingTracker;

    public GeoJsonWriterProperties() {
        //this.nestingStrategy = JsonNestingStrategyFactory.getNestingStrategy();
        //this.nestingTracker = new JsonNestingTracker(nestingStrategy);
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
    public void onStart(FeatureTransformationContextGeoJson transformationContext,
                        Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        //reset();

        this.nestingStrategy = JsonNestingStrategyFactory.getNestingStrategy(transformationContext.getGeoJsonConfig()
                                                                                                  .getNestedObjectStrategy(), transformationContext.getGeoJsonConfig()
                                                                                                                                                   .getMultiplicityStrategy(), transformationContext.getGeoJsonConfig()
                                                                                                                                                                                                    .getSeparator());
        this.nestingTracker = new JsonNestingTracker(nestingStrategy);

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext,
                             Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        writePropertyName(transformationContext.getJson(), "", ImmutableList.of(), transformationContext.getGeoJsonConfig()
                                                                                                        .getNestedObjectStrategy(), transformationContext.getGeoJsonConfig()
                                                                                                                                                         .getMultiplicityStrategy());
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
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        //TODO if NESTED_OBJECT -> write to buffer until onFeatureEnd, somehow catch id and save to map
    }

    protected String getPropertiesFieldName() {
        return "properties";
    }

    protected boolean shouldSkipProperty(FeatureTransformationContextGeoJson transformationContext) {
        return !hasMappingAndValue(transformationContext)
                || !propertyIsInFields(transformationContext, transformationContext.getState()
                                                                                   .getCurrentFeatureProperty()
                                                                                   .get()
                                                                                   .getName())
                || (transformationContext.getState()
                                         .getCurrentFeatureProperty()
                                         .get()
                                         .isId() /*TODO && !((GeoJsonPropertyMapping) transformationContext.getState()
                                                                                                                           .getCurrentFeatureType()
                                                                                                                           .get()).isIdAsProperty()*/);
    }

    protected boolean hasMappingAndValue(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getState()
                                    .getCurrentFeatureProperty()
                                    .isPresent()
                && transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent();
    }

    protected boolean propertyIsInFields(FeatureTransformationContextGeoJson transformationContext,
                                         String... properties) {
        return transformationContext.getFields()
                                    .isEmpty()
                || transformationContext.getFields()
                                        .contains("*")
                || transformationContext.getFields()
                                        .stream()
                                        .anyMatch(field -> Arrays.asList(properties)
                                                                 .contains(field));
    }

    protected void writeId(JsonGenerator json, String currentValue, String name,
                           FeatureProperty.Type type, List<Integer> multiplicities,
                           FeatureTransformerGeoJson.NESTED_OBJECTS nestedObjectStrategy,
                           FeatureTransformerGeoJson.MULTIPLICITY multiplicityStrategy,
                           FeatureTransformationContextGeoJson transformationContext) throws IOException {
        writePropertyName(json, getIdFieldName(name), multiplicities, nestedObjectStrategy, multiplicityStrategy);
        writeValue(json, currentValue, type);
    }

    protected String getIdFieldName(String name) {
        return name;
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext,
                           Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (shouldSkipProperty(transformationContext)) return;

        final FeatureProperty currentFeatureProperty = transformationContext.getState()
                                                                    .getCurrentFeatureProperty()
                                                                    .get();
        String currentValue = transformationContext.getState()
                                                   .getCurrentValue()
                                                   .get();
        JsonGenerator json = transformationContext.getJson();
        //TODO
        //JsonGenerator jsonOut = transformationContext.getJsonGenerator();

        FeatureTransformerGeoJson.NESTED_OBJECTS nestedObjectStrategy = transformationContext.getGeoJsonConfig()
                                                                   .getNestedObjectStrategy();
        FeatureTransformerGeoJson.MULTIPLICITY multiplicityStrategy = transformationContext.getGeoJsonConfig()
                                                                 .getMultiplicityStrategy();
        List<Integer> multiplicities = transformationContext.getState()
                                                            .getCurrentMultiplicity();

        if (!currentStarted) {
            this.currentStarted = true;

            transformationContext.getJson()
                                 .writeObjectFieldStart(getPropertiesFieldName());
        }

        //TODO if REFERENCE and EMBED
        // - extract id, write buffer from map

        //TODO: new transformations handling
        /*if (currentFeatureProperty.getType() == GEO_JSON_TYPE.STRING && currentFeatureProperty.getFormat() != null && !currentFeatureProperty.getFormat()
                                                                                                                     .isEmpty()) {
            //TODO serviceUrl to StringTemplateFilters, additionalSubstitutions
            boolean more = false;
            String formattedValue = "";
            if (currentFormatter == null) {

                formattedValue = StringTemplateFilters.applyTemplate(currentFeatureProperty.getFormat(), currentValue);

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
            } else {
                currentValue = formattedValue;
                this.currentFormatter = null;
            }
        }*/

        if (currentFeatureProperty.isId()) {
            writeId(json, currentValue, currentFeatureProperty.getName(), currentFeatureProperty.getType(), multiplicities, nestedObjectStrategy, multiplicityStrategy, transformationContext);
        } else {
            writePropertyName(json, currentFeatureProperty.getName(), multiplicities, nestedObjectStrategy, multiplicityStrategy);
            writeValue(json, currentValue, currentFeatureProperty.getType());
        }
    }

    private void writePropertyName(JsonGenerator json, String name, List<Integer> multiplicities,
                                   FeatureTransformerGeoJson.NESTED_OBJECTS nestedObjectStrategy,
                                   FeatureTransformerGeoJson.MULTIPLICITY multiplicityStrategy) throws IOException {

        List<String> path = Splitter.on('.')
                                    .omitEmptyStrings()
                                    .splitToList(Strings.nullToEmpty(name));
        if (path.isEmpty() && lastPath.isEmpty()) {
            return;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("PATH {} {}", lastPath, path);
        }

        boolean doNotCloseValueArray = currentFieldMulti;
        nestingTracker.track(path, multiplicities, json, doNotCloseValueArray);
        currentFieldMulti = false;

        if (nestedObjectStrategy == FeatureTransformerGeoJson.NESTED_OBJECTS.NEST) {
            lastPath = path;
        } else {
            if (name.contains("[")) {
                currentFieldMulti = true;
            }
            currentFieldName = name.replaceAll("\\[\\]", "");
        }
    }

    private void writeValue(JsonGenerator json, String value, FeatureProperty.Type type) throws IOException {
        switch (type) {

            case BOOLEAN:
                json.writeBoolean(value.toLowerCase()
                                       .equals("t") || value.toLowerCase()
                                                            .equals("true") || value.equals("1"));
                break;
            case INTEGER:
                try {
                    json.writeNumber(Long.parseLong(value));
                    break;
                } catch (NumberFormatException e) {
                    //ignore
                }
            case FLOAT:
                try {
                    json.writeNumber(Double.parseDouble(value));
                    break;
                } catch (NumberFormatException e2) {
                    //ignore
                }
            /*case NUMBER:
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
                }*/
            default:
                json.writeString(value);
        }
    }
}
