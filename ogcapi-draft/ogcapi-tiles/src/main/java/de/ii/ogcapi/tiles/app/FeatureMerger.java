/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.ii.ogcapi.tiles.domain.ImmutableMvtFeature.Builder;
import de.ii.ogcapi.tiles.domain.MvtFeature;
import de.ii.xtraplatform.base.domain.LogContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FeatureMerger {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMerger.class);

  static final String NULL = "__NULL__";

  private final List<String> groupBy;
  private final boolean allProperties;
  private final List<String> properties;
  private final GeometryFactory geometryFactory;
  private final PrecisionModel precisionModel;
  private final String context;

  FeatureMerger(
      List<String> groupBy,
      boolean allProperties,
      List<String> properties,
      GeometryFactory geometryFactory,
      PrecisionModel precisionModel,
      String context) {
    this.groupBy = groupBy;
    this.allProperties = allProperties;
    this.properties = properties;
    this.geometryFactory = geometryFactory;
    this.precisionModel = precisionModel;
    this.context = context;
  }

  List<MvtFeature> merge(Set<MvtFeature> mergeFeatures) {
    ImmutableList<ImmutableList<Object>> valueGroups =
        groupBy.stream()
            .map(
                att ->
                    mergeFeatures.stream()
                        .map(props -> props.getProperties().getOrDefault(att, NULL))
                        .distinct()
                        .collect(ImmutableList.toImmutableList()))
            .collect(ImmutableList.toImmutableList());

    List<MvtFeature> polygonFeatures = new ArrayList<>();
    List<MvtFeature> lineStringFeatures = new ArrayList<>();
    if (mergeFeatures.stream()
        .anyMatch(
            feature ->
                feature.getGeometry() instanceof Polygon
                    || feature.getGeometry() instanceof MultiPolygon)) {
      Lists.cartesianProduct(valueGroups)
          .forEach(
              values -> {
                try {
                  polygonFeatures.addAll(mergePolygons(mergeFeatures, values));
                } catch (Exception e) {
                  LOGGER.error(
                      "{}: Error while merging polygon geometries grouped by {}. The features are skipped.",
                      context,
                      values);
                  if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
                    LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
                  }
                }
              });
      LOGGER.trace(
          "{}: {} merged polygon features, total pixel area: {}.",
          context,
          polygonFeatures.size(),
          polygonFeatures.stream().mapToDouble(f -> f.getGeometry().getArea()).sum());
    }

    if (mergeFeatures.stream()
        .anyMatch(
            feature ->
                feature.getGeometry() instanceof LineString
                    || feature.getGeometry() instanceof MultiLineString)) {
      Lists.cartesianProduct(valueGroups)
          .forEach(
              values -> {
                try {
                  lineStringFeatures.addAll(mergeLineStrings(mergeFeatures, values));
                } catch (Exception e) {
                  LOGGER.error(
                      "{}: Error while merging line string geometries grouped by {}. The features are skipped.",
                      context,
                      values);
                  if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
                    LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
                  }
                }
              });
      LOGGER.trace(
          "{}: {} merged line string features, total pixel length: {}.",
          context,
          lineStringFeatures.size(),
          lineStringFeatures.stream().mapToDouble(f -> f.getGeometry().getLength()).sum());
    }

    polygonFeatures.addAll(lineStringFeatures);
    return polygonFeatures;
  }

  private List<MvtFeature> mergePolygons(Set<MvtFeature> mergeFeatures, List<Object> values) {
    // merge all polygons with the values for the groupBy attributes
    ImmutableList.Builder<MvtFeature> result = ImmutableList.builder();

    // identify features that match the values
    List<MvtFeature> features =
        mergeFeatures.stream()
            .filter(
                feature ->
                    feature.getGeometry() instanceof Polygon
                        || feature.getGeometry() instanceof MultiPolygon)
            .filter(
                feature -> {
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
    } else if (features.size() == 1) {
      return result.add(features.iterator().next()).build();
    }

    // determine clusters of connected features
    ClusterAnalysis clusterResult = ClusterAnalysis.analyse(features, false);

    // process the standalone features
    clusterResult.standalone.forEach(result::add);

    // process each cluster
    clusterResult
        .clusters
        .asMap()
        .forEach(
            (key, value) -> {
              ImmutableSet.Builder<Polygon> polygonBuilder = new ImmutableSet.Builder<>();
              polygonBuilder.addAll(
                  value.stream()
                      .map(MvtFeature::getGeometry)
                      .map(
                          g ->
                              g instanceof MultiPolygon
                                  ? TileGeometryUtil.splitMultiPolygon((MultiPolygon) g)
                                  : ImmutableList.of(g))
                      .flatMap(Collection::stream)
                      .map(Polygon.class::cast)
                      .collect(Collectors.toSet()));
              if (key.getGeometry() instanceof MultiPolygon)
                polygonBuilder.addAll(
                    TileGeometryUtil.splitMultiPolygon((MultiPolygon) key.getGeometry()));
              else polygonBuilder.add((Polygon) key.getGeometry());
              ImmutableSet<Polygon> polygons = polygonBuilder.build();
              Geometry geom;
              switch (polygons.size()) {
                case 0:
                  return;
                case 1:
                  geom = polygons.iterator().next();
                  break;
                default:
                  try {
                    Iterator<Polygon> iter = polygons.iterator();
                    geom = iter.next();
                    while (iter.hasNext()) {
                      // TODO use OverlayNGRobust instead?
                      // geom = OverlayNGRobust.overlay(geom, iter.next(), OverlayNG.SYMDIFFERENCE);
                      OverlayNG overlay =
                          new OverlayNG(geom, iter.next(), precisionModel, OverlayNG.SYMDIFFERENCE);
                      overlay.setStrictMode(true);
                      geom = overlay.getResult();
                    }
                  } catch (Exception e) {
                    geom = geometryFactory.createMultiPolygon(polygons.toArray(Polygon[]::new));
                  }
              }
              LOGGER.trace(
                  "{} grouped by {}: {} polygons", context, values, geom.getNumGeometries());
              if (!geom.isValid()) {
                geom = new GeometryFixer(geom).getResult();
              }

              if (Objects.isNull(geom)
                  || geom.isEmpty()
                  || geom.getNumGeometries() == 0
                  || !geom.isValid()) {
                LOGGER.debug(
                    "{}: Merged polygon feature grouped by {} has no or an invalid geometry. Using {} unmerged features.",
                    context,
                    values,
                    value.size() + 1);
                result.add(key);
                value.forEach(result::add);

              } else {
                // add merged feature
                Map<String, Object> mainClusterFeatureProperties = key.getProperties();
                ImmutableMap.Builder<String, Object> propertiesBuilder = ImmutableMap.builder();
                mainClusterFeatureProperties.entrySet().stream()
                    .filter(prop -> allProperties || properties.contains(prop.getKey()))
                    .filter(
                        prop ->
                            value.stream()
                                .allMatch(
                                    f ->
                                        f.getProperties().containsKey(prop.getKey())
                                            && f.getProperties()
                                                .get(prop.getKey())
                                                .equals(prop.getValue())))
                    .forEach(prop -> propertiesBuilder.put(prop.getKey(), prop.getValue()));

                result.add(
                    new Builder()
                        .id(key.getId())
                        .properties(propertiesBuilder.build())
                        .geometry(geom)
                        .build());
              }
            });

    return result.build();
  }

  // merge all polygons with the values for the groupBy attributes
  private List<MvtFeature> mergeLineStrings(Set<MvtFeature> mergeFeatures, List<Object> values) {
    ImmutableList.Builder<MvtFeature> result = ImmutableList.builder();

    // identify features that match the values
    List<MvtFeature> features =
        mergeFeatures.stream()
            .filter(
                feature ->
                    feature.getGeometry() instanceof LineString
                        || feature.getGeometry() instanceof MultiLineString)
            .filter(
                feature -> {
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
    } else if (features.size() == 1) {
      return result.add(features.iterator().next()).build();
    }

    // determine clusters of connected features
    ClusterAnalysis clusterResult = ClusterAnalysis.analyse(features, true);

    // process the standalone features
    clusterResult.standalone.forEach(result::add);

    // process each cluster
    clusterResult
        .clusters
        .asMap()
        .forEach(
            (key, value) -> {
              ImmutableSet.Builder<LineString> lineStringBuilder = new ImmutableSet.Builder<>();
              lineStringBuilder.addAll(
                  value.stream()
                      .map(MvtFeature::getGeometry)
                      .map(
                          g ->
                              g instanceof MultiLineString
                                  ? TileGeometryUtil.splitMultiLineString((MultiLineString) g)
                                  : ImmutableList.of(g))
                      .flatMap(Collection::stream)
                      .map(LineString.class::cast)
                      .collect(Collectors.toSet()));
              if (key.getGeometry() instanceof MultiLineString)
                lineStringBuilder.addAll(
                    TileGeometryUtil.splitMultiLineString((MultiLineString) key.getGeometry()));
              else lineStringBuilder.add((LineString) key.getGeometry());
              ImmutableSet<LineString> lineStrings = lineStringBuilder.build();
              Geometry geom;
              switch (lineStrings.size()) {
                case 0:
                  return;
                case 1:
                  geom = lineStrings.iterator().next();
                  break;
                default:
                  try {
                    LineMerger lineMerger = new LineMerger();
                    lineMerger.add(lineStrings);
                    geom =
                        geometryFactory.createMultiLineString(
                            ((Collection<LineString>) lineMerger.getMergedLineStrings())
                                .toArray(LineString[]::new));
                  } catch (Exception e) {
                    geom =
                        geometryFactory.createMultiLineString(
                            lineStrings.toArray(LineString[]::new));
                  }
              }
              LOGGER.trace(
                  "{} grouped by {}: {} line strings", context, values, geom.getNumGeometries());
              if (!geom.isValid()) {
                geom = new GeometryFixer(geom).getResult();
              }

              if (geom.isEmpty() || geom.getNumGeometries() == 0 || !geom.isValid()) {
                LOGGER.debug(
                    "{}: Merged line string feature grouped by {} has no or an invalid geometry. Using {} unmerged features.",
                    context,
                    values,
                    value.size());
                result.add(key);
                value.forEach(result::add);

              } else {
                // add merged feature
                Map<String, Object> mainClusterFeatureProperties = key.getProperties();
                ImmutableMap.Builder<String, Object> propertiesBuilder = ImmutableMap.builder();
                mainClusterFeatureProperties.entrySet().stream()
                    .filter(prop -> allProperties || properties.contains(prop.getKey()))
                    .filter(
                        prop ->
                            value.stream()
                                .allMatch(
                                    f ->
                                        f.getProperties().containsKey(prop.getKey())
                                            && f.getProperties()
                                                .get(prop.getKey())
                                                .equals(prop.getValue())))
                    .forEach(prop -> propertiesBuilder.put(prop.getKey(), prop.getValue()));

                result.add(
                    new Builder()
                        .id(key.getId())
                        .properties(propertiesBuilder.build())
                        .geometry(geom)
                        .build());
              }
            });

    return result.build();
  }
}
