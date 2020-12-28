/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.ii.ldproxy.ogcapi.tiles.domain.Rule;
import de.ii.ldproxy.ogcapi.tiles.domain.Tile;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.streams.domain.HttpClient;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

public class FeatureTransformerTilesMVT extends FeatureTransformerSimpleFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTransformerTilesMVT.class);
    static final String NULL = "__NULL__";

    private final int limit;
    private final Tile tile;
    private final TileMatrixSet tileMatrixSet;
    private final FeatureTransformationContextTiles transformationContext;
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
    private final PrecisionModel tilePrecisionModel;
    private final GeometryPrecisionReducer reducer;
    private final GeometryFactory geometryFactoryTile;
    private final List<String> groupBy;

    private int polygonCount = 0;
    private int lineStringCount = 0;
    private int pointCount = 0;
    private final Polygon clipGeometry;
    private long mergeCount = 0;
    private Set<MvtFeature> mergeFeatures;

    // TODO The class is getting too complex, factor out separate concerns.
    //      See https://github.com/interactive-instruments/ldproxy/issues/313.
    public FeatureTransformerTilesMVT(FeatureTransformationContextTiles transformationContext, HttpClient httpClient) {
        super(TilesConfiguration.class,
              transformationContext.getApiData(), transformationContext.getCollectionId(),
              transformationContext.getCodelists(), transformationContext.getServiceUrl(),
              transformationContext.isFeatureCollection(), transformationContext.getOutputStream(),
              transformationContext.getCrsTransformer().orElse(null), transformationContext.shouldSwapCoordinates(),
              transformationContext.getFields(), transformationContext.getProcessingParameters());
        this.limit = transformationContext.getLimit();
        this.transformationContext = transformationContext;
        this.tile = transformationContext.getTile();
        this.tileMatrixSet = tile.getTileMatrixSet();
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

        this.polygonLimit = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getMaxPolygonPerTileDefault() : 10000;
        this.lineStringLimit = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getMaxLineStringPerTileDefault() : 10000;
        this.pointLimit = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getMaxPointPerTileDefault() : 10000;
        this.maxRelativeAreaChangeInPolygonRepair = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getMaxRelativeAreaChangeInPolygonRepair() : 0.1;
        this.maxAbsoluteAreaChangeInPolygonRepair = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getMaxAbsoluteAreaChangeInPolygonRepair() : 1.0;
        this.minimumSizeInPixel = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getMinimumSizeInPixel() : 1.0;

        final Map<String, List<Rule>> rules = Objects.nonNull(tilesConfiguration) ? tilesConfiguration.getRules() : ImmutableMap.of();
        this.groupBy = (Objects.nonNull(rules) && rules.containsKey(tileMatrixSet.getId())) ?
                rules.get(tileMatrixSet.getId()).stream()
                     .filter(rule -> rule.getMax()>=tile.getTileLevel() && rule.getMin()<=tile.getTileLevel() && rule.getMerge().orElse(false))
                     .map(rule -> rule.getGroupBy())
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

    private void addFeature(MvtFeature feature) {
        Geometry geom = feature.getGeometry();
        // Geometry is invalid? -> log this information and skip it, if that option is used
        if (!geom.isValid()) {
            LOGGER.info("A merged feature in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Properties: {}", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), feature.getProperties());
            if (Objects.nonNull(tilesConfiguration) && tilesConfiguration.getIgnoreInvalidGeometries())
                return;
        }
        encoder.addFeature(layerName, feature.getProperties(), geom);
    }

    private class ClusterAnalysis {
        Multimap<MvtFeature, MvtFeature> clusters = ArrayListMultimap.create();
        Map<MvtFeature, MvtFeature> inCluster = new HashMap<>();
        Set<MvtFeature> standalone = new HashSet<>();
    }

    private ClusterAnalysis clusterAnalysis(List<MvtFeature> features, boolean boundary) {
        // determine clusters of connected features
        ClusterAnalysis clusterResult = new ClusterAnalysis();
        for (int i = 0; i < features.size(); i++) {
            MvtFeature fi = features.get(i);
            Geometry gi = fi.getGeometry();
            Optional<MvtFeature> cluster = Optional.ofNullable(clusterResult.inCluster.get(fi));
            for (int j = i+1; j < features.size(); j++) {
                MvtFeature fj = features.get(j);
                Geometry gj = fj.getGeometry();
                boolean clustered = boundary ? gi.getBoundary().intersects(gj.getBoundary()) : gi.intersects(gj);
                if (clustered) {
                    if (cluster.isPresent()) {
                        // already in a cluster, add to the new feature to the cluster
                        clusterResult.clusters.put(cluster.get(),fj);
                        clusterResult.inCluster.put(fj,cluster.get());
                    } else {
                        // new cluster
                        clusterResult.clusters.put(fi,fj);
                        clusterResult.inCluster.put(fj,fi);
                    }
                }
            }
            // if the feature wasn't already in a cluster and hasn't started a new cluster, it is standalone
            if (!cluster.isPresent() && !clusterResult.clusters.containsKey(fi)) {
                clusterResult.standalone.add(fi);
            }
        }
        return clusterResult;
    }

    private Geometry repairPolygon(Geometry geom) {
        geom = TileGeometryUtil.repairPolygon(geom, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair, 1.0/tilePrecisionModel.getScale());
        // now follow the same steps as for feature geometries
        if (Objects.nonNull(geom)) {
            // reduce the geometry to the tile grid
            geom = TileGeometryUtil.reduce(geom, reducer, tilePrecisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
            if (Objects.nonNull(geom)) {
                // finally again remove any small rings or line strings created in the processing
                geom = TileGeometryUtil.removeSmallPieces(geom, minimumSizeInPixel);
                if (Objects.nonNull(geom) && !geom.isValid()) {
                    LOGGER.trace("Merged polygonal geometry invalid after initial processing. Another attempt to repair.");
                    geom = TileGeometryUtil.repairPolygon(geom, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair, 1.0 / tilePrecisionModel.getScale());
                    // now follow the same steps as for feature geometries
                    if (Objects.nonNull(geom)) {
                        // reduce the geometry to the tile grid
                        geom = TileGeometryUtil.reduce(geom, reducer, tilePrecisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
                        if (Objects.nonNull(geom)) {
                            // finally again remove any small rings or line strings created in the processing
                            geom = TileGeometryUtil.removeSmallPieces(geom, minimumSizeInPixel);
                        }
                    }
                }
            }
        }
        return geom;
    }

    // merge all polygons with the values for the groupBy attributes
    private List<MvtFeature> mergePolygons(List<Object> values) {
        ImmutableList.Builder<MvtFeature> result = ImmutableList.builder();

        // identify features that match the values
        List<MvtFeature> features = mergeFeatures
                                            .stream()
                                            .filter(feature -> feature.getGeometry() instanceof Polygon || feature.getGeometry() instanceof MultiPolygon)
                                            .filter(feature -> {
                                                int i = 0;
                                                boolean match = true;
                                                for (String att : groupBy) {
                                                    if (values.get(i).equals(NULL)) {
                                                        if (feature.getProperties().containsKey(att)) {
                                                            match = false;
                                                            break;
                                                        }
                                                    } else if (!values.get(i).equals(feature.getProperties().get(att))) {
                                                        match = false;
                                                        break;
                                                    }
                                                    i++;
                                                }
                                                return match;
                                            })
                                            .collect(Collectors.toUnmodifiableList());

        // nothing to merge?
        if (features.isEmpty()) {
            return result.build();
        } else if (features.size()==1) {
            return result.add(features.iterator().next()).build();
        }

        // determine clusters of connected features
        ClusterAnalysis clusterResult = clusterAnalysis(features, false);

        // process the standalone features
        clusterResult.standalone
                .stream()
                .forEach(feature -> result.add(feature));

        // process each cluster
        clusterResult.clusters
                .asMap()
                .entrySet()
                .stream()
                .forEach(entry -> {
                    ImmutableSet.Builder<Polygon> polygonBuilder = new ImmutableSet.Builder<>();
                    polygonBuilder.addAll(entry.getValue().stream()
                                               .map(f -> f.getGeometry())
                                               .map(g -> g instanceof MultiPolygon ? TileGeometryUtil.splitMultiPolygon((MultiPolygon) g) : ImmutableList.of(g))
                                               .flatMap(Collection::stream)
                                               .map(Polygon.class::cast)
                                               .collect(Collectors.toSet()));
                    if (entry.getKey().getGeometry() instanceof MultiPolygon)
                        polygonBuilder.addAll(TileGeometryUtil.splitMultiPolygon((MultiPolygon) entry.getKey().getGeometry()));
                    else
                        polygonBuilder.add((Polygon) entry.getKey().getGeometry());
                    ImmutableSet<Polygon> polygons = polygonBuilder.build();
                    Geometry geom;
                    switch(polygons.size()){
                        case 0:
                            return;
                        case 1:
                            geom = polygons.iterator().next();
                            break;
                        default:
                            try {
                                Iterator<Polygon> iter = polygons.iterator();
                                geom = iter.next();
                                while(iter.hasNext()){
                                    geom = geom.symDifference(iter.next());
                                }
                            } catch (Exception e) {
                                geom = geometryFactoryTile.createMultiPolygon(polygons.toArray(Polygon[]::new));
                            }
                    }
                    LOGGER.trace("collection {}, tile {}/{}/{}/{} grouped by {}: {} polygons", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), values, geom.getNumGeometries());
                    if (!geom.isValid()) {
                        geom = repairPolygon(geom);
                    }

                    if (Objects.isNull(geom) || geom.isEmpty() || geom.getNumGeometries()==0 || !geom.isValid()) {
                        LOGGER.trace("Merged polygon feature grouped by {} in collection {} has no or an invalid geometry in tile {}/{}/{}/{}. Use unmerged features.", values, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
                        result.add(entry.getKey());
                        entry.getValue().stream().forEach(feature -> result.add(feature));

                    } else {
                        // add merged feature
                        Map<String, Object> mainClusterFeatureProperties = entry.getKey().getProperties();
                        ImmutableMap.Builder<String, Object> propertiesBuilder = ImmutableMap.builder();
                        mainClusterFeatureProperties.entrySet()
                                                    .stream()
                                                    .filter(prop -> allProperties || properties.contains(prop.getKey()))
                                                    .filter(prop -> entry.getValue()
                                                                         .stream()
                                                                         .allMatch(f -> f.getProperties().containsKey(prop.getKey()) && f.getProperties().get(prop.getKey()).equals(prop.getValue())))
                                                    .forEach(prop -> propertiesBuilder.put(prop.getKey(), prop.getValue()));

                        result.add(new ImmutableMvtFeature.Builder()
                                           .id(entry.getKey().getId())
                                           .properties(propertiesBuilder.build())
                                           .geometry(geom).build());
                    }

        });

        return result.build();
    }

    // merge all polygons with the values for the groupBy attributes
    private List<MvtFeature> mergeLineStrings(List<Object> values) {
        ImmutableList.Builder<MvtFeature> result = ImmutableList.builder();

        // identify features that match the values
        List<MvtFeature> features = mergeFeatures
                .stream()
                .filter(feature -> feature.getGeometry() instanceof LineString || feature.getGeometry() instanceof MultiLineString)
                .filter(feature -> {
                    int i = 0;
                    boolean match = true;
                    for (String att : groupBy) {
                        if (values.get(i).equals(NULL)) {
                            if (feature.getProperties().containsKey(att)) {
                                match = false;
                                break;
                            }
                        } else if (!values.get(i).equals(feature.getProperties().get(att))) {
                            match = false;
                            break;
                        }
                        i++;
                    }
                    return match;
                })
                .collect(Collectors.toUnmodifiableList());

        // nothing to merge?
        if (features.isEmpty()) {
            return result.build();
        } else if (features.size()==1) {
            return result.add(features.iterator().next()).build();
        }

        // determine clusters of connected features
        ClusterAnalysis clusterResult = clusterAnalysis(features, true);

        // process the standalone features
        clusterResult.standalone
                .stream()
                .forEach(feature -> result.add(feature));

        // process each cluster
        clusterResult.clusters
                .asMap()
                .entrySet()
                .stream()
                .forEach(entry -> {
                    ImmutableSet.Builder<LineString> lineStringBuilder = new ImmutableSet.Builder<>();
                    lineStringBuilder.addAll(entry.getValue().stream()
                                               .map(f -> f.getGeometry())
                                               .map(g -> g instanceof MultiLineString ? TileGeometryUtil.splitMultiLineString((MultiLineString) g) : ImmutableList.of(g))
                                               .flatMap(Collection::stream)
                                               .map(LineString.class::cast)
                                               .collect(Collectors.toSet()));
                    if (entry.getKey().getGeometry() instanceof MultiLineString)
                        lineStringBuilder.addAll(TileGeometryUtil.splitMultiLineString((MultiLineString) entry.getKey().getGeometry()));
                    else
                        lineStringBuilder.add((LineString) entry.getKey().getGeometry());
                    ImmutableSet<LineString> lineStrings = lineStringBuilder.build();
                    Geometry geom;
                    switch(lineStrings.size()){
                        case 0:
                            return;
                        case 1:
                            geom = lineStrings.iterator().next();
                            break;
                        default:
                            try {
                                LineMerger lineMerger = new LineMerger();
                                lineMerger.add(lineStrings);
                                geom = geometryFactoryTile.createMultiLineString(((Collection<LineString>) lineMerger.getMergedLineStrings()).toArray(LineString[]::new));
                            } catch (Exception e) {
                                geom = geometryFactoryTile.createMultiLineString(lineStrings.toArray(LineString[]::new));
                            }
                    }
                    LOGGER.trace("collection {}, tile {}/{}/{}/{} grouped by {}: {} line strings", collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), values, geom.getNumGeometries());
                    if (geom.isEmpty() || geom.getNumGeometries()==0 || !geom.isValid()) {
                        LOGGER.trace("Merged line string feature grouped by {} in collection {} has no or an invalid geometry in tile {}/{}/{}/{}. Use unmerged features.", values, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol());
                        result.add(entry.getKey());
                        entry.getValue().stream().forEach(feature -> result.add(feature));

                    } else {
                        // add merged feature
                        Map<String, Object> mainClusterFeatureProperties = entry.getKey().getProperties();
                        ImmutableMap.Builder<String, Object> propertiesBuilder = ImmutableMap.builder();
                        mainClusterFeatureProperties.entrySet()
                                                    .stream()
                                                    .filter(prop -> allProperties || properties.contains(prop.getKey()))
                                                    .filter(prop -> entry.getValue()
                                                                         .stream()
                                                                         .allMatch(f -> f.getProperties().containsKey(prop.getKey()) && f.getProperties().get(prop.getKey()).equals(prop.getValue())))
                                                    .forEach(prop -> propertiesBuilder.put(prop.getKey(), prop.getValue()));

                        result.add(new ImmutableMvtFeature.Builder()
                                           .id(entry.getKey().getId())
                                           .properties(propertiesBuilder.build())
                                           .geometry(geom).build());
                    }

                });

        return result.build();
    }

    @Override
    public void onEnd() {

        if (Objects.nonNull(groupBy) && mergeCount >0) {
            ImmutableList<ImmutableList<Object>> valueGroups = groupBy.stream()
                                                                 .map(att -> mergeFeatures.stream()
                                                                                          .map(props -> props.getProperties().containsKey(att) ? props.getProperties().get(att) : NULL)
                                                                                          .distinct()
                                                                                          .collect(ImmutableList.toImmutableList()))
                                                                 .collect(ImmutableList.toImmutableList());

            if (mergeFeatures.stream().anyMatch(feature -> feature.getGeometry() instanceof Polygon || feature.getGeometry() instanceof MultiPolygon)) {
                List<MvtFeature> features = new ArrayList<>();
                Lists.cartesianProduct(valueGroups)
                     .stream()
                     .forEach(values -> {
                         try {
                             features.addAll(mergePolygons(values));
                         } catch (Exception e) {
                             LOGGER.error("Error while merging polygon geometries grouped by {} in tile {}/{}/{}/{} in collection {}. The features are skipped.", values, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), collectionId);
                             if (LOGGER.isDebugEnabled()) {
                                 LOGGER.debug("Stacktrace:", e);
                             }
                         }
                     });
                LOGGER.trace("{} merged polygon features in tile {}/{}/{}/{} in collection {}, total pixel area: {}.",
                            features.size(), tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), collectionId, features.stream()
                                                                                                                                                     .mapToDouble(f -> f.getGeometry().getArea())
                                                                                                                                                     .sum());
                features.stream()
                        .forEach(feature -> addFeature(feature));
            }

            if (mergeFeatures.stream().anyMatch(feature -> feature.getGeometry() instanceof LineString || feature.getGeometry() instanceof MultiLineString)) {
                List<MvtFeature> features = new ArrayList<>();
                Lists.cartesianProduct(valueGroups)
                     .stream()
                     .forEach(values -> {
                         try {
                             features.addAll(mergeLineStrings(values));
                         } catch (Exception e) {
                             LOGGER.error("Error while merging line string geometries grouped by {} in tile {}/{}/{}/{} in collection {}. The features are skipped.", values, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), collectionId);
                             if (LOGGER.isDebugEnabled()) {
                                 LOGGER.debug("Stacktrace:", e);
                             }
                         }
                     });
                LOGGER.trace("{} merged line string features in tile {}/{}/{}/{} in collection {}, total pixel length: {}.",
                             features.size(), tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), collectionId, features.stream()
                                                                                                                                                      .mapToDouble(f -> f.getGeometry().getLength())
                                                                                                                                                      .sum());
                features.stream()
                        .forEach(feature -> addFeature(feature));
            }
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
        geom = TileGeometryUtil.removeSmallPieces(geom, minimumSizeInPixel);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // simplify the geometry
        geom = TopologyPreservingSimplifier.simplify(geom, 1.0/tilePrecisionModel.getScale());
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // reduce the geometry to the tile grid
        geom = TileGeometryUtil.reduce(geom, reducer, tilePrecisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // if the resulting geometry is invalid, try to make it valid
        if (!geom.isValid()) {
            geom = TileGeometryUtil.repairPolygon(geom, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair, 1.0 / tilePrecisionModel.getScale());
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;
        }

        // limit the coordinates to the tile with a buffer
        geom = TileGeometryUtil.clipGeometry(geom, clipGeometry);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // reduce the geometry to the tile grid
        geom = TileGeometryUtil.reduce(geom, reducer, tilePrecisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        // finally again remove any small rings or line strings created in the processing
        geom = TileGeometryUtil.removeSmallPieces(geom, minimumSizeInPixel);
        if (Objects.isNull(geom) || geom.isEmpty())
            return null;

        if (!geom.isValid()) {
            // try once more
            LOGGER.trace("Polygonal geometry invalid after initial processing. Final attempt to repair.");

            geom = TileGeometryUtil.repairPolygon(geom, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair, 1.0 / tilePrecisionModel.getScale());
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // limit the coordinates to the tile with a buffer
            geom = TileGeometryUtil.clipGeometry(geom, clipGeometry);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // reduce the geometry to the tile grid
            geom = TileGeometryUtil.reduce(geom, reducer, tilePrecisionModel, maxRelativeAreaChangeInPolygonRepair, maxAbsoluteAreaChangeInPolygonRepair);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;

            // finally again remove any small rings or line strings created in the processing
            geom = TileGeometryUtil.removeSmallPieces(geom, minimumSizeInPixel);
            if (Objects.isNull(geom) || geom.isEmpty())
                return null;
        }

        return geom;
    }

    @Override
    public void onFeatureEnd() {

        if (currentGeometry==null) {
            return;
        }

        try {
            Geometry tileGeometry = prepareTileGeometry(currentGeometry);
            if (Objects.isNull(tileGeometry)) {
                return;
            }

            // if polygons have to be merged, store them for now and process at the end
            if (Objects.nonNull(groupBy) && tileGeometry.getGeometryType().contains("Polygon")) {
                mergeFeatures.add(new ImmutableMvtFeature.Builder()
                                          .id(++mergeCount)
                                          .properties(currentProperties)
                                          .geometry(tileGeometry)
                                          .build());
                return;
            }

            // Geometry is still invalid -> log this information and skip it, if that option is used
            if (!tileGeometry.isValid()) {
                LOGGER.info("Feature {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Size in pixels: {}.", currentId, collectionId, tileMatrixSet.getId(), tile.getTileLevel(), tile.getTileRow(), tile.getTileCol(), currentGeometry.getArea());
                if (Objects.nonNull(tilesConfiguration) && tilesConfiguration.getIgnoreInvalidGeometries()) {
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
    }
}
