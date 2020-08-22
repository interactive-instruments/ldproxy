/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.app;

import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesWriterJson;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterGeometry implements GeoJsonWriter {

    @Override
    public GeoJsonWriterGeometry create() {
        return new GeoJsonWriterGeometry();
    }

    private int geometryNestingDepth = 0;
    private boolean geometryOpen;
    private boolean hasGeometry;
    private GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE currentGeometryType;

    @Override
    public int getSortPriority() {
        return 30;
    }

    private void reset() {
        this.geometryNestingDepth = 0;
        this.geometryOpen = false;
        this.hasGeometry = false;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        reset();

        next.accept(transformationContext);
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        if (!geometryOpen && !hasGeometry && !transformationContext.getState()
                                                                   .isBuffering()) {
            // buffer properties until geometry arrives
            transformationContext.startBuffering();
        }

        next.accept(transformationContext);
    }

    @Override
    public void onCoordinates(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.getState()
                                 .getCurrentGeometryType()
                                 .isPresent()
                && transformationContext.getState()
                                        .getCoordinatesWriterBuilder()
                                        .isPresent()
                && transformationContext.getState()
                                        .getCurrentValue()
                                        .isPresent()) {

            this.currentGeometryType = transformationContext.getState()
                                                            .getCurrentGeometryType()
                                                            .get();
            int currentGeometryNestingChange = currentGeometryType == GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.LINE_STRING ? 0 : transformationContext.getState()
                                                                                                                                                           .getCurrentGeometryNestingChange();

            // handle nesting
            if (!geometryOpen) {
                this.geometryOpen = true;
                this.hasGeometry = true;
                //TODO: to FeatureTransformerGeoJson
                this.geometryNestingDepth = currentGeometryNestingChange;

                transformationContext.stopBuffering();

                transformationContext.getJson()
                                     .writeFieldName("geometry");
                transformationContext.getJson()
                                     .writeStartObject();
                transformationContext.getJson()
                                     .writeStringField("type", currentGeometryType.toString());
                transformationContext.getJson()
                                     .writeFieldName("coordinates");
                if (currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.POINT) {
                    transformationContext.getJson()
                                         .writeStartArray();
                }

                // TODO
                /*switch (currentGeometryType) {
                    case MULTI_POLYGON:
                        //json.writeStartArray();
                    case POLYGON:
                    case MULTI_LINE_STRING:
                        json.writeStartArray();
                }*/

                for (int i = 0; i < currentGeometryNestingChange; i++) {
                    transformationContext.getJson()
                                         .writeStartArray();
                }
            } else if (currentGeometryNestingChange > 0) {
                for (int i = 0; i < currentGeometryNestingChange; i++) {
                    transformationContext.getJson()
                                         .writeEndArray();
                }
                for (int i = 0; i < currentGeometryNestingChange; i++) {
                    transformationContext.getJson()
                                         .writeStartArray();
                }
            }

            Writer coordinatesWriter = transformationContext.getState()
                                                            .getCoordinatesWriterBuilder()
                                                            .get()
                                                            //TODO
                                                            .coordinatesWriter(ImmutableCoordinatesWriterJson.of(transformationContext.getJson(), 2))
                                                            .build();
            coordinatesWriter.write(transformationContext.getState()
                                                         .getCurrentValue()
                                                         .get());
            coordinatesWriter.close();
        }

        next.accept(transformationContext);
    }

    @Override
    public void onGeometryEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        next.accept(transformationContext);

        // close geometry field and stop buffering
        closeGeometryIfAny(transformationContext);

    }

    @Override
    public void onFeatureEnd(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        // close geometry if no coordinates were written and stop buffering
        closeGeometryIfNone(transformationContext);

        // next chain for extensions
        next.accept(transformationContext);
    }

    private void closeGeometryIfAny(FeatureTransformationContextGeoJson transformationContext) throws IOException {
        if (geometryOpen) {
            this.geometryOpen = false;

            // close nesting braces
            for (int i = 0; i < geometryNestingDepth; i++) {
                transformationContext.getJson()
                                     .writeEndArray();
            }

            this.geometryNestingDepth = 0;

        /*switch (geometryType.get()) {
            case MULTI_POLYGON:
                //json.writeEndArray();
            case POLYGON:
            case MULTI_LINE_STRING:
                json.writeEndArray();
        }*/

            //close coordinates
            if (currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.POINT) {
                transformationContext.getJson()
                                     .writeEndArray();
            }

            //close geometry object
            transformationContext.getJson()
                                 .writeEndObject();

            transformationContext.flushBuffer();
        }
    }

    private void closeGeometryIfNone(FeatureTransformationContextGeoJson transformationContext) throws IOException {

        if (!hasGeometry) {
            transformationContext.stopBuffering();

            // null geometry
            transformationContext.getJson()
                                 .writeFieldName("geometry");
            transformationContext.getJson()
                                 .writeNull();

            transformationContext.flushBuffer();
        }
        this.hasGeometry = false;
    }
}
