/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.PropertyBase;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.immutables.value.Value;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Value.Modifiable
@Value.Style(set = "*")
public interface PropertyMVT extends PropertyBase<PropertyMVT, FeatureSchema> {

  @Value.Lazy
  default boolean hasCoordinates() {
    return getNestedProperties().stream().anyMatch(child -> child.isCoordinate() || child.hasCoordinates());
  }

  @Value.Lazy
  default boolean isCoordinate() {
    return isArray() && getNestedProperties().stream().filter(PropertyBase::isValue).count() >= 2;
  }

  default Optional<Geometry> getJtsGeometry(GeometryFactory geometryFactory) {
    return getSchema().flatMap(FeatureSchema::getGeometryType)
        .flatMap(geometryType -> {
          switch (geometryType) {
            case POINT:
              return getJtsPoint(geometryFactory);
            case MULTI_POINT:
              return getJtsMultiPoint(geometryFactory);
            case LINE_STRING:
              return getJtsLineString(geometryFactory, true);
            case MULTI_LINE_STRING:
              return getJtsMultiLineString(geometryFactory);
            case POLYGON:
              return getJtsPolygon(geometryFactory, true);
            case MULTI_POLYGON:
              return getJtsMultiPolygon(geometryFactory);
          }
          return Optional.empty();
        });
  }

  default Optional<Geometry> getJtsPoint(GeometryFactory geometryFactory) {
    return getNestedProperties().isEmpty()
        ? Optional.empty()
        : getNestedProperties().get(0)
            .getJtsCoordinate()
            .map(geometryFactory::createPoint);
  }

  default Optional<MultiPoint> getJtsMultiPoint(GeometryFactory geometryFactory) {
    return getJtsCoordinateArrayAsGeometry(geometryFactory::createMultiPointFromCoords, true);
  }

  default Optional<LineString> getJtsLineString(GeometryFactory geometryFactory, boolean isTopLevel) {
    return getJtsCoordinateArrayAsGeometry(geometryFactory::createLineString, isTopLevel);
  }

  default Optional<LinearRing> getJtsLinearRing(GeometryFactory geometryFactory) {
    return getJtsCoordinateArrayAsGeometry(geometryFactory::createLinearRing, false);
  }

  default Optional<MultiLineString> getJtsMultiLineString(GeometryFactory geometryFactory) {
    LineString[] lineStrings = getJtsGeometryArray(arrayProperty -> arrayProperty.getJtsLineString(geometryFactory, false), LineString[]::new);
    return lineStrings.length == 0
        ? Optional.empty()
        : Optional.of(geometryFactory.createMultiLineString(lineStrings));
  }

  default Optional<Polygon> getJtsPolygon(GeometryFactory geometryFactory, boolean isTopLevel) {
    LinearRing[] linearRings = isTopLevel
        ? getJtsGeometryArray(arrayProperty -> arrayProperty.getJtsLinearRing(geometryFactory), LinearRing[]::new)
        : getJtsNestedGeometryArray(arrayProperty -> arrayProperty.getJtsLinearRing(geometryFactory), LinearRing[]::new);
    return linearRings.length == 0
        ? Optional.empty()
        : linearRings.length == 1
          ? Optional.of(geometryFactory.createPolygon(linearRings[0]))
          : Optional.of(geometryFactory.createPolygon(linearRings[0], Arrays.copyOfRange(linearRings, 1, linearRings.length)));
  }

  default Optional<MultiPolygon> getJtsMultiPolygon(GeometryFactory geometryFactory) {
    Polygon[] polygons = getJtsGeometryArray(arrayProperty -> arrayProperty.getJtsPolygon(geometryFactory, false), Polygon[]::new);
    return polygons.length == 0
        ? Optional.empty()
        : Optional.of(geometryFactory.createMultiPolygon(polygons));
  }

  default <T extends Geometry> T[] getJtsGeometryArray(Function<PropertyMVT, Optional<T>> geometryCreator, IntFunction<T[]> arrayCreator) {
    return getNestedProperties().isEmpty()
        ? arrayCreator.apply(0)
        : getNestedProperties().get(0)
            .getJtsNestedGeometryArray(geometryCreator, arrayCreator);
  }

  default <T extends Geometry> T[] getJtsNestedGeometryArray(Function<PropertyMVT, Optional<T>> geometryCreator, IntFunction<T[]> arrayCreator) {
    return getNestedProperties().stream()
              .map(geometryCreator)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .toArray(arrayCreator);
  }

  default <T extends Geometry> Optional<T> getJtsCoordinateArrayAsGeometry(Function<Coordinate[], T> geometryCreator, boolean isTopLevel) {
    Coordinate[] coordinateArray = isTopLevel ? getJtsCoordinateArray() : getJtsNestedCoordinateArray();
    return coordinateArray.length == 0
        ? Optional.empty()
        : Optional.of(geometryCreator.apply(coordinateArray));
  }

  @Value.Lazy
  default Coordinate[] getJtsCoordinateArray() {
    return getNestedProperties().isEmpty()
        ? new Coordinate[0]
        : getNestedProperties().get(0)
            .getJtsNestedCoordinateArray();
  }

  @Value.Lazy
  default Coordinate[] getJtsNestedCoordinateArray() {
    return getNestedProperties().stream()
              .map(PropertyMVT::getJtsCoordinate)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .toArray(Coordinate[]::new);
  }

  @Value.Lazy
  default Optional<Coordinate> getJtsCoordinate() {
    Coordinate coordinate = new Coordinate();
    int i = 0;

    for (PropertyMVT value : getNestedProperties()) {
      if (value.isValue() && Objects.nonNull(value.getValue())) {
        double doubleValue;
        try {
          doubleValue = Double.parseDouble(value.getValue());
        } catch (Throwable e) {
          break;
        }

        if (i == 0) {
          coordinate.setX(doubleValue);
        } else if (i == 1) {
          coordinate.setY(doubleValue);
        } else if (i == 2) {
          coordinate.setZ(doubleValue);
        } else {
          break;
        }

        i++;
      }
    }
    return i >= 2
        ? Optional.of(coordinate)
        : Optional.empty();
  }
}
