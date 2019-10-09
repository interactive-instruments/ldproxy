/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterSkeleton implements GeoJsonWriter {

    @Override
    public GeoJsonWriterSkeleton create() {
        return new GeoJsonWriterSkeleton();
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.isFeatureCollection()) {

            transformationContext.getJson()
                                 .writeStartObject();
            transformationContext.getJson()
                                 .writeStringField("type", "FeatureCollection");
        }

        next.accept(transformationContext);

        if (transformationContext.isFeatureCollection()) {
            transformationContext.getJson()
                                 .writeFieldName("features");
            transformationContext.getJson()
                                 .writeStartArray();
        }
    }

    @Override
    public void onEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        if (transformationContext.isFeatureCollection()) {
            // end of features array
            transformationContext.getJson()
                                 .writeEndArray();
        }

        // next chain for extensions
        next.accept(transformationContext);

        if (transformationContext.isFeatureCollection()) {
            // end of collection object
            transformationContext.getJson()
                                 .writeEndObject();
        }
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        transformationContext.getJson()
                             .writeStartObject();
        transformationContext.getJson()
                             .writeStringField("type", "Feature");

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        // next chain for extensions
        next.accept(transformationContext);

        // end of feature
        transformationContext.getJson()
                             .writeEndObject();
    }
}