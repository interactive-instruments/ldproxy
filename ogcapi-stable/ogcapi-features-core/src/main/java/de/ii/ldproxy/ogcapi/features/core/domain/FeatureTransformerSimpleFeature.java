/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Generic Feature Transformer for features where all properties are literals (no arrays or objects)
 */
public abstract class FeatureTransformerSimpleFeature extends FeatureTransformerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerSimpleFeature.class);

    protected final OutputStream outputStream;

    protected final String collectionId;
    protected final CrsTransformer crsTransformer;
    protected final boolean swapCoordinates;
    protected final List<String> properties;
    protected final boolean allProperties;
    protected final GeometryFactory geometryFactory;
    protected final Pattern separatorPattern;
    protected final int maxMultiplicity;
    protected final Map<String, Object> processingParameters;

    protected SimpleFeatureGeometry currentGeometryType;
    private int currentGeometryNesting;

    protected Geometry currentGeometry;
    protected SortedMap<String, Object> currentProperties;
    protected String currentId;
    private List<Point> currentPoints;
    private List<LineString> currentLineStrings;
    private List<LinearRing> currentLinearRings;
    private List<Polygon> currentPolygons;
    private int currentDimension;

    private StringBuilder currentValueBuilder = new StringBuilder();
    private FeatureProperty currentProperty = null;
    private String currentPropertyName = null;

    public <T extends ExtensionConfiguration & FeatureTransformations> FeatureTransformerSimpleFeature(Class<T> buildingBlockConfigurationClass,
                                                                                                       OgcApiDataV2 apiData,
                                                                                                       String collectionId,
                                                                                                       Map<String, Codelist> codelists,
                                                                                                       String serviceUrl,
                                                                                                       boolean isFeatureCollection,
                                                                                                       OutputStream outputStream,
                                                                                                       CrsTransformer crsTransformer,
                                                                                                       boolean swapCoordinates,
                                                                                                       List<String> fields,
                                                                                                       int maxMultiplicity,
                                                                                                       Map<String, Object> processingParameters) {
        super(buildingBlockConfigurationClass, apiData, collectionId, codelists, serviceUrl, isFeatureCollection);
        this.outputStream = outputStream;
        this.crsTransformer = crsTransformer;
        this.swapCoordinates = swapCoordinates;
        this.processingParameters = processingParameters;
        this.collectionId = collectionId;
        this.properties = fields;
        this.maxMultiplicity = maxMultiplicity;
        this.allProperties = properties.contains("*");
        this.separatorPattern = Pattern.compile("\\s+|\\,");
        this.geometryFactory = new GeometryFactory();
    }

    @Override
    public void onFeatureStart(FeatureType featureType) {
        resetFeatureInfo();
    }

    private void resetFeatureInfo() {
        // reset feature information
        currentGeometry = null;
        currentPropertyName = null;
        currentId = null;
        currentProperty = null;
        currentProperties = new TreeMap<>();
    }

    @Override
    public void onPropertyStart(FeatureProperty featureProperty, List<Integer> multiplicities) {

        // NOTE: feature attributes are always literal values, no arrays or objects

        FeatureProperty processedFeatureProperty = featureProperty;
        if (Objects.nonNull(processedFeatureProperty)) {

            List<FeaturePropertySchemaTransformer> schemaTransformations = getSchemaTransformations(processedFeatureProperty);
            for (FeaturePropertySchemaTransformer schemaTransformer : schemaTransformations) {
                processedFeatureProperty = schemaTransformer.transform(processedFeatureProperty);
            }
        }

        // the property may have been removed by the transformations
        if (Objects.isNull(processedFeatureProperty))
            return;

        if (multiplicities.stream()
                          .anyMatch(mult -> mult > maxMultiplicity))
            return;

        currentProperty = processedFeatureProperty;
        currentPropertyName = currentProperty.getName();

        if (!allProperties && !(properties.contains(currentPropertyName) ||
                                properties.contains(currentPropertyName.replace("[]", "")))) {
            currentProperty = null;
        }

        if (Objects.nonNull(currentProperty))
            multiplicities.forEach(index -> currentPropertyName = currentPropertyName.replaceFirst("\\[[^\\]]*\\]", "."+index));
    }

    @Override
    public void onPropertyText(String text) {
        if (Objects.nonNull(currentProperty))
            currentValueBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() throws Exception {
        if (currentValueBuilder.length() > 0) {
            String value = currentValueBuilder.toString();
            List<FeaturePropertyValueTransformer> valueTransformations = getValueTransformations(currentProperty);
            for (FeaturePropertyValueTransformer valueTransformer : valueTransformations) {
                value = valueTransformer.transform(value);
                if (Objects.isNull(value))
                    break;
            }
            // skip, if the value has been transformed to null
            if (Objects.nonNull(value)) {
                switch (currentProperty.getType()) {
                    case BOOLEAN:
                        currentProperties.put(currentPropertyName, value.toLowerCase().equals("t") || value.toLowerCase().equals("true") || value.equals("1"));
                        break;
                    case INTEGER:
                        currentProperties.put(currentPropertyName, Long.parseLong(value));
                        break;
                    case FLOAT:
                        currentProperties.put(currentPropertyName, Double.parseDouble(value));
                        break;
                    case DATETIME:
                    case STRING:
                    default:
                        currentProperties.put(currentPropertyName, value);
                }

                if (currentProperty.isId())
                    currentId = value;
            }
        }

        // reset
        currentProperty = null;
        currentPropertyName = null;
        currentValueBuilder.setLength(0);
    }

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
                    currentPoints.add(geometryFactory.createPoint(coord));
                break;
            case LINE_STRING:
            case MULTI_LINE_STRING:
                coords = parseCoordinates(text);
                if (coords!=null)
                    currentLineStrings.add(geometryFactory.createLineString(coords));
                break;
            case POLYGON:
            case MULTI_POLYGON:
                coords = parseCoordinates(text);
                if (coords!=null)
                    currentLinearRings.add(geometryFactory.createLinearRing(coords));
                break;
        }
    }

    private Coordinate parseCoordinate(String text) {
        double[] coord = ArrayUtils.toPrimitive(
                Splitter.on(separatorPattern)
                        .splitToList(text)
                        .stream()
                        .map(num -> Double.parseDouble(num))
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
                        .map(num -> Double.parseDouble(num))
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
        switch (currentGeometryType) {
            case MULTI_POLYGON:
                if (currentGeometryNesting==1) {
                    int rings = currentLinearRings.size();
                    if (rings == 1)
                        currentPolygons.add(geometryFactory.createPolygon(currentLinearRings.get(0)));
                    else if (rings > 1)
                        currentPolygons.add(geometryFactory.createPolygon(currentLinearRings.get(0), currentLinearRings.subList(1, rings).toArray(new LinearRing[0])));
                    currentLinearRings = new Vector<>();
                }
                break;
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
                currentGeometry = geometryFactory.createMultiPoint(currentPoints.toArray(new Point[0]));
                break;
            case LINE_STRING:
                if (currentLineStrings.size()==1)
                    currentGeometry = currentLineStrings.get(0);
                break;
            case MULTI_LINE_STRING:
                currentGeometry = geometryFactory.createMultiLineString(currentLineStrings.toArray(new LineString[0]));
                break;
            case POLYGON:
                int rings = currentLinearRings.size();
                if (rings==1)
                    currentGeometry = geometryFactory.createPolygon(currentLinearRings.get(0));
                else if (rings>1)
                    currentGeometry = geometryFactory.createPolygon(currentLinearRings.get(0), currentLinearRings.subList(1, rings).toArray(new LinearRing[0]));
                currentLinearRings = new Vector<>();
                break;
            case MULTI_POLYGON:
                currentGeometry = geometryFactory.createMultiPolygon(currentPolygons.toArray(new Polygon[0]));
                break;
        }

        currentProperty = null;
        currentGeometryType = null;
    }
}
