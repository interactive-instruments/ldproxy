/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformations;
import de.ii.ldproxy.ogcapi.features.core.domain.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.features.geojson.app.GeoJsonConfiguration;
import de.ii.xtraplatform.streams.domain.HttpClient;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import no.ecc.vectortile.VectorTileEncoder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FeatureTransformerTilesMVT implements FeatureTransformer2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerTilesMVT.class);

    private OutputStream outputStream;

    private final int limit;
    private final String collectionId;
    private final Tile tile;
    private final TileMatrixSet tileMatrixSet;
    private final CrsTransformer crsTransformer;
    private final boolean swapCoordinates;
    private final Map<String, Codelist> codelists;
    private final Optional<FeatureTransformations> baseTransformations;
    private final Optional<FeatureTransformations> geojsonTransformations;
    private final FeatureTransformationContextTiles transformationContext;
    private final Map<String, Object> processingParameters;
    private final TilesConfiguration tilesConfiguration;
    private final VectorTileEncoder encoder;
    private final AffineTransformation affineTransformation;
    private final int polygonLimit;
    private final int lineStringLimit;
    private final int pointLimit;
    private final String layerName;
    private final List<String> properties;
    private final GeometryFactory geometryFactory;
    private final Pattern separatorPattern;

    private SimpleFeatureGeometry currentGeometryType;
    private int currentGeometryNesting;

    private Geometry currentGeometry;
    private List<Point> currentPoints;
    private List<LineString> currentLineStrings;
    private List<LinearRing> currentLinearRings;
    private List<Polygon> currentPolygons;
    private int currentDimension;

    private SortedMap<String, Object> currentProperties;
    private String currentId;
    private StringBuilder currentValueBuilder = new StringBuilder();
    private FeatureProperty currentProperty = null;
    private String currentPropertyName = null;
    private int polygonCount = 0;
    private int lineStringCount = 0;
    private int pointCount = 0;

    public FeatureTransformerTilesMVT(FeatureTransformationContextTiles transformationContext, HttpClient httpClient) {
        this.outputStream = transformationContext.getOutputStream();
        this.limit = transformationContext.getLimit();
        this.crsTransformer = transformationContext.getCrsTransformer()
                                                   .orElse(null);
        this.swapCoordinates = transformationContext.shouldSwapCoordinates();
        this.codelists = transformationContext.getCodelists();
        this.transformationContext = transformationContext;
        this.processingParameters = transformationContext.getProcessingParameters();
        this.collectionId = transformationContext.getCollectionId();
        this.tile = transformationContext.getTile();
        this.tileMatrixSet = tile.getTileMatrixSet();
        this.properties = transformationContext.getFields();
        this.geometryFactory = new GeometryFactory();
        this.separatorPattern = Pattern.compile("\\s+|\\,");

        if (collectionId!=null) {
            FeatureTypeConfigurationOgcApi featureType = transformationContext.getApiData()
                    .getCollections()
                    .get(transformationContext.getCollectionId());
            baseTransformations = Objects.isNull(featureType) ? Optional.empty() : featureType
                    .getExtension(OgcApiFeaturesCoreConfiguration.class)
                    .map(configuration -> configuration);
            // TODO keep those only for the GeoJSON variant
            geojsonTransformations = Objects.isNull(featureType) ? Optional.empty() : featureType
                    .getExtension(GeoJsonConfiguration.class)
                    .map(configuration -> configuration);
            tilesConfiguration = transformationContext.getConfiguration();
            layerName = collectionId;
        } else {
            baseTransformations = Optional.empty();
            geojsonTransformations = Optional.empty();
            tilesConfiguration = transformationContext.getConfiguration();
            layerName = "layer";
        }

        this.encoder = new VectorTileEncoder(tileMatrixSet.getTileExtent());
        this.affineTransformation = tile.createTransformNativeToTile();

        this.polygonLimit = tilesConfiguration!=null ? tilesConfiguration.getMaxPolygonPerTileDefault() : 10000;
        this.lineStringLimit = tilesConfiguration!=null ? tilesConfiguration.getMaxLineStringPerTileDefault() : 10000;
        this.pointLimit = tilesConfiguration!=null ? tilesConfiguration.getMaxPointPerTileDefault() : 10000;
    }

    @Override
    public String getTargetFormat() {
        return TileFormatMVT.MEDIA_TYPE.toString();
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) {

        LOGGER.trace("START");

        if (numberReturned.isPresent()) {
            long returned = numberReturned.getAsLong();
            long matched = numberMatched.orElse(-1);
            LOGGER.trace("numberMatched {}", matched);
            LOGGER.trace("numberReturned {}", returned);
        }
    }

    @Override
    public void onEnd() {

        /*
        for (FeatureProcess process : processes.asList()) {
            data = process.execute(data, processingParameters);
        }
         */

        try {
            byte[] mvt = encoder.encode();
            outputStream.write(mvt);
            outputStream.flush();

            // write/update tile in cache
            File tileFile = transformationContext.getTileFile();
            if (!tileFile.exists() || tileFile.canWrite()) {
                FileUtils.writeByteArrayToFile(tileFile, mvt);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing output stream.");
        }

        LOGGER.trace("Response written.");
    }

    @Override
    public void onFeatureStart(FeatureType featureType) {

        // reset feature information
        currentGeometry = null;
        currentProperty = null;
        currentPropertyName = null;
        currentProperties = new TreeMap<>();
        currentId = null;
    }

    @Override
    public void onFeatureEnd() {

        if (currentGeometry==null)
            return;

        currentGeometry.apply(affineTransformation);

        // filter features
        String geomType = currentGeometry.getGeometryType();
        if (geomType.contains("Polygon")) {
            currentGeometry.reverse();
            double area = currentGeometry.getArea();
            if (area <= 4)
                return;
            if (polygonCount++ > polygonLimit)
                return;
        } else if (geomType.contains("LineString")) {
            double length = currentGeometry.getLength();
            if (length <= 2)
                return;
            if (lineStringCount++ > lineStringLimit)
                return;
        } else if (geomType.contains("Point")) {
            if (pointCount++ > pointLimit)
                return;
        }

        // If we have an id that happens to be a long value, use it
        Long id = null;
        if (currentId != null) {
            try {
                id = Long.parseLong(currentId);
            } catch (Exception e) {
                // nothing to do
            }
        }

        // Add the feature with the layer name, a Map with attributes and the JTS Geometry.
        if (id != null)
            encoder.addFeature(layerName, currentProperties, currentGeometry, id);
        else
            encoder.addFeature(layerName, currentProperties, currentGeometry);

        // reset
        currentId = null;
        currentProperty = null;
        currentProperties = new TreeMap<>();
    }

    @Override
    public void onPropertyStart(FeatureProperty featureProperty, List<Integer> multiplicities) {

        // NOTE: MVT feature attributes are always literal values, no arrays or objects

        FeatureProperty processedFeatureProperty = featureProperty;
        if (Objects.nonNull(processedFeatureProperty)) {

            List<FeaturePropertySchemaTransformer> schemaTransformations = getSchemaTransformations(processedFeatureProperty);
            for (FeaturePropertySchemaTransformer schemaTransformer : schemaTransformations) {
                processedFeatureProperty = schemaTransformer.transform(processedFeatureProperty);
            }
        }

        currentProperty = processedFeatureProperty;
        currentPropertyName = currentProperty.getName();
        multiplicities.stream()
                .forEachOrdered(index -> currentPropertyName = currentPropertyName.replaceFirst("\\[[^\\]]*\\]", "."+index));

        if (!properties.contains("*")) {
            int idx = currentPropertyName.indexOf(".");
            if (idx!=-1 && !properties.contains(currentPropertyName.substring(0,idx)))
                currentProperty = null;
            return;
        }
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
            }
            // skip, if the value has been transformed to null
            if (Objects.nonNull(value)) {
                switch (currentProperty.getType()) {
                    case BOOLEAN:
                        currentProperties.put(currentPropertyName, value.toLowerCase().equals("t") || value.toLowerCase().equals("true") || value.equals("1"));
                        break;
                    case INTEGER:
                        currentProperties.put(currentPropertyName, Long.parseLong(value));
                    case FLOAT:
                        currentProperties.put(currentPropertyName, Double.parseDouble(value));
                    case DATETIME:
                    case STRING:
                    default:
                        currentProperties.put(currentPropertyName, value);
                }
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
                int rings = currentLinearRings.size();
                if (rings==1)
                    currentPolygons.add(geometryFactory.createPolygon(currentLinearRings.get(0)));
                else if (rings>1)
                    currentPolygons.add(geometryFactory.createPolygon(currentLinearRings.get(0),currentLinearRings.subList(1,rings).toArray(new LinearRing[0])));
                currentLinearRings = new Vector<>();
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
                    currentGeometry = geometryFactory.createPolygon(currentLinearRings.get(0),currentLinearRings.subList(1,rings).toArray(new LinearRing[0]));
                currentLinearRings = new Vector<>();
                break;
            case MULTI_POLYGON:
                currentGeometry = geometryFactory.createMultiPolygon(currentPolygons.toArray(new Polygon[0]));
                break;
        }

        currentProperty = null;
        currentGeometryType = null;
    }

    // TODO move somewhere central for reuse?
    private List<FeaturePropertyValueTransformer> getValueTransformations(FeatureProperty featureProperty) {
        List<FeaturePropertyValueTransformer> valueTransformations = null;
        if (baseTransformations.isPresent()) {
            valueTransformations = baseTransformations.get()
                    .getValueTransformations(transformationContext.getCodelists(), transformationContext.getServiceUrl())
                    .get(featureProperty.getName()
                            .replaceAll("\\[[^\\]]+?\\]", "[]"));
        }
        if (geojsonTransformations.isPresent()) {
            if (Objects.nonNull(valueTransformations)) {
                List<FeaturePropertyValueTransformer> moreTransformations = geojsonTransformations.get()
                        .getValueTransformations(new HashMap<String, Codelist>(), transformationContext.getServiceUrl())
                        .get(featureProperty.getName()
                                .replaceAll("\\[[^\\]]+?\\]", "[]"));
                if (Objects.nonNull(moreTransformations)) {
                    valueTransformations = Stream
                            .of(valueTransformations, moreTransformations)
                            .flatMap(Collection::stream)
                            .collect(ImmutableList.toImmutableList());
                }
            } else {
                valueTransformations = geojsonTransformations.get()
                        .getValueTransformations(new HashMap<String, Codelist>(), transformationContext.getServiceUrl())
                        .get(featureProperty.getName()
                                .replaceAll("\\[[^\\]]+?\\]", "[]"));
            }
        }

        return Objects.nonNull(valueTransformations) ? valueTransformations : ImmutableList.of();
    }

    // TODO move somewhere central for reuse?
    private List<FeaturePropertySchemaTransformer> getSchemaTransformations(FeatureProperty featureProperty) {
        List<FeaturePropertySchemaTransformer> schemaTransformations = null;
        if (baseTransformations.isPresent()) {
            schemaTransformations = baseTransformations.get()
                    .getSchemaTransformations(false)
                    .get(featureProperty.getName()
                            .replaceAll("\\[[^\\]]+?\\]", "[]"));
        }
        if (geojsonTransformations.isPresent()) {
            if (Objects.nonNull(schemaTransformations)) {
                List<FeaturePropertySchemaTransformer> moreTransformations = geojsonTransformations.get()
                        .getSchemaTransformations(false)
                        .get(featureProperty.getName()
                                .replaceAll("\\[[^\\]]+?\\]", "[]"));
                if (Objects.nonNull(moreTransformations)) {
                    schemaTransformations = Stream
                            .of(schemaTransformations, moreTransformations)
                            .flatMap(Collection::stream)
                            .collect(ImmutableList.toImmutableList());
                }
            } else {
                schemaTransformations = geojsonTransformations.get()
                        .getSchemaTransformations(false)
                        .get(featureProperty.getName()
                                .replaceAll("\\[[^\\]]+?\\]", "[]"));
            }
        }

        return Objects.nonNull(schemaTransformations) ? schemaTransformations : ImmutableList.of();
    }
}
