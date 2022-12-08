/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

// reconsider in https://github.com/interactive-instruments/ldproxy/issues/543
@SuppressWarnings("PMD.ExcessivePublicCount")
public interface Geometry<T> {

  String COORDINATE_DIMENSION_ERROR_MESSAGE =
      "The first coordinate has dimension %d, but at least one other coordinate has a different dimension.";

  @SuppressWarnings("PMD.FieldNamingConventions")
  enum Type {
    Point,
    LineString,
    Polygon,
    MultiPoint,
    MultiLineString,
    MultiPolygon
  }

  Type getType();

  T getCoordinates();

  @JsonIgnore
  Optional<EpsgCrs> getCrs();

  @JsonIgnore
  List<Coordinate> getCoordinatesFlat();

  @JsonIgnore
  boolean is3d();

  @Value.Immutable
  @JsonDeserialize(builder = ImmutablePoint.Builder.class)
  interface Point extends Geometry<Coordinate> {

    static Point of(double x, double y) {
      return new ImmutablePoint.Builder().coordinates(Coordinate.of(x, y)).build();
    }

    static Point of(double x, double y, double z) {
      return new ImmutablePoint.Builder().coordinates(Coordinate.of(x, y, z)).build();
    }

    static Point of(List<Double> xyz) {
      if (xyz.size() == 2) {
        return Point.of(xyz.get(0), xyz.get(1));
      } else if (xyz.size() == 3) {
        return Point.of(xyz.get(0), xyz.get(1), xyz.get(2));
      }
      throw new IllegalArgumentException(
          String.format("A coordinate requires 2 or 3 values. Found: %s", xyz));
    }

    static Point of(Coordinate coordinate) {
      return new ImmutablePoint.Builder().coordinates(coordinate).build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default List<Coordinate> getCoordinatesFlat() {
      return ImmutableList.of(getCoordinates());
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default boolean is3d() {
      return getCoordinates().size() == 3;
    }

    @Override
    default Type getType() {
      return Type.Point;
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableLineString.Builder.class)
  interface LineString extends Geometry<List<Coordinate>> {

    static LineString of(Coordinate... coordinates) {
      return new ImmutableLineString.Builder().addCoordinates(coordinates).build();
    }

    static LineString of(List<Coordinate> coordinates) {
      return new ImmutableLineString.Builder().addAllCoordinates(coordinates).build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default List<Coordinate> getCoordinatesFlat() {
      return getCoordinates();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default boolean is3d() {
      return getCoordinates().get(0).size() == 3;
    }

    @Value.Check
    default void check() {
      Preconditions.checkState(
          getCoordinates().size() > 1,
          "A line string must have at least two coordinates, found %d coordiantes.",
          getCoordinates().size());
      List<Coordinate> coords = getCoordinatesFlat();
      Preconditions.checkState(
          coords.stream().skip(1).allMatch(c -> c.size() == coords.get(0).size()),
          COORDINATE_DIMENSION_ERROR_MESSAGE,
          coords.size());
    }

    @Override
    default Type getType() {
      return Type.LineString;
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutablePolygon.Builder.class)
  interface Polygon extends Geometry<List<List<Coordinate>>> {

    static Polygon of(List<Coordinate>... coordinates) {
      return new ImmutablePolygon.Builder().addCoordinates(coordinates).build();
    }

    static Polygon of(EpsgCrs crs, List<Coordinate>... coordinates) {
      return new ImmutablePolygon.Builder().crs(crs).addCoordinates(coordinates).build();
    }

    static Polygon of(List<List<Coordinate>> coordinates) {
      return new ImmutablePolygon.Builder().addAllCoordinates(coordinates).build();
    }

    static Polygon of(EpsgCrs crs, List<List<Coordinate>> coordinates) {
      return new ImmutablePolygon.Builder().crs(crs).addAllCoordinates(coordinates).build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default List<Coordinate> getCoordinatesFlat() {
      return getCoordinates().stream()
          .flatMap(List::stream)
          .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default boolean is3d() {
      return getCoordinates().get(0).get(0).size() == 3;
    }

    @Value.Check
    default void check() {
      Preconditions.checkState(
          !getCoordinates().isEmpty(),
          "A polygon must have at least an outer ring, no ring was found.");
      getCoordinates().stream()
          .forEach(
              coords ->
                  Preconditions.checkState(
                      coords.size() > 3,
                      "Each ring must have at least 4 coordinates, found: %d.",
                      coords.size()));
      List<Coordinate> coords = getCoordinatesFlat();
      Preconditions.checkState(
          coords.stream().skip(1).allMatch(c -> c.size() == coords.get(0).size()),
          COORDINATE_DIMENSION_ERROR_MESSAGE,
          coords.size());
    }

    @Override
    default Type getType() {
      return Type.Polygon;
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableMultiPoint.Builder.class)
  interface MultiPoint extends Geometry<List<Point>> {

    static MultiPoint of(Point... points) {
      return new ImmutableMultiPoint.Builder().addCoordinates(points).build();
    }

    static MultiPoint of(List<Point> points) {
      return new ImmutableMultiPoint.Builder().addAllCoordinates(points).build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default List<Coordinate> getCoordinatesFlat() {
      return getCoordinates().stream()
          .map(Geometry::getCoordinates)
          .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default boolean is3d() {
      return !getCoordinates().isEmpty() && getCoordinates().get(0).is3d();
    }

    @Value.Check
    default void check() {
      List<Coordinate> coords = getCoordinatesFlat();
      Preconditions.checkState(
          coords.stream().skip(1).allMatch(c -> c.size() == coords.get(0).size()),
          COORDINATE_DIMENSION_ERROR_MESSAGE,
          coords.size());
    }

    @Override
    default Type getType() {
      return Type.MultiPoint;
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableMultiLineString.Builder.class)
  interface MultiLineString extends Geometry<List<LineString>> {

    static MultiLineString of(LineString... lineStrings) {
      return new ImmutableMultiLineString.Builder().addCoordinates(lineStrings).build();
    }

    static MultiLineString of(List<LineString> lineStrings) {
      return new ImmutableMultiLineString.Builder().addAllCoordinates(lineStrings).build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default List<Coordinate> getCoordinatesFlat() {
      return getCoordinates().stream()
          .map(line -> line.getCoordinates())
          .flatMap(List::stream)
          .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default boolean is3d() {
      return !getCoordinates().isEmpty() && getCoordinates().get(0).is3d();
    }

    @Value.Check
    default void check() {
      List<Coordinate> coords = getCoordinatesFlat();
      Preconditions.checkState(
          coords.stream().skip(1).allMatch(c -> c.size() == coords.get(0).size()),
          COORDINATE_DIMENSION_ERROR_MESSAGE,
          coords.size());
    }

    @Override
    default Type getType() {
      return Type.MultiLineString;
    }
  }

  @Value.Immutable
  @JsonDeserialize(builder = ImmutableMultiPolygon.Builder.class)
  interface MultiPolygon extends Geometry<List<Polygon>> {

    static MultiPolygon of(Polygon... polygons) {
      return new ImmutableMultiPolygon.Builder().addCoordinates(polygons).build();
    }

    static MultiPolygon of(List<Polygon> polygons) {
      return new ImmutableMultiPolygon.Builder().addAllCoordinates(polygons).build();
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default List<Coordinate> getCoordinatesFlat() {
      return getCoordinates().stream()
          .map(polygon -> polygon.getCoordinates())
          .flatMap(List::stream)
          .flatMap(List::stream)
          .collect(Collectors.toUnmodifiableList());
    }

    @Override
    @JsonIgnore
    @Value.Derived
    default boolean is3d() {
      return !getCoordinates().isEmpty() && getCoordinates().get(0).is3d();
    }

    @Value.Check
    default void check() {
      List<Coordinate> coords = getCoordinatesFlat();
      Preconditions.checkState(
          coords.stream().skip(1).allMatch(c -> c.size() == coords.get(0).size()),
          COORDINATE_DIMENSION_ERROR_MESSAGE,
          coords.size());
    }

    @Override
    default Type getType() {
      return Type.MultiPolygon;
    }
  }

  class CoordinateSerializer extends StdSerializer<Coordinate> {
    private static final long serialVersionUID = 1L;

    public CoordinateSerializer() {
      this(null);
    }

    public CoordinateSerializer(Class<Coordinate> t) {
      super(t);
    }

    @Override
    public void serialize(Coordinate value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeStartArray();
      gen.writeNumber(value.x);
      gen.writeNumber(value.y);
      if (!Double.isNaN(value.z)) {
        gen.writeNumber(value.z);
      }
      gen.writeEndArray();
    }
  }

  @JsonFormat(shape = JsonFormat.Shape.ARRAY)
  @JsonSerialize(using = CoordinateSerializer.class)
  class Coordinate {

    public final double x;
    public final double y;
    public final double z;

    @JsonCreator
    public Coordinate(
        @JsonProperty("x") double x, @JsonProperty("y") double y, @JsonProperty("z") double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    @JsonIgnore
    public int size() {
      return Double.isNaN(this.z) ? 2 : 3;
    }

    public double get(int index) {
      switch (index) {
        case 0:
          return x;
        case 1:
          return y;
        case 2:
          return z;
        default:
          throw new IllegalStateException(
              String.format(
                  "Invalid index for a coordinate, must be between 0 and 2. Found: %d", index));
      }
    }

    public static Coordinate of(double x, double y) {
      return new Coordinate(x, y, Double.NaN);
    }

    public static Coordinate of(double x, double y, double z) {
      return new Coordinate(x, y, z);
    }

    public static Coordinate of(List<Double> xyz) {
      if (xyz.size() == 2) {
        return Coordinate.of(xyz.get(0), xyz.get(1));
      } else if (xyz.size() == 3) {
        return Coordinate.of(xyz.get(0), xyz.get(1), xyz.get(2));
      }
      throw new IllegalArgumentException(
          String.format("A coordinate requires 2 or 3 values. Found: %s", xyz));
    }
  }
}
