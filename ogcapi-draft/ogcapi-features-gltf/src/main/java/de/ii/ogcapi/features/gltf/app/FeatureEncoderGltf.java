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
import de.ii.ogcapi.features.gltf.domain.FeatureTransformationContextGltf;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.features.gltf.domain.ImmutableAccessor;
import de.ii.ogcapi.features.gltf.domain.ImmutableAssetMetadata;
import de.ii.ogcapi.features.gltf.domain.ImmutableAttributes;
import de.ii.ogcapi.features.gltf.domain.ImmutableBuffer;
import de.ii.ogcapi.features.gltf.domain.ImmutableBufferView;
import de.ii.ogcapi.features.gltf.domain.ImmutableGltfAsset;
import de.ii.ogcapi.features.gltf.domain.ImmutableMaterial;
import de.ii.ogcapi.features.gltf.domain.ImmutableMesh;
import de.ii.ogcapi.features.gltf.domain.ImmutableNode;
import de.ii.ogcapi.features.gltf.domain.ImmutablePbrMetallicRoughness;
import de.ii.ogcapi.features.gltf.domain.ImmutablePrimitive;
import de.ii.ogcapi.features.gltf.domain.ImmutableScene;
import de.ii.ogcapi.features.gltf.domain.Material;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import de.ii.ogcapi.features.html.domain.Geometry.MultiPolygon;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.features.domain.PropertyBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import earcut4j.Earcut;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureEncoderGltf extends FeatureObjectEncoder<PropertyGltf, FeatureGltf> {

  private enum AXES {
    XYZ,
    YZX,
    ZXY
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderGltf.class);

  private static final String GML_ID = "gml_id";
  public static final String ID = "id";
  private static final String LOD_1_SOLID = "lod1Solid";
  private static final String LOD_2_SOLID = "lod2Solid";
  private static final String SURFACES = "surfaces";
  private static final String SURFACE_TYPE = "surfaceType";
  private static final String LOD_2_MULTI_SURFACE = "lod2MultiSurface";
  private static final String CONSISTS_OF_BUILDING_PART = "consistsOfBuildingPart";

  private static final int ARRAY_BUFFER = 34962;
  private static final int ELEMENT_ARRAY_BUFFER = 34963;

  private static final int UNSIGNED_SHORT = 5123;
  private static final int FLOAT = 5126;

  private static final int TRIANGLES = 4;

  private static final byte[] MAGIC = new byte[] {0x67, 0x6c, 0x54, 0x46};
  private static final byte[] VERSION = new byte[] {0x02, 0x00, 0x00, 0x00};
  private static final byte[] JSON = new byte[] {0x4a, 0x53, 0x4f, 0x4e};
  private static final byte[] BIN = new byte[] {0x42, 0x49, 0x4e, 0x00};
  private static final byte[] JSON_PADDING = new byte[] {0x20};
  private static final byte[] BIN_PADDING = new byte[] {0x00};

  private final FeatureTransformationContextGltf transformationContext;
  private final CrsTransformer crs84hToEcef;
  private final boolean clampToGround;
  private final OutputStream outputStream;
  private final Material wall;
  private final Material roof;
  private final Material ground;
  private final Material closure;

  private ImmutableGltfAsset.Builder builder;
  private int nodeId;
  private int meshId;
  private int accessorId;
  private int bytesIndices;
  private int bytesVectors;
  private ByteArrayOutputStream bufferIndices;
  private ByteArrayOutputStream bufferVertices;
  private boolean withNormals;
  private List<Integer> buildingNodes;
  private final long transformerStart = System.nanoTime();
  private long processingStart;
  private long featureCount;
  private long featuresDuration;

  public FeatureEncoderGltf(FeatureTransformationContextGltf transformationContext) {
    this.transformationContext = transformationContext;
    this.crs84hToEcef = transformationContext.getCrsTransformerCrs84hToEcef();
    this.clampToGround = transformationContext.getClampToGround();
    this.outputStream = new OutputStreamToByteConsumer(this::push);

    // TODO make configurable
    this.wall =
        ImmutableMaterial.builder()
            .pbrMetallicRoughness(
                ImmutablePbrMetallicRoughness.builder()
                    .baseColorFactor(ImmutableList.of(0.5f, 0.5f, 0.5f, 1.0f))
                    .metallicFactor(0.2f)
                    .roughnessFactor(1.0f)
                    .build())
            .name("Wall")
            .build();
    this.roof =
        ImmutableMaterial.builder()
            .pbrMetallicRoughness(
                ImmutablePbrMetallicRoughness.builder()
                    .baseColorFactor(ImmutableList.of(1.0f, 0.0f, 0.0f, 1.0f))
                    .metallicFactor(0.5f)
                    .roughnessFactor(0.5f)
                    .build())
            .name("Roof")
            .build();
    this.ground =
        ImmutableMaterial.builder()
            .pbrMetallicRoughness(
                ImmutablePbrMetallicRoughness.builder()
                    .baseColorFactor(ImmutableList.of(0.8f, 0.8f, 0.8f, 1.0f))
                    .metallicFactor(0.2f)
                    .roughnessFactor(1.0f)
                    .build())
            .name("Ground")
            .build();
    this.closure =
        ImmutableMaterial.builder()
            .pbrMetallicRoughness(
                ImmutablePbrMetallicRoughness.builder()
                    .baseColorFactor(ImmutableList.of(0.9f, 0.9f, 0.9f, 0.6f))
                    .metallicFactor(0.8f)
                    .roughnessFactor(0.1f)
                    .build())
            .name("Closure")
            .build();
  }

  @Override
  public FeatureGltf createFeature() {
    return ModifiableFeatureGltf.create();
  }

  @Override
  public PropertyGltf createProperty() {
    return ModifiablePropertyGltf.create();
  }

  @Override
  public void onStart(ModifiableContext context) {
    this.processingStart = System.nanoTime();
    if (transformationContext.isFeatureCollection() && LOGGER.isDebugEnabled()) {
      context.metadata().getNumberMatched().ifPresent(num -> LOGGER.debug("numberMatched {}", num));
      context
          .metadata()
          .getNumberReturned()
          .ifPresent(num -> LOGGER.debug("numberReturned {}", num));
    }

    initNewModel();
  }

  @Override
  public void onFeature(FeatureGltf feature) {
    long featureStart = System.nanoTime();
    feature
        .findPropertyByPath(CONSISTS_OF_BUILDING_PART)
        .ifPresentOrElse(
            buildingParts -> {
              String fid =
                  feature
                      .findPropertyByPath(GML_ID)
                      .map(PropertyGltf::getFirstValue)
                      .orElse(
                          feature
                              .findPropertyByPath(ID)
                              .map(PropertyGltf::getFirstValue)
                              .orElse(null));
              ArrayList<Integer> buildingPartNodes = new ArrayList<>();
              buildingParts
                  .getNestedProperties()
                  .forEach(
                      buildingPart -> {
                        String pid =
                            buildingPart
                                .findPropertyByPath(
                                    ImmutableList.of(CONSISTS_OF_BUILDING_PART, GML_ID))
                                .map(PropertyGltf::getFirstValue)
                                .orElse(
                                    buildingPart
                                        .findPropertyByPath(
                                            ImmutableList.of(CONSISTS_OF_BUILDING_PART, ID))
                                        .map(PropertyGltf::getFirstValue)
                                        .orElse(null));
                        if (transformationContext.getLod() == 1) {
                          buildingPart
                              .findPropertyByPath(
                                  ImmutableList.of(CONSISTS_OF_BUILDING_PART, LOD_1_SOLID))
                              .ifPresent(
                                  solidProperty -> {
                                    buildingPartNodes.add(nodeId);
                                    processSolid(pid, solidProperty);
                                  });
                        } else if (transformationContext.getLod() == 2) {
                          buildingPart
                              .findPropertyByPath(
                                  ImmutableList.of(CONSISTS_OF_BUILDING_PART, SURFACES))
                              .ifPresentOrElse(
                                  surfacesProperty -> {
                                    int initialNodeId = nodeId;
                                    processSemanticSurfaces(pid, surfacesProperty, true);
                                    if (nodeId > initialNodeId) {
                                      buildingPartNodes.add(nodeId);
                                      builder.addNodes(
                                          ImmutableNode.builder()
                                              .name(
                                                  Optional.ofNullable(
                                                      pid)) // TODO add proper name, if there is one
                                              .addChildren(
                                                  IntStream.range(initialNodeId, nodeId).toArray())
                                              .build());
                                      nodeId++;
                                    }
                                  },
                                  () ->
                                      buildingPart
                                          .findPropertyByPath(
                                              ImmutableList.of(
                                                  CONSISTS_OF_BUILDING_PART, LOD_2_SOLID))
                                          .ifPresent(
                                              solidProperty -> {
                                                buildingPartNodes.add(nodeId);
                                                processSolid(pid, solidProperty);
                                              }));
                        }
                      });
              if (!buildingPartNodes.isEmpty()) {
                buildingNodes.add(nodeId);
                builder.addNodes(
                    ImmutableNode.builder()
                        .name(Optional.ofNullable(fid)) // TODO add proper name, if there is one
                        .children(buildingPartNodes)
                        .build());
                nodeId++;
              }
            },
            () -> {
              String fid =
                  feature
                      .findPropertyByPath(GML_ID)
                      .map(PropertyGltf::getFirstValue)
                      .orElse(
                          feature
                              .findPropertyByPath(ID)
                              .map(PropertyGltf::getFirstValue)
                              .orElse(null));
              if (transformationContext.getLod() == 1) {
                feature
                    .findPropertyByPath(LOD_1_SOLID)
                    .ifPresent(
                        solidProperty -> {
                          buildingNodes.add(nodeId);
                          processSolid(fid, solidProperty);
                        });
              } else if (transformationContext.getLod() == 2) {
                feature
                    .findPropertyByPath(SURFACES)
                    .ifPresentOrElse(
                        surfacesProperty -> {
                          int initialNodeId = nodeId;
                          processSemanticSurfaces(fid, surfacesProperty, false);
                          if (nodeId > initialNodeId) {
                            buildingNodes.add(nodeId);
                            builder.addNodes(
                                ImmutableNode.builder()
                                    .name(
                                        Optional.ofNullable(
                                            fid)) // TODO add proper name, if there is one
                                    .addChildren(IntStream.range(initialNodeId, nodeId).toArray())
                                    .build());
                            nodeId++;
                          }
                        },
                        () ->
                            feature
                                .findPropertyByPath(LOD_2_SOLID)
                                .ifPresent(
                                    solidProperty -> {
                                      buildingNodes.add(nodeId);
                                      processSolid(fid, solidProperty);
                                    }));
              }
            });
    this.featuresDuration += System.nanoTime() - featureStart;
    this.featureCount++;
  }

  @Override
  public void onEnd(ModifiableContext context) {
    long writingStart = System.nanoTime();
    finalizeModel();

    if (LOGGER.isDebugEnabled()) {
      long transformerDuration = toMilliseconds(System.nanoTime() - transformerStart);
      long processingDuration = toMilliseconds(System.nanoTime() - processingStart);
      long writingDuration = toMilliseconds(System.nanoTime() - writingStart);
      String text =
          String.format(
              "glTF features returned: %d, total duration: %dms, processing: %dms, feature processing: %dms, average feature processing: %dms, writing: %dms.",
              featureCount,
              transformerDuration,
              processingDuration,
              toMilliseconds(featuresDuration),
              featureCount == 0 ? 0 : toMilliseconds(featuresDuration / featureCount),
              writingDuration);
      if (processingDuration > 200) {
        LOGGER.debug(text);
      } else if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(text);
      }
    }
  }

  private long toMilliseconds(long nanoseconds) {
    return nanoseconds / 1_000_000;
  }

  private void processSolid(String fid, PropertyGltf solidProperty) {
    try {
      addMultiPolygon(fid, solidProperty, 0, null, null);
    } catch (Exception e) {
      LOGGER.error(
          "Error while processing property '{}' of building '{}', the property is ignored: {}",
          solidProperty.getName(),
          fid,
          e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Stacktrace: ", e);
      }
    }
  }

  private void processSemanticSurfaces(
      String fid, PropertyGltf surfacesProperty, boolean inBuildingPart) {
    // if we clamp the geometry to the ground, we need to determine the height correction for the
    // whole building or building part
    Double minZ = null;
    double[] originBuilding = null;
    if (clampToGround) {
      List<Coordinate> coords =
          surfacesProperty.getNestedProperties().stream()
              .map(
                  surface ->
                      surface.findPropertyByPath(
                          inBuildingPart
                              ? ImmutableList.of(
                                  CONSISTS_OF_BUILDING_PART, SURFACES, LOD_2_MULTI_SURFACE)
                              : ImmutableList.of(SURFACES, LOD_2_MULTI_SURFACE)))
              .flatMap(Optional::stream)
              .map(this::getMultiPolygon)
              .map(MultiPolygon::getCoordinatesFlat)
              .flatMap(Collection::stream)
              .collect(Collectors.toUnmodifiableList());
      double minX = coords.stream().mapToDouble(c -> c.get(0)).min().orElseThrow();
      double maxX = coords.stream().mapToDouble(c -> c.get(0)).max().orElseThrow();
      double minY = coords.stream().mapToDouble(c -> c.get(1)).min().orElseThrow();
      double maxY = coords.stream().mapToDouble(c -> c.get(1)).max().orElseThrow();
      minZ = coords.stream().mapToDouble(c -> c.get(2)).min().orElseThrow();
      double maxZ = coords.stream().mapToDouble(c -> c.get(2)).max().orElseThrow();
      originBuilding = new double[] {(minX + maxX) / 2.0, (minY + maxY) / 2.0, 0.0};
    }

    for (PropertyGltf surface : surfacesProperty.getNestedProperties()) {
      Optional<PropertyGltf> surfaceType =
          surface.findPropertyByPath(
              inBuildingPart
                  ? ImmutableList.of(CONSISTS_OF_BUILDING_PART, SURFACES, SURFACE_TYPE)
                  : ImmutableList.of(SURFACES, SURFACE_TYPE));
      int matId;
      String name = surfaceType.map(PropertyGltf::getFirstValue).orElse("wall");
      switch (name) {
          // TODO: support other semantic surface types
        default:
        case "wall":
          matId = 0;
          break;
        case "roof":
          matId = 1;
          break;
        case "ground":
          matId = 2;
          break;
        case "closure":
          matId = 3;
          break;
      }
      double[] finalOriginBuilding = originBuilding;
      Double finalMinZ = minZ;
      surface
          .findPropertyByPath(
              inBuildingPart
                  ? ImmutableList.of(CONSISTS_OF_BUILDING_PART, SURFACES, LOD_2_MULTI_SURFACE)
                  : ImmutableList.of(SURFACES, LOD_2_MULTI_SURFACE))
          .ifPresent(
              s -> {
                try {
                  addMultiPolygon(
                      name.substring(0, 1).toUpperCase() + name.substring(1),
                      s,
                      matId,
                      finalOriginBuilding,
                      finalMinZ);
                } catch (IOException e) {
                  LOGGER.error(
                      "Error while processing property 'lod2MultiSurface' of building '{}', the property is ignored: {}",
                      fid,
                      e.getMessage());
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Stacktrace: ", e);
                  }
                }
              });
    }
  }

  private void initNewModel() {
    builder =
        ImmutableGltfAsset.builder()
            .asset(
                ImmutableAssetMetadata.builder()
                    .copyright(
                        transformationContext
                            .getApiData()
                            .getMetadata()
                            .flatMap(ApiMetadata::getAttribution))
                    .build())
            .addMaterials(wall, roof, ground, closure);
    nodeId = 0;
    meshId = 0;
    accessorId = 0;
    bytesIndices = 0;
    bytesVectors = 0;
    bufferIndices = new ByteArrayOutputStream();
    bufferVertices = new ByteArrayOutputStream();
    withNormals =
        Boolean.TRUE.equals(
            transformationContext.getConfiguration(GltfConfiguration.class).getWithNormals());
    buildingNodes = new ArrayList<>();
  }

  private void finalizeModel() {
    builder.addBufferViews(
        ImmutableBufferView.builder()
            .buffer(0) // only one buffer
            .byteLength(bytesIndices)
            .byteOffset(0)
            .target(ELEMENT_ARRAY_BUFFER)
            .build());
    builder.addBufferViews(
        ImmutableBufferView.builder()
            .buffer(0) // only one buffer
            .byteLength(bytesVectors)
            .byteOffset(bytesIndices)
            .byteStride(3 * 4)
            .target(ARRAY_BUFFER)
            .build());
    int bufferLength = bufferIndices.size() + bufferVertices.size();
    builder.addBuffers(ImmutableBuffer.builder().byteLength(bufferLength).build());
    builder.addNodes(
        ImmutableNode.builder()
            .children(buildingNodes)
            // z-up (CRS) => y-up (glTF uses y-up)
            .addMatrix(
                1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
            .build());
    builder.addScenes(ImmutableScene.builder().nodes(ImmutableList.of(nodeId)).build());

    byte[] json;
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new Jdk8Module());
      // for debugging
      // mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
      json = mapper.writeValueAsBytes(builder.build());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          String.format("Could not write glTF asset. Reason: %s", e.getMessage()));
    }

    int jsonPadding = (4 - json.length % 4) % 4;
    int bufferPadding = (4 - bufferLength % 4) % 4;
    int totalLength = 12 + 8 + json.length + jsonPadding + 8 + bufferLength + bufferPadding;
    try {
      outputStream.write(MAGIC);
      outputStream.write(VERSION);
      outputStream.write(intToLittleEndianInt(totalLength));

      outputStream.write(intToLittleEndianInt(json.length + jsonPadding));
      outputStream.write(JSON);
      outputStream.write(json);
      for (int i = 0; i < jsonPadding; i++) {
        outputStream.write(JSON_PADDING);
      }

      outputStream.write(intToLittleEndianInt(bufferLength + bufferPadding));
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

  // TODO support other geometric primitives

  private void addMultiPolygon(
      String name,
      PropertyGltf geometryProperty,
      int materialId,
      double[] originBuilding,
      Double minZ)
      throws IOException {
    Geometry.MultiPolygon geometry = getMultiPolygon(geometryProperty);
    List<Double> vertices = new ArrayList<>();
    List<Double> normals = new ArrayList<>();
    List<Integer> indices = new ArrayList<>();
    int indexCount = 0;
    double area = 0.0;
    double area01;
    double area20;
    double area12;
    AXES axes = AXES.XYZ;
    int num;
    List<Double> data = new ArrayList<>();
    List<Integer> holeIndices = new ArrayList<>();
    // triangulate the polygons, translate relative to origin
    for (Geometry.Polygon polygon : geometry.getCoordinates()) {
      num = 0;
      data.clear();
      holeIndices.clear();

      // change axis order, if we have a vertical polygon; ensure we still have a right-handed CRS
      for (Geometry.LineString ring : polygon.getCoordinates()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Ring: {}", ring);
        }
        List<Geometry.Coordinate> coords = ring.getCoordinates();
        if (num == 0) {
          area01 = computeArea(coords, 0, 1);
          area20 = computeArea(coords, 2, 0);
          area12 = computeArea(coords, 1, 2);
          if (Math.abs(area01) > 0.0
              && Math.abs(area01) > Math.abs(area20)
              && Math.abs(area01) > Math.abs(area12)) {
            axes = AXES.XYZ;
            area = area01;
          } else if (Math.abs(area20) > 0.0
              && Math.abs(area20) > Math.abs(area01)
              && Math.abs(area20) > Math.abs(area12)) {
            axes = AXES.ZXY;
            area = area20;
          } else if (Math.abs(area12) > 0.0
              && Math.abs(area12) > Math.abs(area01)
              && Math.abs(area12) > Math.abs(area20)) {
            axes = AXES.YZX;
            area = area12;
          } else {
            break;
          }
          if (LOGGER.isTraceEnabled()) {
            LOGGER.debug("Area: {}, axes: {}", area, axes.name());
          }
        } else {
          holeIndices.add(data.size() / 3);
        }

        if (withNormals) {
          Geometry.Coordinate normal = computeNormal(coords);
          for (int i = 0; i < coords.size() - 1; i++) {
            normals.addAll(normal);
          }
        }

        if (axes == AXES.ZXY) {
          coords =
              coords.stream()
                  .map(coord -> Geometry.Coordinate.of(coord.get(2), coord.get(0), coord.get(1)))
                  .collect(Collectors.toUnmodifiableList());
        } else if (axes == AXES.YZX) {
          coords =
              coords.stream()
                  .map(coord -> Geometry.Coordinate.of(coord.get(1), coord.get(2), coord.get(0)))
                  .collect(Collectors.toUnmodifiableList());
        }
        data.addAll(getDoubleList(coords));

        num++;
      }
      if (data.isEmpty()) {
        continue;
      }

      List<Integer> triangles =
          data.size() > 9
              ? Earcut.earcut(
                  data.stream().mapToDouble(Double::doubleValue).toArray(),
                  holeIndices.isEmpty()
                      ? null
                      : holeIndices.stream().mapToInt(Integer::intValue).toArray(),
                  3)
              : ImmutableList.of(0, 1, 2);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Triangles: {}", triangles);
        LOGGER.trace("Data: {}, {}", data, holeIndices);
      }
      if (triangles.isEmpty()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Cannot triangulate a polygon, the polygon is ignored: {}", data);
        }

        continue;
      }

      for (int i = 0; i < triangles.size() / 3; i++) {
        Integer p0 = triangles.get(i * 3);
        Integer p1 = triangles.get(i * 3 + 1);
        Integer p2 = triangles.get(i * 3 + 2);
        ImmutableList<Geometry.Coordinate> triangle =
            ImmutableList.of(
                Geometry.Coordinate.of(data.subList(p0 * 3, p0 * 3 + 2)),
                Geometry.Coordinate.of(data.subList(p1 * 3, p1 * 3 + 2)),
                Geometry.Coordinate.of(data.subList(p2 * 3, p2 * 3 + 2)));
        double triangleArea = computeAreaTriangle(triangle);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Triangle {}: {}, {}", i + 1, triangleArea, triangle);
        }
        if (area * triangleArea * (num == 1 ? 1.0 : -1.0) < 0) {
          // switch orientation
          triangles.set(i * 3, p2);
          triangles.set(i * 3 + 2, p0);
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SWITCHING ORIENTATION");
          }
        }
      }
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Updated triangles: {}", triangles);
      }

      // back to XYZ axis order
      if (axes == AXES.XYZ) {
        vertices.addAll(data);
      } else if (axes == AXES.ZXY) {
        IntStream.range(0, data.size() / 3)
            .forEach(
                n -> {
                  vertices.add(data.get(n * 3 + 1));
                  vertices.add(data.get(n * 3 + 2));
                  vertices.add(data.get(n * 3));
                });
      } else if (axes == AXES.YZX) {
        IntStream.range(0, data.size() / 3)
            .forEach(
                n -> {
                  vertices.add(data.get(n * 3 + 2));
                  vertices.add(data.get(n * 3));
                  vertices.add(data.get(n * 3 + 1));
                });
      }

      for (int ringIndex : triangles) {
        indices.add(indexCount + ringIndex);
      }

      indexCount += data.size() / 3;
    }

    if (indices.size() > 0) {

      double[] originFeature;
      if (Objects.isNull(originBuilding)) {
        // translate vertices to the center of the feature to have smaller values (glTF uses float)
        ImmutableList<Double> min =
            ImmutableList.of(
                IntStream.range(0, vertices.size() / 3)
                    .filter(n -> n % 3 == 0)
                    .mapToDouble(n -> vertices.get(n * 3))
                    .min()
                    .orElseThrow(),
                IntStream.range(0, vertices.size() / 3)
                    .filter(n -> n % 3 == 1)
                    .mapToDouble(n -> vertices.get(n * 3 + 1))
                    .min()
                    .orElseThrow(),
                IntStream.range(0, vertices.size() / 3)
                    .filter(n -> n % 3 == 2)
                    .mapToDouble(n -> vertices.get(n * 3 + 2))
                    .min()
                    .orElseThrow());
        ImmutableList<Double> max =
            ImmutableList.of(
                IntStream.range(0, vertices.size() / 3)
                    .filter(n -> n % 3 == 0)
                    .mapToDouble(n -> vertices.get(n * 3))
                    .max()
                    .orElseThrow(),
                IntStream.range(0, vertices.size() / 3)
                    .filter(n -> n % 3 == 1)
                    .mapToDouble(n -> vertices.get(n * 3 + 1))
                    .max()
                    .orElseThrow(),
                IntStream.range(0, vertices.size() / 3)
                    .filter(n -> n % 3 == 2)
                    .mapToDouble(n -> vertices.get(n * 3 + 2))
                    .max()
                    .orElseThrow());

        originFeature =
            new double[] {
              (min.get(0) + max.get(0)) / 2.0,
              (min.get(1) + max.get(1)) / 2.0,
              clampToGround ? 0.0 : (min.get(2) + max.get(2)) / 2.0
            };
      } else {
        originFeature = originBuilding;
      }
      double[] originFeatureEcef = crs84hToEcef.transform(originFeature, 1, 3);
      double[] tmp =
          crs84hToEcef.transform(
              IntStream.range(0, vertices.size())
                  .mapToDouble(
                      n ->
                          (clampToGround && n % 3 == 2)
                              ? vertices.get(n) - Objects.requireNonNull(minZ)
                              : vertices.get(n))
                  .toArray(),
              vertices.size() / 3,
              3);
      List<Double> finalVertices =
          IntStream.range(0, tmp.length)
              .mapToObj(n -> tmp[n] - originFeatureEcef[n % 3])
              .collect(Collectors.toUnmodifiableList());

      if (LOGGER.isTraceEnabled()) {
        for (int i = 0; i < indices.size() / 3; i++) {
          Integer p0 = indices.get(i * 3);
          Integer p1 = indices.get(i * 3 + 1);
          Integer p2 = indices.get(i * 3 + 2);

          LOGGER.trace("Indices: {},{},{}", p0, p1, p2);
          LOGGER.trace(
              "Triangle: ({},{},{}) ({},{},{}) ({},{},{}) - ({},{},{})",
              finalVertices.get(p0 * 3),
              finalVertices.get(p0 * 3 + 1),
              finalVertices.get(p0 * 3 + 2),
              finalVertices.get(p1 * 3),
              finalVertices.get(p1 * 3 + 1),
              finalVertices.get(p1 * 3 + 2),
              finalVertices.get(p2 * 3),
              finalVertices.get(p2 * 3 + 1),
              finalVertices.get(p2 * 3 + 2),
              normals.get(p0 * 3),
              normals.get(p0 * 3 + 1),
              normals.get(p0 * 3 + 2));
        }
      }

      for (int v : indices) {
        bufferIndices.write(intToLittleEndianShort(v));
      }

      int accessorIdIndices = accessorId;
      builder.addAccessors(
          ImmutableAccessor.builder()
              .bufferView(0)
              .byteOffset(bytesIndices)
              .componentType(UNSIGNED_SHORT)
              .addMax(indices.stream().max(Comparator.naturalOrder()).orElseThrow())
              .addMin(indices.stream().min(Comparator.naturalOrder()).orElseThrow())
              .count(indices.size())
              .type("SCALAR")
              .build());
      bytesIndices += indices.size() * 2;
      accessorId++;

      if (indices.size() % 2 == 1) {
        // pad for alignment, all offsets must be divisible by 4
        bufferIndices.write(BIN_PADDING);
        bufferIndices.write(BIN_PADDING);
        bytesIndices += 2;
      }

      builder.addAccessors(
          ImmutableAccessor.builder()
              .bufferView(1)
              .byteOffset(bytesVectors)
              .componentType(FLOAT)
              .addAllMax(getMax(finalVertices))
              .addAllMin(getMin(finalVertices))
              .count(finalVertices.size() / 3)
              .type("VEC3")
              .build());
      accessorId++;
      bytesVectors += finalVertices.size() * 4;
      if (withNormals) {
        builder.addAccessors(
            ImmutableAccessor.builder()
                .bufferView(1)
                .byteOffset(bytesVectors)
                .componentType(FLOAT)
                .addAllMax(getMax(normals))
                .addAllMin(getMin(normals))
                .count(normals.size() / 3)
                .type("VEC3")
                .build());
        accessorId++;
        bytesVectors += vertices.size() * 4;
      }

      builder.addMeshes(
          ImmutableMesh.builder()
              .addPrimitives(
                  ImmutablePrimitive.builder()
                      .attributes(
                          withNormals
                              ? ImmutableAttributes.builder()
                                  .position(accessorIdIndices + 1)
                                  .normal(accessorIdIndices + 2)
                                  .build()
                              : ImmutableAttributes.builder()
                                  .position(accessorIdIndices + 1)
                                  .build())
                      .mode(TRIANGLES)
                      .indices(accessorIdIndices)
                      .material(materialId)
                      .build())
              .build());
      builder.addNodes(
          ImmutableNode.builder()
              .name(Optional.ofNullable(name))
              .mesh(meshId)
              .translation(
                  ImmutableList.of(
                      originFeatureEcef[0], originFeatureEcef[1], originFeatureEcef[2]))
              .build());
      meshId++;
      nodeId++;

      for (double v : finalVertices) {
        bufferVertices.write(doubleToLittleEndianFloat(v));
      }

      if (withNormals) {
        for (double v : normals) {
          bufferVertices.write(doubleToLittleEndianFloat(v));
        }
      }
    }
  }

  private Geometry.MultiPolygon getMultiPolygon(PropertyGltf geometryProperty) {
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
                                        getCoordinates(ring.getNestedProperties())))
                            .collect(Collectors.toUnmodifiableList())))
            .collect(Collectors.toUnmodifiableList()));
  }

  private static byte[] intToLittleEndianInt(int v) {
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(v);
    return bb.array();
  }

  private static byte[] intToLittleEndianShort(int v) {
    ByteBuffer bb = ByteBuffer.allocate(2);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putShort((short) v);
    return bb.array();
  }

  private static byte[] doubleToLittleEndianFloat(double v) {
    ByteBuffer bb = ByteBuffer.allocate(4);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putFloat((float) v);
    return bb.array();
  }

  private double computeArea(List<Geometry.Coordinate> ring, int axis1, int axis2) {
    return (IntStream.range(0, ring.size() - 1)
                .mapToDouble(n -> ring.get(n).get(axis1) * ring.get(n + 1).get(axis2))
                .sum()
            - IntStream.range(0, ring.size() - 1)
                .mapToDouble(n -> ring.get(n + 1).get(axis1) * ring.get(n).get(axis2))
                .sum())
        / 2;
  }

  private double computeAreaTriangle(List<Geometry.Coordinate> triangle) {
    return (triangle.get(0).get(0) * (triangle.get(1).get(1) - triangle.get(2).get(1))
            + triangle.get(1).get(0) * (triangle.get(2).get(1) - triangle.get(0).get(1))
            + triangle.get(2).get(0) * (triangle.get(0).get(1) - triangle.get(1).get(1)))
        / 2.0d;
  }

  private Geometry.Coordinate computeNormal(List<Geometry.Coordinate> ring) {
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

  private List<Double> getMin(List<Double> vertices) {
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

  private List<Double> getMax(List<Double> vertices) {
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

  private Geometry.Coordinate getCoordinate(List<PropertyGltf> coordList) {
    return Geometry.Coordinate.of(
        coordList.stream()
            .map(PropertyBase::getValue)
            .filter(Objects::nonNull)
            .map(Double::parseDouble)
            .collect(Collectors.toUnmodifiableList()));
  }

  private List<Geometry.Coordinate> getCoordinates(List<PropertyGltf> coordsList) {
    return coordsList.stream()
        .map(coord -> Geometry.Coordinate.of(getCoordinate(coord.getNestedProperties())))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Double> getDoubleList(List<Geometry.Coordinate> coordinates) {
    // remove the last, redundant coordinate
    return coordinates.subList(0, coordinates.size() - 1).stream()
        .flatMap(List::stream)
        .collect(Collectors.toUnmodifiableList());
  }
}
