/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ogcapi.features.gltf.domain.GltfAsset;
import de.ii.ogcapi.features.gltf.domain.QueriesHandler3dTiles.QueryInputSubtree;
import de.ii.ogcapi.features.gltf.domain.Subtree;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.SIntersects;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.PropertyBase;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderHits.class);

  // TODO make configurable
  public static final int AVAILABLE_LEVELS = 9;
  public static final int SUBTREE_LEVELS = 3;

  public static final double EPSILON = 1.0e-7;

  public static final byte[] MAGIC_GLTF = new byte[] {0x67, 0x6c, 0x54, 0x46};
  public static final byte[] MAGIC_SUBT = new byte[] {0x73, 0x75, 0x62, 0x74};
  public static final byte[] VERSION_1 = new byte[] {0x01, 0x00, 0x00, 0x00};
  public static final byte[] VERSION_2 = new byte[] {0x02, 0x00, 0x00, 0x00};
  public static final byte[] JSON = new byte[] {0x4a, 0x53, 0x4f, 0x4e};
  public static final byte[] BIN = new byte[] {0x42, 0x49, 0x4e, 0x00};
  public static final byte[] JSON_PADDING = new byte[] {0x20};
  public static final byte[] BIN_PADDING = new byte[] {0x00};

  public static void writeGltfBinary(
      GltfAsset gltf,
      ByteArrayOutputStream bufferIndices,
      ByteArrayOutputStream bufferVertices,
      OutputStream outputStream) {

    byte[] json;
    try {
      json = getBytes(gltf);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          String.format("Could not write glTF asset. Reason: %s", e.getMessage()));
    }

    int bufferLength = bufferIndices.size() + bufferVertices.size();
    int jsonPadding = (4 - json.length % 4) % 4;
    int bufferPadding = (4 - bufferLength % 4) % 4;
    int totalLength = 12 + 8 + json.length + jsonPadding + 8 + bufferLength + bufferPadding;
    try {
      outputStream.write(MAGIC_GLTF);
      outputStream.write(VERSION_2);
      outputStream.write(Helper.intToLittleEndianInt(totalLength));

      outputStream.write(Helper.intToLittleEndianInt(json.length + jsonPadding));
      outputStream.write(JSON);
      outputStream.write(json);
      for (int i = 0; i < jsonPadding; i++) {
        outputStream.write(JSON_PADDING);
      }

      outputStream.write(Helper.intToLittleEndianInt(bufferLength + bufferPadding));
      outputStream.write(BIN);
      outputStream.write(bufferIndices.toByteArray());
      outputStream.write(bufferVertices.toByteArray());
      for (int i = 0; i < bufferPadding; i++) {
        outputStream.write(BIN_PADDING);
      }

      outputStream.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Could not write glTF output", e);
    }
  }

  private static byte[] getBytes(Object obj) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    // for debugging
    // mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    return mapper.writeValueAsBytes(obj);
  }

  public static void write3dTilesSubtreeBinary(
      Subtree subtree,
      ByteArrayOutputStream bufferTilesAvailability,
      ByteArrayOutputStream bufferContentAvailability,
      ByteArrayOutputStream bufferChildSubtreeAvailability,
      OutputStream outputStream) {

    byte[] json;
    try {
      json = getBytes(subtree);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          String.format("Could not write 3D Tiles subtree. Reason: %s", e.getMessage()));
    }

    int bufferLength =
        bufferTilesAvailability.size()
            + bufferContentAvailability.size()
            + bufferChildSubtreeAvailability.size();
    int jsonPadding = (4 - json.length % 4) % 4;
    int bufferPadding = (4 - bufferLength % 4) % 4;

    try {
      outputStream.write(MAGIC_SUBT);
      outputStream.write(VERSION_1);

      outputStream.write(Helper.intToLittleEndianLong(json.length + jsonPadding));
      outputStream.write(Helper.intToLittleEndianLong(bufferLength + bufferPadding));

      outputStream.write(json);
      for (int i = 0; i < jsonPadding; i++) {
        outputStream.write(JSON_PADDING);
      }

      outputStream.write(bufferTilesAvailability.toByteArray());
      outputStream.write(bufferContentAvailability.toByteArray());
      outputStream.write(bufferChildSubtreeAvailability.toByteArray());
      for (int i = 0; i < bufferPadding; i++) {
        outputStream.write(BIN_PADDING);
      }

      outputStream.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Could not write glTF output", e);
    }
  }

  private static void sub(OgcApi api, String collectionId, int level, long x, long y) {
    BoundingBox bbox = computeBbox(api, collectionId, level, x, y);

    // sub:
    //   computeBbox
    //   query data
    //   check, if there is at least one feature in the tile
    //   tile available (or not)
    //   content available = tile available and level >= MIN_CONTENT_LEVEL

  }

  public static boolean hasData(
      FeaturesCoreQueriesHandler queriesHandler, QueryInputSubtree queryInput, BoundingBox bbox) {
    Envelope envelope =
        Envelope.of(
            bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax(), bbox.getEpsgCrs());

    FeatureQuery query =
        ImmutableFeatureQuery.builder()
            .type(queryInput.getFeatureType())
            .hitsOnly(true)
            .limit(1)
            .filter(
                SIntersects.of(
                    Property.of(queryInput.getGeometryProperty()), SpatialLiteral.of(envelope)))
            .build();

    FeaturesCoreQueriesHandler.QueryInputFeatures queryInputHits =
        new ImmutableQueryInputFeatures.Builder()
            .collectionId(queryInput.getCollectionId())
            .query(query)
            .featureProvider(queryInput.getFeatureProvider())
            .defaultCrs(OgcCrs.CRS84)
            .defaultPageSize(1)
            .sendResponseAsStream(false)
            .path("/_for_internal_use_only_")
            .showsFeatureSelfLink(false)
            .build();

    ApiRequestContext requestContext =
        new Builder()
            .mediaType(FeaturesFormatHits.MEDIA_TYPE)
            .alternateMediaTypes(ImmutableList.of())
            .language(Locale.ENGLISH)
            .api(queryInput.getApi())
            .request(Optional.empty())
            .requestUri(queryInput.getServicesUri().resolve("/_for_internal_use_only_"))
            .build();

    Response response = queriesHandler.handle(Query.FEATURES, queryInputHits, requestContext);

    String result = new String((byte[]) response.getEntity(), StandardCharsets.UTF_8);
    LOGGER.debug("Result: {}", result);

    return (Long.parseLong(result) > 0);
  }

  public static byte[] intToLittleEndianLong(int v) {
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(v);
    return bb.array();
  }

  public static byte[] intToLittleEndianInt(int v) {
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(v);
    return bb.array();
  }

  public static byte[] intToLittleEndianShort(int v) {
    ByteBuffer bb = ByteBuffer.allocate(2);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putShort((short) v);
    return bb.array();
  }

  public static byte[] doubleToLittleEndianFloat(double v) {
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putFloat((float) v);
    return bb.array();
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

  public static Geometry.Coordinate computeNormal(List<Geometry.Coordinate> ring) {
    if (ring.size() < 3) {
      throw new IllegalStateException(String.format("Ring with less than 3 coordinates: %s", ring));
    }

    double x = 0.0;
    double y = 0.0;
    double z = 0.0;
    for (int i = 0; i < ring.size() - 1; i++) {
      Geometry.Coordinate p0 = ring.get(i);
      Geometry.Coordinate p1 = ring.get(i + 1);

      x += (p0.get(1) - p1.get(1)) * (p0.get(2) + p1.get(2));
      y += (p0.get(2) - p1.get(2)) * (p0.get(0) + p1.get(0));
      z += (p0.get(0) - p1.get(0)) * (p0.get(1) + p1.get(1));
    }
    double length = Math.sqrt(x * x + y * y + z * z);
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

  public static Geometry.Coordinate getCoordinate(List<PropertyGltf> coordList) {
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

  public static BoundingBox computeBbox(
      OgcApi api, String collectionId, int level, long x, long y) {
    BoundingBox bbox = api.getSpatialExtent(collectionId).orElseThrow();
    double dx = bbox.getXmax() - bbox.getXmin();
    double dy = bbox.getYmax() - bbox.getYmin(); // TODO crossing the antimeridian
    double factor = Math.pow(2, level);
    double xmin = bbox.getXmin() + dx / factor * x;
    double xmax = xmin + dx / factor;
    double ymin = bbox.getYmin() + dy / factor * y;
    double ymax = ymin + dy / factor;
    return BoundingBox.of(
        xmin,
        ymin,
        Objects.requireNonNull(bbox.getZmin()),
        xmax,
        ymax,
        Objects.requireNonNull(bbox.getZmax()),
        OgcCrs.CRS84h);
  }

  public static double degToRad(double degree) {
    return degree / 180.0 * Math.PI;
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
