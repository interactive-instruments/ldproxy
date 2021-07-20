/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.stringtemplates.domain.StringTemplateFilters;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterId implements GeoJsonWriter {

    @Override
    public GeoJsonWriterId create() {
        return new GeoJsonWriterId();
    }

    private String currentId;
    private boolean currentIdIsInteger;
    private boolean writeAtFeatureEnd = false;

    @Override
    public int getSortPriority() {
        return 10;
    }

    private void reset() {
        this.currentId = null;
        this.writeAtFeatureEnd = false;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext,
                        Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        reset();

        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext,
                             Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        if (writeAtFeatureEnd) {
            this.writeAtFeatureEnd = false;

            if (Objects.nonNull(currentId)) {
                if (currentIdIsInteger)
                    transformationContext.getJson()
                                         .writeNumberField("id", Long.valueOf(currentId));
                else
                    transformationContext.getJson()
                                         .writeStringField("id", currentId);
                addLinks(transformationContext, currentId);
                this.currentId = null;
                this.currentIdIsInteger = false;
            }
        }

        // next chain for extensions
        next.accept(transformationContext);

    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext,
                           Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.getState()
                                 .getCurrentFeatureProperty()
                                 .isPresent()
                || transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent()) {

            final FeatureProperty currentFeatureProperty = transformationContext.getState()
                                                                                .getCurrentFeatureProperty()
                                                                                .get();
            String currentValue = transformationContext.getState()
                                                       .getCurrentValue()
                                                       .get();

            if (currentFeatureProperty.isId()) {
                boolean isInteger = (currentFeatureProperty.getType() == FeatureProperty.Type.INTEGER);
                if (writeAtFeatureEnd) {
                    currentId = currentValue;
                    currentIdIsInteger = isInteger;
                } else {
                    if (isInteger)
                        transformationContext.getJson()
                                             .writeNumberField("id", Long.valueOf(currentValue));
                    else
                        transformationContext.getJson()
                                             .writeStringField("id", currentValue);

                    addLinks(transformationContext, currentValue);
                }

            } else {
                this.writeAtFeatureEnd = true;
            }
        }

        next.accept(transformationContext);
    }

    @Override
    public void onCoordinates(FeatureTransformationContextGeoJson transformationContext,
                              Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        this.writeAtFeatureEnd = true;

        // next chain for extensions
        next.accept(transformationContext);
    }

    private void addLinks(FeatureTransformationContextGeoJson transformationContext,
                          String featureId) throws IOException {
        if (transformationContext.isFeatureCollection() &&
                Objects.nonNull(featureId) &&
                !featureId.isEmpty()) {
            transformationContext.getState().addCurrentFeatureLinks(new ImmutableLink.Builder().rel("self")
                                                                                        .href(transformationContext.getServiceUrl() + "/collections/" + transformationContext.getCollectionId() + "/items/" + featureId)
                                                                                        .build());
            Optional<String> template = transformationContext.getApiData()
                                                             .getCollections()
                                                             .get(transformationContext.getCollectionId())
                                                             .getPersistentUriTemplate();
            if (template.isPresent()) {
                transformationContext.getState().addCurrentFeatureLinks(new ImmutableLink.Builder().rel("canonical")
                                                                                            .href(StringTemplateFilters.applyTemplate(template.get(), featureId))
                                                                                            .build());
            }
        }
    }
}
