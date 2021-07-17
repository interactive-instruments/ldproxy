/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/*
package de.ii.ldproxy.ogcapi.features.jsonfg.app;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson;
import de.ii.ldproxy.ogcapi.features.geojson.domain.GeoJsonWriter;
import de.ii.ldproxy.ogcapi.features.geojson.domain.legacy.GeoJsonGeometryMapping;
import de.ii.ldproxy.ogcapi.features.jsonfg.domain.JsonFgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.geometries.domain.ImmutableCoordinatesWriterJson;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Consumer;

@Component
@Provides
@Instantiate
public class JsonFgWriterGeometry implements GeoJsonWriter {

    @Override
    public JsonFgWriterGeometry create() {
        return new JsonFgWriterGeometry();
    }

    boolean isEnabled;
    private int geometryNestingDepth = 0;
    private boolean geometryOpen;
    private boolean hasGeometry;
    private GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE currentGeometryType;

    ///////
    private SimpleFeatureGeometry currentGeometryType;
    private int currentGeometryNesting;
    private Geometry currentGeometry;
    private List<Point> currentPoints;
    private List<LineString> currentLineStrings;
    private List<LinearRing> currentLinearRings;
    private List<Polygon> currentPolygons;
    private int currentDimension;
    ////////

    @Override
    public int getSortPriority() {
        return 130;
    }

    private void reset() {
        this.geometryNestingDepth = 0;
        this.geometryOpen = false;
        this.hasGeometry = false;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        isEnabled = isEnabled(transformationContext);
        reset();

        next.accept(transformationContext);
    }

    @Override
    public void onProperty(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {

        if (isEnabled && !geometryOpen && !hasGeometry && !transformationContext.getState()
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
                                     .writeFieldName("where");
                transformationContext.getJson()
                                     .writeStartObject();
                transformationContext.getJson()
                                     .writeStringField("type", currentGeometryType.toString());
                transformationContext.getJson()
                                     .writeFieldName("coordinates");
                if (currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.POINT &&
                    currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.MULTI_POINT) {
                    transformationContext.getJson()
                                         .writeStartArray();
                }

                ///// TODO
                switch (currentGeometryType) {
                    case MULTI_POLYGON:
                        //json.writeStartArray();
                    case POLYGON:
                    case MULTI_LINE_STRING:
                        json.writeStartArray();
                }
                /////

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

    //////////////
    @Override
    public void onGeometryStart(FeatureProperty featureProperty, SimpleFeatureGeometry type, Integer dimension) {
        if (Objects.nonNull(featureProperty)) {

            currentProperty = featureProperty;
            currentGeometryType = type;
            if (currentGeometryType==SimpleFeatureGeometry.ANY || currentGeometryType==SimpleFeatureGeometry.NONE ||
                    currentGeometryType==SimpleFeatureGeometry.GEOMETRY_COLLECTION) {
                // only process well-defined primitives or aggregates
                currentProperty = null;
                currentGeometryType = null;
                return;
            }

            currentGeometryNesting = 0;
            currentDimension = dimension;
            currentGeometry = null;

            switch (currentGeometryType) {
                case POINT:
                case MULTI_POINT:
                    currentPoints = new Vector<>();
                    break;
                case LINE_STRING:
                case MULTI_LINE_STRING:
                    currentLineStrings = new Vector<>();
                    break;
                case POLYGON:
                case MULTI_POLYGON:
                    currentPolygons = new Vector<>();
                    currentLinearRings = new Vector<>();
                    break;
            }
        }
    }

    @Override
    public void onGeometryNestedStart() {
        if (Objects.isNull(currentGeometryType))
            return;

        currentGeometryNesting++;
    }

    @Override
    public void onGeometryCoordinates(String text) {
        if (Objects.isNull(currentGeometryType))
            return;

        Coordinate coord;
        CoordinateSequence coords;
        switch (currentGeometryType) {
            case POINT:
            case MULTI_POINT:
                coord = parseCoordinate(text);
                if (coord!=null)
                    currentPoints.add(geometryFactoryWorld.createPoint(coord));
                break;
            case LINE_STRING:
            case MULTI_LINE_STRING:
                coords = parseCoordinates(text);
                if (coords!=null)
                    currentLineStrings.add(geometryFactoryWorld.createLineString(coords));
                break;
            case POLYGON:
            case MULTI_POLYGON:
                coords = parseCoordinates(text);
                if (coords!=null)
                    currentLinearRings.add(geometryFactoryWorld.createLinearRing(coords));
                break;
        }
    }

    private Coordinate parseCoordinate(String text) {
        double[] coord = ArrayUtils.toPrimitive(
                Splitter.on(separatorPattern)
                        .splitToList(text)
                        .stream()
                        .map(Double::parseDouble)
                        .toArray(Double[]::new));
        if (crsTransformer!=null) {
            if (currentDimension==2)
                coord = crsTransformer.transform(coord, 1, swapCoordinates);
            else if (currentDimension==3)
                coord = crsTransformer.transform3d(coord, 1, swapCoordinates);
        }
        if (currentDimension==2)
            return new Coordinate(coord[0],coord[1]);
        else if (currentDimension==3)
            return new Coordinate(coord[0],coord[1],coord[2]);
        return null;
    }

    private CoordinateSequence parseCoordinates(String text) {
        double[] coords = ArrayUtils.toPrimitive(
                Splitter.on(separatorPattern)
                        .omitEmptyStrings()
                        .splitToList(text)
                        .stream()
                        .map(Double::parseDouble)
                        .toArray(Double[]::new));
        if (crsTransformer!=null) {
            if (currentDimension == 2) {
                coords = crsTransformer.transform(coords, coords.length / currentDimension, swapCoordinates);
            } else if (currentDimension == 3) {
                coords = crsTransformer.transform3d(coords, coords.length / currentDimension, swapCoordinates);
            }
        }
        return PackedCoordinateSequenceFactory.DOUBLE_FACTORY.create(coords, currentDimension);
    }

    @Override
    public void onGeometryNestedEnd() {
        if (currentGeometryType == SimpleFeatureGeometry.MULTI_POLYGON) {
            if (currentGeometryNesting == 1) {
                int rings = currentLinearRings.size();
                if (rings == 1)
                    currentPolygons.add(geometryFactoryWorld.createPolygon(currentLinearRings.get(0)));
                else if (rings > 1)
                    currentPolygons.add(geometryFactoryWorld.createPolygon(currentLinearRings.get(0), currentLinearRings.subList(1, rings).toArray(new LinearRing[0])));
                currentLinearRings = new Vector<>();
            }
        }
        currentGeometryNesting--;
    }

    @Override
    public void onGeometryEnd() {
        switch (currentGeometryType) {
            case POINT:
                if (currentPoints.size()==1)
                    currentGeometry = currentPoints.get(0);
                break;
            case MULTI_POINT:
                currentGeometry = geometryFactoryWorld.createMultiPoint(currentPoints.toArray(new Point[0]));
                break;
            case LINE_STRING:
                if (currentLineStrings.size()==1)
                    currentGeometry = currentLineStrings.get(0);
                break;
            case MULTI_LINE_STRING:
                currentGeometry = geometryFactoryWorld.createMultiLineString(currentLineStrings.toArray(new LineString[0]));
                break;
            case POLYGON:
                int rings = currentLinearRings.size();
                if (rings==1)
                    currentGeometry = geometryFactoryWorld.createPolygon(currentLinearRings.get(0));
                else if (rings>1)
                    currentGeometry = geometryFactoryWorld.createPolygon(currentLinearRings.get(0), currentLinearRings.subList(1, rings).toArray(new LinearRing[0]));
                currentLinearRings = new Vector<>();
                break;
            case MULTI_POLYGON:
                currentGeometry = geometryFactoryWorld.createMultiPolygon(currentPolygons.toArray(new Polygon[0]));
                break;
        }

        currentProperty = null;
        currentGeometryType = null;
    }
    ////////

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

            //close coordinates
            if (currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.POINT &&
                currentGeometryType != GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE.MULTI_POINT) {
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
                                 .writeFieldName("where");
            transformationContext.getJson()
                                 .writeNull();

            transformationContext.flushBuffer();
        }
        this.hasGeometry = false;
    }

    private boolean isEnabled(FeatureTransformationContextGeoJson transformationContext) {
        return transformationContext.getMediaType().equals(FeaturesFormatJsonFg.MEDIA_TYPE)
                && transformationContext.getApiData()
                                        .getCollections()
                                        .get(transformationContext.getCollectionId())
                                        .getExtension(JsonFgConfiguration.class)
                                        .filter(JsonFgConfiguration::isEnabled)
                                        .map(cfg -> cfg.getWhere().getEnabled())
                                        .isPresent();
    }

}
*/
