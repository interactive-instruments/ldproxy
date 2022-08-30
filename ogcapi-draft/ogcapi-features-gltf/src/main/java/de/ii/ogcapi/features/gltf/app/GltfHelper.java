/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import static de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry.MULTI_POLYGON;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.gltf.domain.GltfAsset;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO cleanup, consolidate, harmonize code

public class GltfHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(GltfHelper.class);
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          // for debugging
          // .enable(SerializationFeature.INDENT_OUTPUT)
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

  private static final double EPSILON = 1.0e-7;

  public static final byte[] MAGIC_GLTF = new byte[] {0x67, 0x6c, 0x54, 0x46};
  public static final byte[] VERSION_2 = new byte[] {0x02, 0x00, 0x00, 0x00};
  public static final byte[] JSON = new byte[] {0x4a, 0x53, 0x4f, 0x4e};
  public static final byte[] BIN = new byte[] {0x42, 0x49, 0x4e, 0x00};
  public static final byte[] JSON_PADDING = new byte[] {0x20};
  public static final byte[] BIN_PADDING = new byte[] {0x00};
  static final double MAX_SHORT = 32767.0;
  static final double MAX_BYTE = 127.0;

  public static void writeGltfBinary(
      GltfAsset gltf, List<ByteArrayOutputStream> buffers, OutputStream outputStream) {

    byte[] json;
    try {
      json = getJson(gltf);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          String.format("Could not write glTF asset. Reason: %s", e.getMessage()));
    }

    // EXT_structural_metadata requires 8-byte padding
    int bufferLength = buffers.stream().map(ByteArrayOutputStream::size).reduce(0, Integer::sum);
    int jsonPadding = (8 - json.length % 8) % 8;
    int bufferPadding = (8 - bufferLength % 8) % 8;
    int totalLength = 12 + 8 + json.length + jsonPadding + 8 + bufferLength + bufferPadding;
    try {
      outputStream.write(MAGIC_GLTF);
      outputStream.write(VERSION_2);
      outputStream.write(GltfHelper.intToLittleEndianInt(totalLength));

      outputStream.write(GltfHelper.intToLittleEndianInt(json.length + jsonPadding));
      outputStream.write(JSON);
      outputStream.write(json);
      for (int i = 0; i < jsonPadding; i++) {
        outputStream.write(JSON_PADDING);
      }

      outputStream.write(GltfHelper.intToLittleEndianInt(bufferLength + bufferPadding));
      outputStream.write(BIN);
      for (ByteArrayOutputStream b : buffers) {
        outputStream.write(b.toByteArray());
      }
      for (int i = 0; i < bufferPadding; i++) {
        outputStream.write(BIN_PADDING);
      }

      outputStream.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Could not write glTF output", e);
    }
  }

  private static byte[] getJson(Object obj) throws JsonProcessingException {
    return MAPPER.writeValueAsBytes(obj);
  }

  public static byte[] intToLittleEndianInt(int v) {
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(v);
    return bb.array();
  }

  public static byte[] longToLittleEndianLong(long v) {
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putLong(v);
    return bb.array();
  }

  public static byte[] intToLittleEndianShort(int v) {
    ByteBuffer bb = ByteBuffer.allocate(2);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putShort((short) v);
    return bb.array();
  }

  public static byte[] doubleToLittleEndianShort(double v) {
    ByteBuffer bb = ByteBuffer.allocate(2);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putShort((short) Math.max(-32767L, Math.min(32767L, Math.round(v))));
    return bb.array();
  }

  public static byte[] doubleToLittleEndianByte(double v) {
    ByteBuffer bb = ByteBuffer.allocate(1);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.put((byte) Math.max(-127L, Math.min(127L, Math.round(v))));
    return bb.array();
  }

  public static byte[] doubleToLittleEndianFloat(double v) {
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putFloat((float) v);
    return bb.array();
  }

  public static byte[] doubleToLittleEndianDouble(double v) {
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putDouble(v);
    return bb.array();
  }

  public static Geometry.MultiPolygon getMultiPolygon(PropertyGltf geometryProperty) {
    if (geometryProperty.getGeometryType().orElse(SimpleFeatureGeometry.ANY) != MULTI_POLYGON) {
      throw new IllegalStateException(
          "Unexpected geometry type, MultiPolygon required: " + geometryProperty.getGeometryType());
    }
    return Geometry.MultiPolygon.of(
        geometryProperty.getNestedProperties().get(0).getNestedProperties().stream()
            .map(
                polygon ->
                    Geometry.Polygon.of(
                        polygon.getNestedProperties().stream()
                            .map(
                                ring ->
                                    Geometry.LineString.of(
                                        GltfHelper.getCoordinates(ring.getNestedProperties())))
                            .collect(Collectors.toUnmodifiableList())))
            .collect(Collectors.toUnmodifiableList()));
  }

  public static double computeArea(double[] ring, int axis1, int axis2) {
    int len = ring.length / 3;
    return (IntStream.range(0, ring.length / 3)
                .mapToDouble(n -> ring[n * 3 + axis1] * ring[((n + 1) % len) * 3 + axis2])
                .sum()
            - IntStream.range(0, ring.length / 3)
                .mapToDouble(n -> ring[((n + 1) % len) * 3 + axis1] * ring[n * 3 + axis2])
                .sum())
        / 2;
  }

  public static double computeAreaTriangle(
      List<Geometry.Coordinate> triangle, int axis1, int axis2) {
    return (triangle.get(0).get(axis1) * (triangle.get(1).get(axis2) - triangle.get(2).get(axis2))
            + triangle.get(1).get(axis1) * (triangle.get(2).get(axis2) - triangle.get(0).get(axis2))
            + triangle.get(2).get(axis1)
                * (triangle.get(0).get(axis2) - triangle.get(1).get(axis2)))
        / 2.0d;
  }

  public static Geometry.Coordinate computeNormal(double[] ring) {
    if (ring.length < 9) {
      throw new IllegalStateException(
          String.format("Ring with less than 3 coordinates: %s", Arrays.toString(ring)));
    }

    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    if (ring.length == 9) {
      // a triangle, use cross product
      double ux = ring[3] - ring[0];
      double uy = ring[4] - ring[1];
      double uz = ring[5] - ring[2];
      double vx = ring[6] - ring[0];
      double vy = ring[7] - ring[1];
      double vz = ring[8] - ring[2];
      x = uy * vz - uz * vy;
      y = uz * vx - ux * vz;
      z = ux * vy - uy * vx;
    } else {
      // use Newell's method
      int l = ring.length;
      for (int i = 0; i < l / 3; i++) {
        x += (ring[i * 3 + 1] - ring[(i * 3 + 4) % l]) * (ring[i * 3 + 2] + ring[(i * 3 + 5) % l]);
        y += (ring[i * 3 + 2] - ring[(i * 3 + 5) % l]) * (ring[i * 3] + ring[(i * 3 + 3) % l]);
        z += (ring[i * 3] - ring[(i * 3 + 3) % l]) * (ring[i * 3 + 1] + ring[(i * 3 + 4) % l]);
      }
    }
    double length = Math.sqrt(x * x + y * y + z * z);
    if (length == 0.0) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Normal has length 0 for ring: {}", ring);
      }
      return null;
    }
    return Geometry.Coordinate.of(x / length, y / length, z / length);
  }

  public static List<Double> getMin(List<Double> vertices) {
    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    IntStream.range(0, 3)
        .forEach(
            axis ->
                IntStream.range(0, vertices.size())
                    .filter(n -> n % 3 == axis)
                    .mapToObj(vertices::get)
                    .min(Comparator.naturalOrder())
                    .ifPresentOrElse(
                        builder::add,
                        () -> {
                          throw new IllegalStateException(
                              String.format(
                                  "glTF generation: Cannot compute minimum for axis %d for vertices: %s",
                                  axis, vertices));
                        }));
    return builder.build();
  }

  public static List<Double> getMax(List<Double> vertices) {
    ImmutableList.Builder<Double> builder = ImmutableList.builder();
    IntStream.range(0, 3)
        .forEach(
            axis ->
                IntStream.range(0, vertices.size())
                    .filter(n -> n % 3 == axis)
                    .mapToObj(vertices::get)
                    .max(Comparator.naturalOrder())
                    .ifPresentOrElse(
                        builder::add,
                        () -> {
                          throw new IllegalStateException(
                              String.format(
                                  "glTF generation: Cannot compute maximum for axis %d for vertices: %s",
                                  axis, vertices));
                        }));
    return builder.build();
  }

  private static Geometry.Coordinate getCoordinate(List<PropertyGltf> coordList) {
    return Geometry.Coordinate.of(
        coordList.stream()
            .map(PropertyBase::getValue)
            .filter(Objects::nonNull)
            .map(Double::parseDouble)
            .collect(Collectors.toUnmodifiableList()));
  }

  public static List<Geometry.Coordinate> getCoordinates(List<PropertyGltf> coordsList) {
    return coordsList.stream()
        .map(coord -> Geometry.Coordinate.of(getCoordinate(coord.getNestedProperties())))
        .collect(Collectors.toUnmodifiableList());
  }

  public static boolean isCoplanar(List<Geometry.Coordinate> coords) {
    if (coords.size() < 4) {
      return true;
    }

    // find three points on the ring that are not collinear
    int[] n = find3rdPoint(coords);

    if (n[1] == -1) {
      return true;
    }

    // establish plane from points A, B, C
    Coordinate AB =
        Coordinate.of(
            coords.get(n[0]).get(0) - coords.get(0).get(0),
            coords.get(n[0]).get(1) - coords.get(0).get(1),
            coords.get(n[0]).get(2) - coords.get(0).get(2));
    Coordinate AC =
        Coordinate.of(
            coords.get(n[1]).get(0) - coords.get(0).get(0),
            coords.get(n[1]).get(1) - coords.get(0).get(1),
            coords.get(n[1]).get(2) - coords.get(0).get(2));

    Coordinate X = crossProduct(AB, AC);

    double d =
        X.get(0) * coords.get(0).get(0)
            + X.get(1) * coords.get(0).get(1)
            + X.get(2) * coords.get(0).get(2);

    // check for all other points that they are on the plane
    for (int i = 3; i < coords.size(); i++) {
      if (Math.abs(
              X.get(0) * coords.get(i).get(0)
                  + X.get(1) * coords.get(i).get(1)
                  + X.get(2) * coords.get(i).get(2)
                  - d)
          > EPSILON) {
        return false;
      }
    }
    return true;
  }

  public static int[] find3rdPoint(List<Geometry.Coordinate> coords) {
    // find three points on the ring that are not collinear
    int k = 1;
    boolean found = false;
    while (!found && k < coords.size()) {
      if (length(sub(coords.get(k), coords.get(0))) > EPSILON) {
        found = true;
      } else {
        k++;
      }
    }
    if (!found) {
      return new int[] {-1, -1};
    }
    int n = k + 1;
    found = false;
    while (!found && n < coords.size()) {
      if (!colinear(coords.get(0), coords.get(k), coords.get(n))) {
        found = true;
      } else {
        n++;
      }
    }

    if (!found) {
      return new int[] {k, -1};
    }

    return new int[] {k, n};
  }

  public static boolean colinear(
      Geometry.Coordinate v1, Geometry.Coordinate v2, Geometry.Coordinate v3) {
    Coordinate AB =
        Coordinate.of(v2.get(0) - v1.get(0), v2.get(1) - v1.get(1), v2.get(2) - v1.get(2));
    Coordinate AC =
        Coordinate.of(v3.get(0) - v1.get(0), v3.get(1) - v1.get(1), v3.get(2) - v1.get(2));

    return length(crossProduct(normalize(AB), normalize(AC))) < EPSILON;
  }

  public static Geometry.Coordinate crossProduct(Geometry.Coordinate v1, Geometry.Coordinate v2) {
    return Coordinate.of(
        v1.get(1) * v2.get(2) - v2.get(1) * v1.get(2),
        v2.get(0) * v1.get(2) - v1.get(0) * v2.get(2),
        v1.get(0) * v2.get(1) - v1.get(1) * v2.get(0));
  }

  public static double dot(Geometry.Coordinate v1, Geometry.Coordinate v2) {
    return v1.get(0) * v2.get(0) + v1.get(1) * v2.get(1) + v1.get(2) * v2.get(2);
  }

  public static Geometry.Coordinate add(Geometry.Coordinate v1, Geometry.Coordinate v2) {
    return Coordinate.of(v1.get(0) + v2.get(0), v1.get(1) + v2.get(1), v1.get(1) + v2.get(1));
  }

  public static Geometry.Coordinate sub(Geometry.Coordinate v1, Geometry.Coordinate v2) {
    return Coordinate.of(v1.get(0) - v2.get(0), v1.get(1) - v2.get(1), v1.get(1) - v2.get(1));
  }

  public static double length(Geometry.Coordinate v) {
    return Math.sqrt(
        v.size() == 2
            ? v.get(0) * v.get(0) + v.get(1) * v.get(1)
            : v.get(0) * v.get(0) + v.get(1) * v.get(1) + v.get(2) * v.get(2));
  }

  public static Geometry.Coordinate normalize(Geometry.Coordinate v) {
    double length = length(v);
    return v.size() == 2
        ? new Geometry.Coordinate(v.get(0) / length, v.get(1) / length)
        : new Geometry.Coordinate(v.get(0) / length, v.get(1) / length, v.get(2) / length);
  }
}
