/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.features.core.domain.FeatureTransformerBase;
import de.ii.ldproxy.ogcapi.tiles.app.TileFormatMVT;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import static de.ii.ldproxy.ogcapi.tiles.app.CapabilityVectorTiles.MAX_ABSOLUTE_AREA_CHANGE_IN_POLYGON_REPAIR;
import static de.ii.ldproxy.ogcapi.tiles.app.CapabilityVectorTiles.MAX_LINE_STRING_PER_TILE_DEFAULT;
import static de.ii.ldproxy.ogcapi.tiles.app.CapabilityVectorTiles.MAX_POINT_PER_TILE_DEFAULT;
import static de.ii.ldproxy.ogcapi.tiles.app.CapabilityVectorTiles.MAX_POLYGON_PER_TILE_DEFAULT;
import static de.ii.ldproxy.ogcapi.tiles.app.CapabilityVectorTiles.MAX_RELATIVE_AREA_CHANGE_IN_POLYGON_REPAIR;
import static de.ii.ldproxy.ogcapi.tiles.app.CapabilityVectorTiles.MINIMUM_SIZE_IN_PIXEL;

public class FeatureTransformerTilesMVT extends FeatureTransformerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerTilesMVT.class);
    static final String NULL = "__NULL__";

    private final OutputStream outputStream;

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
    private final double maxRelativeAreaChangeInPolygonRepair;
    private final double maxAbsoluteAreaChangeInPolygonRepair;
    private final double minimumSizeInPixel;
    private final String layerName;
    private final List<String> properties;
    private final boolean allProperties;
    private final PrecisionModel tilePrecisionModel;
    private final GeometryPrecisionReducer reducer;
    private final GeometryFactory geometryFactoryWorld;
    private final GeometryFactory geometryFactoryTile;
    private final Pattern separatorPattern;
    private final List<String> groupBy;

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
    private final StringBuilder currentValueBuilder = new StringBuilder();
    private FeatureProperty currentProperty = null;
    private String currentPropertyName = null;
    private final int polygonCount = 0;
    private final int lineStringCount = 0;
    private final int pointCount = 0;
    private final Polygon clipGeometry;
    private final Set<MvtFeature> mergeFeatures;

    private long mergeCount = 0;

    // TODO The class is getting too complex, factor out separate concerns.
    //      See https://github.com/interactive-instruments/ldproxy/issues/313.
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
        this.allProperties = properties.contains("*");
        this.separatorPattern = Pattern.compile("\\s+|,");
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

        this.polygonLimit = Objects.nonNull(tilesConfiguration) && Objects.nonNull(tilesConfiguration.getMaxPolygonPerTileDefaultDerived()) ?
                tilesConfiguration.getMaxPolygonPerTileDefaultDerived() : MAX_POLYGON_PER_TILE_DEFAULT;
        this.lineStringLimit = Objects.nonNull(tilesConfiguration) && Objects.nonNull(tilesConfiguration.getMaxLineStringPerTileDefaultDerived()) ?
                tilesConfiguration.getMaxLineStringPerTileDefaultDerived() : MAX_LINE_STRING_PER_TILE_DEFAULT;
        this.pointLimit = Objects.nonNull(tilesConfiguration) && Objects.nonNull(tilesConfiguration.getMaxPointPerTileDefaultDerived()) ?
                tilesConfiguration.getMaxPointPerTileDefaultDerived() : MAX_POINT_PER_TILE_DEFAULT;
        this.maxRelativeAreaChangeInPolygonRepair = Objects.nonNull(tilesConfiguration) && Objects.nonNull(tilesConfiguration.getMaxRelativeAreaChangeInPolygonRepairDerived()) ?
                tilesConfiguration.getMaxRelativeAreaChangeInPolygonRepairDerived() : MAX_RELATIVE_AREA_CHANGE_IN_POLYGON_REPAIR;
        this.maxAbsoluteAreaChangeInPolygonRepair = Objects.nonNull(tilesConfiguration) && Objects.nonNull(tilesConfiguration.getMaxAbsoluteAreaChangeInPolygonRepairDerived()) ?
                tilesConfiguration.getMaxAbsoluteAreaChangeInPolygonRepairDerived() : MAX_ABSOLUTE_AREA_CHANGE_IN_POLYGON_REPAIR;
        this.minimumSizeInPixel = Objects.nonNull(tilesConfiguration) && Objects.nonNull(tilesConfiguration.getMinimumSizeInPixelDerived()) ?
                tilesConfiguration.getMinimumSizeInPixelDerived() : MINIMUM_SIZE_IN_PIXEL;

        final Map<String, List<Rule>> rules = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getRulesDerived() : ImmutableMap.of();
        this.groupBy = (Objects.nonNull(rules) && rules.containsKey(tileMatrixSet.getId())) ?
                rules.get(tileMatrixSet.getId()).stream()
                     .filter(rule -> rule.getMax()>=tile.getTileLevel() && rule.getMin()<=tile.getTileLevel() && rule.getMerge().orElse(false))
                     .map(Rule::getGroupBy)
                     .findAny()
                     .orElse(null) :
                null;
        this.mergeFeatures = new HashSet<>();

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

        LOGGER.trace("Start generating tile for collection {}, tile {}/{}/{}/{}.", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());

        if (numberReturned.isPresent()) {
            long returned = numberReturned.getAsLong();
            long matched = numberMatched.orElse(-1);
            if (numberMatched.isPresent())
                LOGGER.trace("numberMatched {}", matched);
            LOGGER.trace("numberReturned {}", returned);
        }
    }

    @Override
    public void onEnd() {

        if (Objects.nonNull(groupBy) && mergeCount >0) {
            FeatureMerger merger = new FeatureMerger(groupBy, allProperties, properties, geometryFactoryTile, tilePrecisionModel, String.format("Collection %s, tile %s/%d/%d/%d", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol()));
            merger.merge(mergeFeatures)
                  .forEach(mergedFeature -> {
                      Geometry geom = mergedFeature.getGeometry();
                      // Geometry is invalid? -> log this information and skip it, if that option is used
                      if (!geom.isValid()) {
                          LOGGER.info("A merged feature in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Properties: {}", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), mergedFeature.getProperties());
                          if (Objects.nonNull(tilesConfiguration) &&
                              Objects.requireNonNullElse(tilesConfiguration.getIgnoreInvalidGeometriesDerived(),false))
                              return;
                      }
                      encoder.addFeature(layerName, mergedFeature.getProperties(), geom);
            });
        }

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

        LOGGER.trace("Tile response written.");
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
    public void onFeatureEnd() {

        if (currentGeometry==null) {
            resetFeatureInfo();
            return;
        }

        try {
            Geometry tileGeometry = TileGeometryUtil.getTileGeometry(currentGeometry, affineTransformation, clipGeometry, reducer, tilePrecisionModel, minimumSizeInPixel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
            if (Objects.isNull(tileGeometry)) {
                resetFeatureInfo();
                return;
            }

            // if polygons have to be merged, store them for now and process at the end
            if (Objects.nonNull(groupBy) && tileGeometry.getGeometryType().contains("Polygon")) {
                mergeFeatures.add(new ImmutableMvtFeature.Builder()
                                          .id(++mergeCount)
                                          .properties(currentProperties)
                                          .geometry(tileGeometry)
                                          .build());
                resetFeatureInfo();
                return;
            }

            // Geometry is invalid -> log this information and skip it, if that option is used
            if (!tileGeometry.isValid()) {
                LOGGER.info("Feature {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Size in pixels: {}.", currentId, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), currentGeometry.getArea());
                if (Objects.nonNull(tilesConfiguration) && Objects.requireNonNullElse(tilesConfiguration.getIgnoreInvalidGeometriesDerived(), false)) {
                    resetFeatureInfo();
                    return;
                }
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
                encoder.addFeature(layerName, currentProperties, tileGeometry, id);
            else
                encoder.addFeature(layerName, currentProperties, tileGeometry);

        } catch (Exception e) {
            LOGGER.error("Error while processing feature {} in tile {}/{}/{}/{} in collection {}. The feature is skipped.", currentId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), collectionId);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", e);
            }
        }

        resetFeatureInfo();
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

        if (!allProperties && !(properties.contains(currentPropertyName) ||
                                properties.contains(currentPropertyName.replace("[]", "")))) {
            currentProperty = null;
        }

        if (Objects.nonNull(currentProperty))
            multiplicities.forEach(index -> currentPropertyName = currentPropertyName.replaceFirst("\\[[^]]*]", "."+index));
    }

    @Override
    public void onPropertyText(String text) {
        if (Objects.nonNull(currentProperty))
            currentValueBuilder.append(text);
    }

    @Override
    public void onPropertyEnd() {
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
                        currentProperties.put(currentPropertyName, value.equalsIgnoreCase("t") || value.equalsIgnoreCase("true") || value.equals("1"));
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
}
