/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CesiumData {

  private class FeatureGeometry {
    String name;
    Geometry<?> geometry;

    FeatureGeometry(String name, Geometry<?> geometry) {
      this.name = name;
      this.geometry = geometry;
    }
  }

  public final List<FeatureHtml> features;
  private final List<String> geometryProperties;
  private final boolean clampToGround;

  private List<FeatureGeometry> geometries = null;

  public CesiumData(List<FeatureHtml> features, List<String> geometryProperties) {
    this.features = features;
    this.geometryProperties = geometryProperties;
    this.clampToGround = true; // TODO make configurable
  }

  private List<FeatureGeometry> getGeometries(
      List<FeatureHtml> features, List<String> geometryProperties) {
    return features.stream()
        .map(
            feature -> {
              List<PropertyHtml> geomProperties = ImmutableList.of();
              for (String geometryProperty : geometryProperties) {
                geomProperties = feature.findPropertiesByPath(geometryProperty);
                if (!geomProperties.isEmpty()) break;
              }
              if (geomProperties.isEmpty()) {
                Optional<PropertyHtml> defaultGeom = feature.getGeometry();
                if (defaultGeom.isPresent()) {
                  geomProperties = ImmutableList.of(defaultGeom.get());
                }
              }
              return geomProperties.stream()
                  .map(PropertyHtml::parseGeometry)
                  .map(geom -> new FeatureGeometry(feature.getName(), geom))
                  .collect(Collectors.toUnmodifiableList());
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  public String getFeatureExtent() {
    if (Objects.isNull(geometries)) geometries = getGeometries(features, geometryProperties);
    List<Coordinate> coordinates =
        geometries.stream()
            .map(f -> f.geometry.getCoordinatesFlat())
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableList());
    double minLon = getMin(coordinates, 0).orElse(-180.0);
    double minLat = getMin(coordinates, 1).orElse(-90.0);
    double maxLon = getMax(coordinates, 0).orElse(180.0);
    double maxLat = getMax(coordinates, 1).orElse(90.0);
    return "Rectangle.fromDegrees(" + minLon + "," + minLat + "," + maxLon + "," + maxLat + ")";
  }

  public List<String> getMapEntities() {
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    if (Objects.isNull(geometries)) geometries = getGeometries(features, geometryProperties);
    geometries.stream()
        .forEach(
            f -> {
              final Double minHeight =
                  clampToGround && f.geometry.is3d()
                      ? getMin(f.geometry.getCoordinatesFlat(), 2).orElse(0.0)
                      : null;
              switch (f.geometry.getType()) {
                case MultiPoint:
                  ((Geometry.MultiPoint) f.geometry)
                      .getCoordinates().stream()
                          .forEach(point -> addPoint(builder, f.name, point, minHeight));
                  break;
                case Point:
                  addPoint(builder, f.name, (Geometry.Point) f.geometry, minHeight);
                  break;
                case MultiLineString:
                  ((Geometry.MultiLineString) f.geometry)
                      .getCoordinates().stream()
                          .forEach(line -> addLineString(builder, f.name, line, minHeight));
                  break;
                case LineString:
                  addLineString(builder, f.name, (Geometry.LineString) f.geometry, minHeight);
                  break;
                case MultiPolygon:
                  ((Geometry.MultiPolygon) f.geometry)
                      .getCoordinates().stream()
                          .forEach(polygon -> addPolygon(builder, f.name, polygon, minHeight));
                  break;
                case Polygon:
                  addPolygon(builder, f.name, (Geometry.Polygon) f.geometry, minHeight);
                  break;
                default:
                  throw new IllegalStateException(
                      "Unsupported geometry type: " + f.geometry.getType());
              }
            });
    return builder.build();
  }

  private void addPolygon(
      ImmutableList.Builder<String> builder,
      String name,
      Geometry.Polygon polygon,
      Double minHeight) {
    boolean is3d = polygon.is3d();
    if (polygon.getCoordinates().size() == 1) {
      builder.add(
          "viewer.entities.add({"
              + "name:\""
              + name
              + "\","
              + (is3d
                  ? "polygon:{hierarchy:Cartesian3.fromDegreesArrayHeights(["
                      + getCoordinatesString(
                          polygon.getCoordinates().get(0).getCoordinates(), minHeight)
                      + "]),perPositionHeight:true,"
                  : "polygon:{hierarchy:Cartesian3.fromDegreesArrayHeights(["
                      + getCoordinatesString(polygon.getCoordinates().get(0).getCoordinates())
                      + "]),perPositionHeight:true,")
              + "material:Color.BLUE.withAlpha(0.5),"
              + "outline:true,outlineColor:Color.BLUE"
              + "}});");
    } else {
      builder.add(
          "viewer.entities.add({"
              + "name:\""
              + name
              + "\","
              + (is3d
                  ? "polygon:{hierarchy:{positions:Cartesian3.fromDegreesArrayHeights(["
                      + getCoordinatesString(
                          polygon.getCoordinates().get(0).getCoordinates(), minHeight)
                      + "]),"
                      + "holes:["
                      + IntStream.range(1, polygon.getCoordinates().size())
                          .mapToObj(
                              n ->
                                  "{positions:Cartesian3.fromDegreesArrayHeights(["
                                      + getCoordinatesString(
                                          polygon.getCoordinates().get(n).getCoordinates(),
                                          minHeight)
                                      + "])}")
                          .collect(Collectors.joining(","))
                      + "]},perPositionHeight:true,"
                  : "polygon:{hierarchy:{positions:Cartesian3.fromDegreesArrayHeights(["
                      + getCoordinatesString(polygon.getCoordinates().get(0).getCoordinates())
                      + "]),"
                      + "holes:["
                      + IntStream.range(1, polygon.getCoordinates().size())
                          .mapToObj(
                              n ->
                                  "{positions:Cartesian3.fromDegreesArrayHeights(["
                                      + getCoordinatesString(
                                          polygon.getCoordinates().get(n).getCoordinates())
                                      + "])}")
                          .collect(Collectors.joining(","))
                      + "]},perPositionHeight:true,")
              + "material:Color.BLUE.withAlpha(0.5),"
              + "outline:true,outlineColor:Color.BLUE"
              + "}});");
    }
  }

  private void addLineString(
      ImmutableList.Builder<String> builder,
      String name,
      Geometry.LineString line,
      Double minHeight) {
    builder.add(
        "viewer.entities.add({"
            + "name:\""
            + name
            + "\","
            + (line.is3d()
                ? "polyline:{positions:Cartesian3.fromDegreesArrayHeights(["
                    + getCoordinatesString(line.getCoordinates(), minHeight)
                    + "]),perPositionHeight:true,"
                : "polyline:{positions:Cartesian3.fromDegreesArrayHeights(["
                    + getCoordinatesString(line.getCoordinates())
                    + "]),perPositionHeight:true,")
            +
            // "outline:true,outlineColor:Color.BLUE," +
            "width:1,"
            + "material:Color.BLUE"
            + "}});");
  }

  private void addPoint(
      ImmutableList.Builder<String> builder, String name, Geometry.Point point, Double minHeight) {
    builder.add(
        "viewer.entities.add({"
            + "name:\""
            + name
            + "\","
            + "position:Cartesian3.fromDegrees("
            + getCoordinatesString(point.getCoordinates(), minHeight)
            + "),"
            + "point:{pixelSize:5,color:Color.BLUE}"
            + "});");
  }

  private Optional<Double> getMin(List<Geometry.Coordinate> coordinates, int axis) {
    return coordinates.stream().map(coord -> coord.get(axis)).min(Comparator.naturalOrder());
  }

  private Optional<Double> getMax(List<Geometry.Coordinate> coordinates, int axis) {
    return coordinates.stream().map(coord -> coord.get(axis)).max(Comparator.naturalOrder());
  }

  private boolean is3d(List<Geometry.Coordinate> coordinates) {
    return !coordinates.isEmpty() && coordinates.get(0).size() == 3;
  }

  private String getCoordinatesString(List<Geometry.Coordinate> coordinates) {
    if (is3d(coordinates))
      return coordinates.stream()
          .flatMap(List::stream)
          .map(String::valueOf)
          .collect(Collectors.joining(","));

    return coordinates.stream()
        .map(coord -> ImmutableList.of(coord.get(0), coord.get(1), 0.0))
        .flatMap(List::stream)
        .map(String::valueOf)
        .collect(Collectors.joining(","));
  }

  private String getCoordinatesString(List<Geometry.Coordinate> coordinates, Double deltaHeight) {
    if (Objects.isNull(deltaHeight)) return getCoordinatesString(coordinates);
    return coordinates.stream()
        .map(
            coord -> Geometry.Coordinate.of(coord.get(0), coord.get(1), coord.get(2) - deltaHeight))
        .flatMap(List::stream)
        .map(String::valueOf)
        .collect(Collectors.joining(","));
  }
}
