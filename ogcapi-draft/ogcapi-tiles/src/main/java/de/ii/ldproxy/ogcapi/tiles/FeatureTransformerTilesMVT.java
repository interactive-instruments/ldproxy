/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.base.Splitter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyValueTransformer;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.streams.domain.HttpClient;
import no.ecc.vectortile.VectorTileEncoder;
import org.apache.commons.lang3.ArrayUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

public class FeatureTransformerTilesMVT extends FeatureTransformerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerTilesMVT.class);

    private OutputStream outputStream;

    private final int limit;
    private final String collectionId;
    private final Tile tile;
    private final TileMatrixSet tileMatrixSet;
    private final CrsTransformer crsTransformer;
    private final boolean swapCoordinates;
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
    private final PrecisionModel tilePrecisionModel;
    private final GeometryPrecisionReducer reducer;
    private final GeometryFactory geometryFactoryWorld;
    private final GeometryFactory geometryFactoryTile;
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
    private final Polygon clipGeometry;

    public FeatureTransformerTilesMVT(FeatureTransformationContextTiles transformationContext, HttpClient httpClient) {
        super(TilesConfiguration.class,
              transformationContext.getApiData(), transformationContext.getCollectionId(),
              transformationContext.getCodelists(), transformationContext.getServiceUrl(),
              transformationContext.isFeatureCollection());
        this.outputStream = transformationContext.getOutputStream();
        this.limit = transformationContext.getLimit();
        this.crsTransformer = transformationContext.getCrsTransformer()
                                                   .orElse(null);
        this.swapCoordinates = transformationContext.shouldSwapCoordinates();
        this.transformationContext = transformationContext;
        this.processingParameters = transformationContext.getProcessingParameters();
        this.collectionId = transformationContext.getCollectionId();
        this.tile = transformationContext.getTile();
        this.tileMatrixSet = tile.getTileMatrixSet();
        this.properties = transformationContext.getFields();
        this.separatorPattern = Pattern.compile("\\s+|\\,");
        this.geometryFactoryWorld = new GeometryFactory();
        this.tilePrecisionModel = new PrecisionModel((double)tileMatrixSet.getTileExtent() / (double)tileMatrixSet.getTileSize());
        this.geometryFactoryTile = new GeometryFactory(tilePrecisionModel);
        this.reducer = new GeometryPrecisionReducer(tilePrecisionModel);

        if (collectionId!=null) {
            tilesConfiguration = transformationContext.getConfiguration();
            layerName = collectionId;
        } else {
            tilesConfiguration = transformationContext.getConfiguration();
            layerName = "layer";
        }

        this.encoder = new VectorTileEncoder(tileMatrixSet.getTileExtent());
        this.affineTransformation = tile.createTransformNativeToTile();

        this.polygonLimit = tilesConfiguration!=null ? tilesConfiguration.getMaxPolygonPerTileDefault() : 10000;
        this.lineStringLimit = tilesConfiguration!=null ? tilesConfiguration.getMaxLineStringPerTileDefault() : 10000;
        this.pointLimit = tilesConfiguration!=null ? tilesConfiguration.getMaxPointPerTileDefault() : 10000;

        final int size = tileMatrixSet.getTileSize();
        final int buffer = 8;
        CoordinateXY[] coords = new CoordinateXY[5];
        coords[0] = new CoordinateXY(0 - buffer, size + buffer);
        coords[1] = new CoordinateXY(size + buffer, size + buffer);
        coords[2] = new CoordinateXY(size + buffer, 0 - buffer);
        coords[3] = new CoordinateXY(0 - buffer, 0 - buffer);
        coords[4] = coords[0];
        this.clipGeometry = geometryFactoryTile.createPolygon(coords);
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

        try {
            byte[] mvt = encoder.encode();
            outputStream.write(mvt);
            outputStream.flush();

            // write/update tile in cache
            Path tileFile = transformationContext.getTileFile();
            if (Files.notExists(tileFile) || Files.isWritable(tileFile)) {
                Files.write(tileFile, mvt);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing output stream.", e);
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

        // convert to the tile coordinate system
        currentGeometry.apply(affineTransformation);

        // remove small rings or line strings (small in the context of the tile)
        currentGeometry = TileGeometryUtil.removeSmallPieces(currentGeometry);
        if (currentGeometry==null)
            return;

        // limit the coordinates to the tile with a buffer
        currentGeometry = TileGeometryUtil.clipGeometry(currentGeometry, clipGeometry);

        // reduce the geometry to the tile grid
        currentGeometry = reducer.reduce(currentGeometry);

        // if the resulting geometry is invalid, try to make it valid
        if (!currentGeometry.isValid()) {
            if (currentGeometry instanceof Polygon || currentGeometry instanceof MultiPolygon)
                currentGeometry = currentGeometry.buffer(0.0);

            // again, remove small rings or line strings (small in the context of the tile) that may
            // have been created in some cases by changing to the tile grid
            currentGeometry = TileGeometryUtil.removeSmallPieces(currentGeometry);
            if (currentGeometry==null)
                return;

            // again, make sure the geometry is using the tile grid
            currentGeometry = reducer.reduce(currentGeometry);
        }

        // Geometry is still invalid -> log this information and skip it, if that option is used
        if (!currentGeometry.isValid()) {
            LOGGER.info("Feature {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}.", currentId, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
            if (tilesConfiguration.getIgnoreInvalidGeometries())
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

        // the property may have been removed by the transformations
        if (Objects.isNull(processedFeatureProperty))
            return;

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
                    currentPolygons.add(geometryFactoryWorld.createPolygon(currentLinearRings.get(0)));
                else if (rings>1)
                    currentPolygons.add(geometryFactoryWorld.createPolygon(currentLinearRings.get(0), currentLinearRings.subList(1, rings).toArray(new LinearRing[0])));
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
}
