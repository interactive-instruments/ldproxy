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
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class JsonFgWriterFeatureType implements GeoJsonWriter {

    boolean isEnabled;
    List<String> writeAtEnd;

    @Override
    public JsonFgWriterFeatureType create() {
        return new JsonFgWriterFeatureType();
    }

    @Override
    public int getSortPriority() {
        return 120;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(transformationContext);
        writeAtEnd = ImmutableList.of();

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext,
                               Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (isEnabled) {
            Optional<JsonFgConfiguration> config = transformationContext.getApiData().getExtension(JsonFgConfiguration.class, transformationContext.getCollectionId());
            if (config.isPresent()) {
                List<String> types = config.get().getFeatureTypes();
                if (!types.isEmpty()) {
                    if (types.stream()
                             .anyMatch(type -> type.contains("{{type}}"))) {
                        writeAtEnd = types;
                    } else {
                        writeTypes(transformationContext, types);
                    }
                }
            }
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext,
                           Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (!writeAtEnd.isEmpty()
                && transformationContext.getState()
                                 .getCurrentFeatureProperty()
                                 .isPresent()
                && transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent()) {

            final FeatureProperty currentFeatureProperty = transformationContext.getState()
                                                                                .getCurrentFeatureProperty()
                                                                                .get();
            String currentValue = transformationContext.getState()
                                                       .getCurrentValue()
                                                       .get();

            if (currentFeatureProperty.isType()) {
                writeAtEnd = writeAtEnd.stream()
                                       .map(type -> type.replace("{{type}}", currentValue))
                                       .collect(Collectors.toUnmodifiableList());
            }
        }

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (!writeAtEnd.isEmpty()) {
            writeTypes(transformationContext, writeAtEnd);
            writeAtEnd = ImmutableList.of();
        }

        next.accept(transformationContext);
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getApiData()
                                    .getCollections()
                                    .get(transformationContext.getCollectionId())
                                    .getExtension(JsonFgConfiguration.class)
                                    .filter(JsonFgConfiguration::isEnabled)
                                    .filter(cfg -> Objects.nonNull(cfg.getFeatureTypes()) && !cfg.getFeatureTypes().isEmpty())
                                    .filter(cfg -> cfg.getIncludeInGeoJson().contains(JsonFgConfiguration.OPTION.featureTypes) ||
                                            transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE))
                                    .isPresent();
    }

    private void writeTypes(FeatureTransformationContextGeoJson transformationContext, List<String> types) throws IOException {
        if (types.stream()
                 .allMatch(type -> type.contains("{{type}}")))
            return;

        JsonGenerator json = transformationContext.getJson();
        if (types.size()==1)
            json.writeStringField("featureType", types.get(0));
        else {
            json.writeFieldName("featureType");
            json.writeStartArray(types.size());
            for (String t: types)
                if (!t.contains("{{type}}"))
                    json.writeString(t);
            json.writeEndArray();
        }
    }

}
