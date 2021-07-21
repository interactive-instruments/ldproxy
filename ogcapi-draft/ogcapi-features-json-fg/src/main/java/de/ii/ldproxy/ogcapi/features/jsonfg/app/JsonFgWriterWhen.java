/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class JsonFgWriterWhen implements GeoJsonWriter {

    boolean isEnabled;
    String currentIntervalStart;
    String currentIntervalEnd;
    String currentInstant;
    List<String> currentQueryables;

    @Override
    public JsonFgWriterWhen create() {
        return new JsonFgWriterWhen();
    }

    @Override
    public int getSortPriority() {
        return 100;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(transformationContext);

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) {
        if (isEnabled) {
            currentIntervalStart = null;
            currentIntervalEnd = null;
            currentInstant = null;
            currentQueryables = transformationContext.getApiData()
                                                           .getCollections()
                                                           .get(transformationContext.getCollectionId())
                                                           .getExtension(FeaturesCoreConfiguration.class)
                                                           .filter(FeaturesCoreConfiguration::isEnabled)
                                                           .flatMap(cfg -> cfg.getQueryables().map(FeaturesCollectionQueryables::getTemporal))
                                                           .orElse(ImmutableList.of());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext,
                             Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled) {
            if (Objects.nonNull(currentInstant)) {
                JsonGenerator json = transformationContext.getJson();
                json.writeFieldName("when");
                json.writeStartObject();
                json.writeStringField("instant", currentInstant);
                json.writeEndObject();
            } else if (Objects.nonNull(currentIntervalStart) || Objects.nonNull(currentIntervalEnd)) {
                JsonGenerator json = transformationContext.getJson();
                json.writeFieldName("when");
                json.writeStartObject();
                json.writeArrayFieldStart("interval");
                if (Objects.nonNull(currentIntervalStart))
                    json.writeString(currentIntervalStart);
                else
                    json.writeNull();
                if (Objects.nonNull(currentIntervalEnd))
                    json.writeString(currentIntervalEnd);
                else
                    json.writeNull();
                json.writeEndArray();
                json.writeEndObject();
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext,
                           Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled
                && !currentQueryables.isEmpty()
                && transformationContext.getState()
                                        .getCurrentFeatureProperty()
                                        .isPresent()
                && transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent()) {

            final FeatureProperty currentFeatureProperty = transformationContext.getState()
                                                                                .getCurrentFeatureProperty()
                                                                                .get();
            final String currentPropertyName = currentFeatureProperty.getName();

            final String currentValue = transformationContext.getState()
                                                       .getCurrentValue()
                                                       .get();

            // TODO this is too simple and only works for simple data, but it is sufficient for the T17 data
            //      and this should be straightforward with the updated feature transformers
            if (currentQueryables.contains(currentPropertyName)) {
                if (currentQueryables.get(0).equals(currentPropertyName)) {
                    if (currentQueryables.size()==1)
                        currentInstant = currentValue;
                    else
                        currentIntervalStart = currentValue;
                } else if (currentQueryables.get(1).equals(currentPropertyName)) {
                    currentIntervalEnd = currentValue;
                }
            }
        }

        next.accept(transformationContext);
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE)
                && transformationContext.getApiData()
                                        .getCollections()
                                        .get(transformationContext.getCollectionId())
                                        .getExtension(JsonFgConfiguration.class)
                                        .filter(JsonFgConfiguration::isEnabled)
                                        .filter(JsonFgConfiguration::getWhen)
                                        .isPresent();
    }
}
