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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FeatureTransformerTilesMVT extends FeatureTransformerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerTilesMVT.class);
    static final String NULL = "__NULL__";

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
    private final List<String> groupByAttributes;

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
    private long mergeId = 0;
    private Map<Long, SortedMap<String, Object>> mergeProperties;
    private Map<Long, Geometry> mergeGeometries;

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

        final Map<String, List<Postprocessing>> postprocessing = tilesConfiguration.getPostprocessing();
        this.groupByAttributes = (Objects.nonNull(postprocessing) && postprocessing.containsKey(tileMatrixSet.getId())) ?
                postprocessing.get(tileMatrixSet.getId()).stream()
                             .filter(proc -> proc.getMax()>=tile.getTileLevel() && proc.getMin()<=tile.getTileLevel() && proc.getMergeFeatures().orElse(false))
                             .map(proc -> proc.getGroupByWhenMerging())
                             .findAny()
                             .orElse(null) :
                null;
        this.mergeProperties = new HashMap<>();
        this.mergeGeometries = new HashMap<>();

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

    private void mergePolygons(List<Object> values) {
        // merge all polygons with the values
        MultiPolygon multiPolygon = geometryFactoryTile.createMultiPolygon(mergeGeometries.entrySet()
                                                                                          .stream()
                                                                                          .filter(entry -> entry.getValue() instanceof Polygon || entry.getValue() instanceof MultiPolygon)
                                                                                          .filter(entry -> {
                                                                                              int i = 0;
                                                                                              boolean match = true;
                                                                                              for (String att : groupByAttributes) {
                                                                                                  if (values.get(i).equals(NULL) && !mergeProperties.get(entry.getKey()).containsKey(att)) {
                                                                                                      match = false;
                                                                                                      break;
                                                                                                  } else if (!values.get(i).equals(mergeProperties.get(entry.getKey()).get(att))) {
                                                                                                      match = false;
                                                                                                      break;
                                                                                                  }
                                                                                                  i++;
                                                                                              }
                                                                                              return match;
                                                                                          })
                                                                                          .map(entry -> entry.getValue())
                                                                                          .map(g -> g instanceof MultiPolygon ? TileGeometryUtil.splitMultiPolygon((MultiPolygon) g) : ImmutableList.of(g))
                                                                                          .flatMap(Collection::stream)
                                                                                          .map(g -> (Polygon) g)
                                                                                          .toArray(Polygon[]::new));
        if (multiPolygon.getNumGeometries()==0)
            return;

        LOGGER.trace("collection {}, tile {}/{}/{}/{} grouped by {}: {} polygons", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), values, multiPolygon.getNumGeometries());
        Geometry geom = TileGeometryUtil.processPolygons(multiPolygon, reducer);
        // now follow the same steps as for feature geometries
        if (Objects.nonNull(geom)) {
            // reduce the geometry to the tile grid
            geom = reducer.reduce(geom);
            if (Objects.nonNull(geom)) {
                // finally again remove any small rings or line strings created in the processing
                geom = TileGeometryUtil.removeSmallPieces(geom);
                if (!geom.isValid()) {
                    LOGGER.trace("Second attempt.");
                    geom = TileGeometryUtil.processPolygons(geom, reducer);
                    // now follow the same steps as for feature geometries
                    if (Objects.nonNull(geom)) {
                        // reduce the geometry to the tile grid
                        geom = reducer.reduce(geom);
                        if (Objects.nonNull(geom)) {
                            // finally again remove any small rings or line strings created in the processing
                            geom = TileGeometryUtil.removeSmallPieces(geom);
                        }
                    }
                }
            }
        }

        ImmutableMap.Builder<String, Object> propertiesBuilder = ImmutableMap.builder();
        int i = 0;
        for (String att : groupByAttributes) {
            propertiesBuilder.put(att,values.get(i++));
        }

        if (Objects.isNull(geom) || geom.isEmpty() || geom.getNumGeometries()==0) {
            LOGGER.trace("Merged polygon feature grouped by {} in collection {} has no geometry in tile {}/{}/{}/{}.", values, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
            return;
        }

        // Geometry is invalid -> log this information and skip it, if that option is used
        if (!geom.isValid()) {
            LOGGER.info("Merged polygon feature grouped by {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}.", values, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
            if (!tilesConfiguration.getIgnoreInvalidGeometries()) {
                encoder.addFeature(layerName, propertiesBuilder.build(), geom);
            }
        } else
            encoder.addFeature(layerName, propertiesBuilder.build(), geom);
    }

    private void mergeLineStrings(List<Object> values) {
        // merge all line strings with the values
        List<LineString> lineStrings = mergeGeometries.entrySet()
                                                      .stream()
                                                      .filter(entry -> entry.getValue() instanceof LineString || entry.getValue() instanceof MultiLineString)
                                                      .filter(entry -> {
                                                          int i = 0;
                                                          boolean match = true;
                                                          for (String att : groupByAttributes) {
                                                              if (values.get(i).equals(NULL) && !mergeProperties.get(entry.getKey()).containsKey(att)) {
                                                                  match = false;
                                                                  break;
                                                              } else if (!values.get(i).equals(mergeProperties.get(entry.getKey()).get(att))) {
                                                                  match = false;
                                                                  break;
                                                              }
                                                          }
                                                          return match;
                                                      })
                                                      .map(entry -> entry.getValue())
                                                      .map(g -> g instanceof MultiLineString ? TileGeometryUtil.splitMultiLineString((MultiLineString) g) : ImmutableList.of(g))
                                                      .flatMap(Collection::stream)
                                                      .map(g -> (LineString) g)
                                                      .collect(Collectors.toList());
        if (lineStrings.isEmpty())
            return;

        LOGGER.trace("collection {}, tile {}/{}/{}/{} grouped by {}: {} line strings", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), values, lineStrings.size());
        Geometry geom = TileGeometryUtil.processLineStrings(lineStrings, reducer, 1.0/tilePrecisionModel.getScale());

        ImmutableMap.Builder<String, Object> propertiesBuilder = ImmutableMap.builder();
        int i = 0;
        for (String att : groupByAttributes) {
            propertiesBuilder.put(att,values.get(i++));
        }

        if (Objects.isNull(geom) || geom.getNumGeometries()==0) {
            LOGGER.trace("Merged line string feature grouped by {} in collection {} has no geometry in tile {}/{}/{}/{}.", values, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
            return;
        }

        // Geometry is invalid -> log this information and skip it, if that option is used
        if (!geom.isValid()) {
            LOGGER.info("Merged line string feature grouped by {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}.", values, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
            if (!tilesConfiguration.getIgnoreInvalidGeometries()) {
                encoder.addFeature(layerName, propertiesBuilder.build(), geom);
            }
        } else
            encoder.addFeature(layerName, propertiesBuilder.build(), geom);
    }

    @Override
    public void onEnd() {

        if (Objects.nonNull(groupByAttributes) && mergeId>0) {
            ImmutableList<ImmutableList<Object>> groups = groupByAttributes.stream()
                                                                           .map(att -> mergeProperties.values()
                                                                                                      .stream()
                                                                                                      .map(props -> props.containsKey(att) ? props.get(att) : NULL)
                                                                                                      .distinct()
                                                                                                      .collect(ImmutableList.toImmutableList()))
                                                                           .collect(ImmutableList.toImmutableList());

            if (mergeGeometries.values().stream().anyMatch(geom -> geom instanceof Polygon || geom instanceof MultiPolygon))
                Lists.cartesianProduct(groups)
                     .stream()
                     .forEach(values -> mergePolygons(values));

            if (mergeGeometries.values().stream().anyMatch(geom -> geom instanceof LineString || geom instanceof MultiLineString))
                Lists.cartesianProduct(groups)
                     .stream()
                     .forEach(values -> mergeLineStrings(values));

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

        LOGGER.trace("Response written.");
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

    private Geometry prepareTileGeometry(Geometry geom) {

        // The following changes are applied:
        // 1. The coordinates are converted to the tile coordinate system (0/0 is top left, 256/256 is bottom right)
        // 2. Small rings or line strings are dropped (small in the context of the tile, one pixel or less). The idea
        //    is to simply drop them as early as possible and before the next processing steps which may depend on
        //    having valid geometries and removing everything that will eventually be removed anyway helps.
        // 3. Remove unnecessary vertices and snap coordinates to the grid.
        // 4. If the resulting geometry is invalid polygonal geometry, try to make it valid.
        // 5. Hopefully we have a valid geometry now, so try to clip it to the tile.
        //
        // After each step, check, if we still have a geometry or the resulting tile geometry was too small for
        // the tile. In that case the feature is ignored.

        // convert to the tile coordinate system
        geom.apply(affineTransformation);

        // remove small rings or line strings (small in the context of the tile)
        geom = TileGeometryUtil.removeSmallPieces(geom);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // simplify the geometry
        geom = TopologyPreservingSimplifier.simplify(geom, 1.0/tilePrecisionModel.getScale());
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // reduce the geometry to the tile grid
        geom = reducer.reduce(geom);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // if the resulting geometry is invalid, try to make it valid
        if (!geom.isValid()) {
            geom = TileGeometryUtil.processPolygons(geom, reducer);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;
        }

        // limit the coordinates to the tile with a buffer
        geom = TileGeometryUtil.clipGeometry(geom, clipGeometry);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // finally again remove any small rings or line strings created in the processing
        geom = TileGeometryUtil.removeSmallPieces(geom);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        if (!geom.isValid()) {
            // try once more
            LOGGER.trace("Second attempt.");

            geom = TileGeometryUtil.processPolygons(geom, reducer);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // limit the coordinates to the tile with a buffer
            geom = TileGeometryUtil.clipGeometry(geom, clipGeometry);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // reduce the geometry to the tile grid
            geom = reducer.reduce(geom);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // finally again remove any small rings or line strings created in the processing
            geom = TileGeometryUtil.removeSmallPieces(geom);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;
        }

        return geom;
    }

    @Override
    public void onFeatureEnd() {

        if (currentGeometry==null) {
            resetFeatureInfo();
            return;
        }

        Geometry tileGeometry = prepareTileGeometry(currentGeometry);
        if (Objects.isNull(tileGeometry)) {
            resetFeatureInfo();
            return;
        }

        // if polygons have to be merged, store them for now and process at the end
        if (Objects.nonNull(groupByAttributes) && tileGeometry.getGeometryType().contains("Polygon")) {
            mergeGeometries.put(++mergeId, tileGeometry);
            mergeProperties.put(mergeId, currentProperties);
            resetFeatureInfo();
            return;
        }

        // Geometry is still invalid -> log this information and skip it, if that option is used
        if (!tileGeometry.isValid()) {
            LOGGER.info("Feature {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Pixel size: {}.", currentId, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), currentGeometry.getArea());
            if (tilesConfiguration.getIgnoreInvalidGeometries()) {
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
