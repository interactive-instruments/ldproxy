/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import static de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry.MULTI_POLYGON;

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
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import de.ii.ogcapi.features.html.domain.Geometry.MultiPolygon;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import earcut4j.Earcut;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO cleanup, consolidate, harmonize code

public class FeatureEncoderGltf extends FeatureObjectEncoder<PropertyGltf, FeatureGltf> {

  private static final String KHR_MESH_QUANTIZATION = "KHR_mesh_quantization";
  private static final BigDecimal NUM_1_0 = new BigDecimal("1");
  private static final BigDecimal NUM_0_0 = new BigDecimal("0");
  private static final BigDecimal NUM_MINUS_1_0 = new BigDecimal("-1");
  private static final int INITIAL_SIZE = 1_024;

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

  private static final int BYTE = 5120;
  private static final int UNSIGNED_BYTE = 5121;
  private static final int SHORT = 5122;
  private static final int UNSIGNED_SHORT = 5123;
  private static final int FLOAT = 5126;

  private static final int TRIANGLES = 4;

  private final FeatureTransformationContextGltf transformationContext;
  private final CrsTransformer crs84hToEcef;
  private final boolean clampToGround;
  private final OutputStream outputStream;
  private final Map<String, Integer> matIdMap;
  private final boolean polygonOrientationIsNotGuaranteed;

  private ImmutableGltfAsset.Builder builder;

  private int nodeId;
  private int meshId;
  private int accessorId;
  private int bytesIndices;
  private int bytesVertices;
  private int bytesNormals;
  private ByteArrayOutputStream bufferIndices;
  private ByteArrayOutputStream bufferVertices;
  private ByteArrayOutputStream bufferNormals;
  private final boolean withNormals;
  private final boolean quantizeMesh;
  private List<Integer> buildingNodes;
  private final long transformerStart = System.nanoTime();
  private long processingStart;
  private long featureFetched;
  private long featureCount;
  private long featuresDuration;

  public FeatureEncoderGltf(FeatureTransformationContextGltf transformationContext) {
    this.transformationContext = transformationContext;
    this.crs84hToEcef = transformationContext.getCrsTransformerCrs84hToEcef();
    this.clampToGround = transformationContext.getClampToGround();
    this.outputStream = new OutputStreamToByteConsumer(this::push);
    this.withNormals =
        transformationContext.getConfiguration(GltfConfiguration.class).writeNormals();
    this.quantizeMesh =
        transformationContext.getConfiguration(GltfConfiguration.class).useMeshQuantization();
    this.polygonOrientationIsNotGuaranteed =
        transformationContext
            .getConfiguration(GltfConfiguration.class)
            .polygonOrientationIsNotGuaranteed();
    this.matIdMap = new HashMap<>();
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
    if (transformationContext.isFeatureCollection()) {
      featureFetched = context.metadata().getNumberReturned().orElseThrow();
      if (featureFetched > 10000) {
        LOGGER.warn("Fetching a large number of features for a tile: {}", featureFetched);
      }
    }
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
                                        .ifPresentOrElse(
                                            solidProperty -> {
                                              if (processSolid(pid, solidProperty)) {
                                                buildingPartNodes.add(nodeId - 1);
                                              }
                                            },
                                            () ->
                                                buildingPart
                                                    .findPropertyByPath(
                                                        ImmutableList.of(
                                                            CONSISTS_OF_BUILDING_PART, LOD_1_SOLID))
                                                    .ifPresent(
                                                        solidProperty -> {
                                                          if (processSolid(pid, solidProperty)) {
                                                            buildingPartNodes.add(nodeId - 1);
                                                          }
                                                        })));
                      });
              if (!buildingPartNodes.isEmpty()) {
                buildingNodes.add(nodeId);
                this.featureCount++;
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
              feature
                  .findPropertyByPath(SURFACES)
                  .ifPresentOrElse(
                      surfacesProperty -> {
                        int initialNodeId = nodeId;
                        processSemanticSurfaces(fid, surfacesProperty, false);
                        if (nodeId > initialNodeId) {
                          buildingNodes.add(nodeId);
                          this.featureCount++;
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
                              .ifPresentOrElse(
                                  solidProperty -> {
                                    if (processSolid(fid, solidProperty)) {
                                      buildingNodes.add(nodeId - 1);
                                      this.featureCount++;
                                    }
                                  },
                                  () ->
                                      feature
                                          .findPropertyByPath(LOD_1_SOLID)
                                          .ifPresent(
                                              solidProperty -> {
                                                if (processSolid(fid, solidProperty)) {
                                                  buildingNodes.add(nodeId - 1);
                                                  this.featureCount++;
                                                }
                                              })));
            });
    this.featuresDuration += System.nanoTime() - featureStart;
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
              "glTF features fetched: %d, returned: %d, total duration: %dms, processing: %dms, feature processing: %dms, average feature processing: %dms, writing: %dms.",
              featureFetched,
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

  private boolean processSolid(String fid, PropertyGltf solidProperty) {
    try {
      return addMultiPolygon(fid, solidProperty, getMaterialId("wall"), null, null);
    } catch (Exception e) {
      LOGGER.error(
          "Error while processing property '{}' of feature '{}', the property is ignored: {}",
          solidProperty.getName(),
          fid,
          e.getMessage());
      if (LOGGER.isDebugEnabled(MARKER.STACKTRACE)) {
        LOGGER.debug("Stacktrace: ", e);
      }
      return false;
    }
  }

  private void processSemanticSurfaces(
      String fid, PropertyGltf surfacesProperty, boolean inBuildingPart) {
    // if we clamp the geometry to the ground, we need to determine the height correction for the
    // whole building or building part
    double[] minBuilding = null;
    double[] maxBuilding = null;
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
      minBuilding =
          new double[] {
            coords.stream().mapToDouble(c -> c.get(0)).min().orElseThrow(),
            coords.stream().mapToDouble(c -> c.get(1)).min().orElseThrow(),
            coords.stream().mapToDouble(c -> c.get(2)).min().orElseThrow()
          };
      maxBuilding =
          new double[] {
            coords.stream().mapToDouble(c -> c.get(0)).max().orElseThrow(),
            coords.stream().mapToDouble(c -> c.get(1)).max().orElseThrow(),
            coords.stream().mapToDouble(c -> c.get(2)).max().orElseThrow()
          };
    }

    final double[] finalMinBuilding = minBuilding;
    final double[] finalMaxBuilding = maxBuilding;
    for (PropertyGltf surface : surfacesProperty.getNestedProperties()) {
      Optional<PropertyGltf> surfaceType =
          surface.findPropertyByPath(
              inBuildingPart
                  ? ImmutableList.of(CONSISTS_OF_BUILDING_PART, SURFACES, SURFACE_TYPE)
                  : ImmutableList.of(SURFACES, SURFACE_TYPE));
      String name =
          surfaceType.map(PropertyGltf::getFirstValue).map(String::toLowerCase).orElse("wall");
      // TODO: support other semantic surface types
      int matId = getMaterialId(name);
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
                      finalMinBuilding,
                      finalMaxBuilding);
                } catch (IOException e) {
                  LOGGER.error(
                      "Error while processing property 'lod2MultiSurface' of feature '{}', the property is ignored: {}",
                      fid,
                      e.getMessage());
                  if (LOGGER.isDebugEnabled(MARKER.STACKTRACE)) {
                    LOGGER.debug("Stacktrace: ", e);
                  }
                }
              });
    }
  }

  private int getMaterialId(String name) {
    return matIdMap.computeIfAbsent(name, val -> matIdMap.size());
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
                    .build());
    nodeId = 0;
    meshId = 0;
    accessorId = 0;
    bytesIndices = 0;
    bytesVertices = 0;
    bytesNormals = 0;
    bufferIndices = new ByteArrayOutputStream(getByteStrideIndices() * INITIAL_SIZE);
    bufferVertices = new ByteArrayOutputStream(getByteStrideVertices() * INITIAL_SIZE);
    bufferNormals = new ByteArrayOutputStream(getByteStrideNormals() * INITIAL_SIZE);
    buildingNodes = new ArrayList<>(INITIAL_SIZE);
  }

  private void finalizeModel() {

    // TODO make materials configurable
    matIdMap.entrySet().stream()
        .sorted(Comparator.comparingInt(Entry::getValue))
        .map(
            entry -> {
              switch (entry.getKey()) {
                case "wall":
                  return ImmutableMaterial.builder()
                      .pbrMetallicRoughness(
                          ImmutablePbrMetallicRoughness.builder()
                              .baseColorFactor(ImmutableList.of(0.5f, 0.5f, 0.5f, 1.0f))
                              .metallicFactor(0.2f)
                              .roughnessFactor(1.0f)
                              .build())
                      .name("Wall")
                      .doubleSided(polygonOrientationIsNotGuaranteed)
                      .build();
                case "roof":
                  return ImmutableMaterial.builder()
                      .pbrMetallicRoughness(
                          ImmutablePbrMetallicRoughness.builder()
                              .baseColorFactor(ImmutableList.of(1.0f, 0.0f, 0.0f, 1.0f))
                              .metallicFactor(0.5f)
                              .roughnessFactor(0.5f)
                              .build())
                      .name("Roof")
                      .doubleSided(polygonOrientationIsNotGuaranteed)
                      .build();
                case "ground":
                  return ImmutableMaterial.builder()
                      .pbrMetallicRoughness(
                          ImmutablePbrMetallicRoughness.builder()
                              .baseColorFactor(ImmutableList.of(0.8f, 0.8f, 0.8f, 1.0f))
                              .metallicFactor(0.2f)
                              .roughnessFactor(1.0f)
                              .build())
                      .name("Ground")
                      .doubleSided(polygonOrientationIsNotGuaranteed)
                      .build();
                case "closure":
                  return ImmutableMaterial.builder()
                      .pbrMetallicRoughness(
                          ImmutablePbrMetallicRoughness.builder()
                              .baseColorFactor(ImmutableList.of(0.9f, 0.9f, 0.9f, 0.6f))
                              .metallicFactor(0.8f)
                              .roughnessFactor(0.1f)
                              .build())
                      .name("Closure")
                      .doubleSided(polygonOrientationIsNotGuaranteed)
                      .build();
                default:
                  return ImmutableMaterial.builder()
                      .pbrMetallicRoughness(
                          ImmutablePbrMetallicRoughness.builder()
                              .baseColorFactor(ImmutableList.of(0.5f, 0.5f, 0.5f, 1.0f))
                              .metallicFactor(0.2f)
                              .roughnessFactor(1.0f)
                              .build())
                      .name(entry.getKey())
                      .doubleSided(polygonOrientationIsNotGuaranteed)
                      .build();
              }
            })
        .forEach(mat -> builder.addMaterials(mat));

    if (bytesIndices > 0) {
      builder.addBufferViews(
          ImmutableBufferView.builder()
              .buffer(0) // only one buffer
              .byteLength(bytesIndices)
              .byteOffset(0)
              .target(ELEMENT_ARRAY_BUFFER)
              .build());
    }
    if (bytesVertices > 0) {
      builder.addBufferViews(
          ImmutableBufferView.builder()
              .buffer(0) // only one buffer
              .byteLength(bytesVertices)
              .byteOffset(bytesIndices)
              .byteStride(getByteStrideVertices())
              .target(ARRAY_BUFFER)
              .build());
    }
    if (bytesNormals > 0) {
      builder.addBufferViews(
          ImmutableBufferView.builder()
              .buffer(0) // only one buffer
              .byteLength(bytesNormals)
              .byteOffset(bytesIndices + bytesVertices)
              .byteStride(getByteStrideNormals())
              .target(ARRAY_BUFFER)
              .build());
    }
    int bufferLength = bufferIndices.size() + bufferVertices.size() + bufferNormals.size();
    if (bufferLength > 0) {
      builder.addBuffers(ImmutableBuffer.builder().byteLength(bufferLength).build());
      builder.addNodes(
          ImmutableNode.builder()
              .children(buildingNodes)
              // z-up (CRS) => y-up (glTF uses y-up)
              .addMatrix(1d, 0d, 0d, 0d, 0d, 0d, -1d, 0d, 0d, 1d, 0d, 0d, 0d, 0d, 0d, 1d)
              .build());
      builder.addScenes(ImmutableScene.builder().nodes(ImmutableList.of(nodeId)).build());
    }

    if (quantizeMesh) {
      builder.addExtensionsUsed(KHR_MESH_QUANTIZATION);
      builder.addExtensionsRequired(KHR_MESH_QUANTIZATION);
    }

    GltfHelper.writeGltfBinary(
        builder.build(), bufferIndices, bufferVertices, bufferNormals, outputStream);
  }

  private int getByteStrideIndices() {
    return 2;
  }

  private int getByteStrideVertices() {
    return (quantizeMesh ? 2 : 3) * 4;
  }

  private int getByteStrideNormals() {
    return (quantizeMesh ? 1 : 3) * 4;
  }

  // TODO support other geometric primitives

  private boolean addMultiPolygon(
      String name,
      PropertyGltf geometryProperty,
      int materialId,
      double[] minBuilding,
      double[] maxBuilding)
      throws IOException {

    // determine the origin to use; eventually we will translate vertices to the center of the
    // feature to have smaller values (glTF uses float)
    Geometry.MultiPolygon geometry = getMultiPolygon(geometryProperty);
    List<Coordinate> coordList = geometry.getCoordinatesFlat();
    double[] min =
        Objects.requireNonNullElse(
            minBuilding,
            new double[] {
              coordList.stream().mapToDouble(coord -> coord.get(0)).min().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(1)).min().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(2)).min().orElseThrow()
            });
    double[] max =
        Objects.requireNonNullElse(
            maxBuilding,
            new double[] {
              coordList.stream().mapToDouble(coord -> coord.get(0)).max().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(1)).max().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(2)).max().orElseThrow()
            });
    double[] originEcef =
        Arrays.stream(
                crs84hToEcef.transform(
                    new double[] {
                      (min[0] + max[0]) / 2.0, (min[1] + max[1]) / 2.0, (min[2] + max[2]) / 2.0
                    },
                    1,
                    3))
            .toArray();

    List<Double> vertices = new ArrayList<>();
    List<Double> normals = new ArrayList<>();
    List<Integer> indices = new ArrayList<>();
    int indexCount = 0;

    // triangulate the polygons, translate relative to origin
    int numRing;
    AXES axes = AXES.XYZ;
    boolean ccw = true;
    List<Double> data = new ArrayList<>();
    List<Integer> holeIndices = new ArrayList<>();
    double area;
    for (Geometry.Polygon polygon : geometry.getCoordinates()) {
      numRing = 0;
      data.clear();
      holeIndices.clear();

      // change axis order, if we have a vertical polygon; ensure we still have a right-handed CRS
      for (Geometry.LineString ring : polygon.getCoordinates()) {
        List<Coordinate> coordsRing = ring.getCoordinates();

        // remove consecutive duplicate points
        coordList =
            IntStream.range(0, coordsRing.size())
                .filter(
                    n ->
                        n == 0
                            || !Objects.equals(
                                coordsRing.get(n).get(0), coordsRing.get(n - 1).get(0))
                            || !Objects.equals(
                                coordsRing.get(n).get(1), coordsRing.get(n - 1).get(1))
                            || !Objects.equals(
                                coordsRing.get(n).get(2), coordsRing.get(n - 1).get(2)))
                .mapToObj(coordsRing::get)
                .collect(Collectors.toUnmodifiableList());

        // skip a degenerated or colinear polygon (no area)
        if (coordList.size() < 4 || GltfHelper.find3rdPoint(coordList)[1] == -1) {
          if (numRing == 0) {
            // skip polygon, if exterior boundary
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace(
                  "Skipping polygon of feature '{}', exterior ring has no effective area: {}",
                  name,
                  coordList);
            }
            break;
          } else {
            // skip ring, if a hole
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace(
                  "Skipping hole of feature '{}', interior ring has no effective area: {}",
                  name,
                  coordList);
            }
            continue;
          }
        }

        // do not copy the last point, same as first point
        double[] coords = new double[(coordList.size() - 1) * 3];
        for (int n = 0; n < coordList.size() - 1; n++) {
          coords[n * 3] = coordList.get(n).get(0);
          coords[n * 3 + 1] = coordList.get(n).get(1);
          coords[n * 3 + 2] =
              clampToGround ? coordList.get(n).get(2) - min[2] : coordList.get(n).get(2);
        }

        // transform to ECEF coordinates
        double[] coordsEcef = crs84hToEcef.transform(coords, coords.length / 3, 3);

        if (LOGGER.isTraceEnabled()) {
          if (!GltfHelper.isCoplanar(coordList)) {
            LOGGER.trace(
                "Feature '{}' has a ring that is not coplanar. The glTF mesh may be invalid. Coordinates: {}",
                name,
                coords);
          }
        }

        if (numRing == 0) {

          final double area01 = Math.abs(GltfHelper.computeArea(coordsEcef, 0, 1));
          final double area12 = Math.abs(GltfHelper.computeArea(coordsEcef, 1, 2));
          final double area20 = Math.abs(GltfHelper.computeArea(coordsEcef, 2, 0));
          if (area01 > area12 && area01 > area20) {
            axes = AXES.XYZ;
            area = area01;
          } else if (area12 > area20) {
            axes = AXES.YZX;
            area = area12;
          } else if (Math.abs(area20) > 0.0) {
            axes = AXES.ZXY;
            area = area20;
          } else {
            LOGGER.trace(
                "The area of the exterior ring is too small, the polygon is ignored: {} {} {} - {}",
                area01,
                area12,
                area20,
                coordsEcef);
            break;
          }
          ccw = area < 0;
          /*
          if (area < transformationContext.getMinArea()) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "The area of the exterior ring is smaller than {} square meters, the polygon is ignored: {} {} {}",
                  transformationContext.getMinArea(),
                  area01,
                  area12,
                  area20);
              break;
            }
          }
           */
        } else {
          /*
          area = axes==AXES.XYZ ? Math.abs(GltfHelper.computeArea(coordsEcef, 0, 1))
              : axes==AXES.YZX ? Math.abs(GltfHelper.computeArea(coordsEcef, 1, 2))
                  : Math.abs(GltfHelper.computeArea(coordsEcef, 2, 0));
          if (area < transformationContext.getMinArea()) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "The area of an interior ring is smaller than {} square meters, the hole is ignored: {}",
                  transformationContext.getMinArea(),
                  area);
              break;
            }
          }
           */
          holeIndices.add((data.size() / 3) + 1);
        }

        data.addAll(Arrays.stream(coordsEcef).boxed().collect(Collectors.toUnmodifiableList()));

        if (withNormals) {
          Geometry.Coordinate normal = GltfHelper.computeNormal(coordsEcef);
          if (Objects.isNull(normal)) {
            if (numRing == 0) {
              // skip polygon, if exterior boundary
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "Skipping polygon of feature '{}', could not compute normal for exterior ring: {}",
                    name,
                    coordList);
              }
              break;
            } else {
              // skip ring, if a hole
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "Skipping hole of feature '{}', could not compute normal for exterior ring: {}",
                    name,
                    coordList);
              }
              continue;
            }
          }
          for (int i = 0; i < coordsEcef.length / 3; i++) {
            normals.addAll(normal);
          }
        }

        numRing++;
      }

      if (data.size() < 9) {
        continue;
      }

      double[] tmp = new double[data.size() / 3 * 2];
      if (axes == AXES.XYZ) {
        for (int n = 0; n < data.size() / 3; n++) {
          tmp[n * 2] = data.get(n * 3);
          tmp[n * 2 + 1] = data.get(n * 3 + 1);
        }
      } else if (axes == AXES.YZX) {
        for (int n = 0; n < data.size() / 3; n++) {
          tmp[n * 2] = data.get(n * 3 + 1);
          tmp[n * 2 + 1] = data.get(n * 3 + 2);
        }
      } else {
        for (int n = 0; n < data.size() / 3; n++) {
          tmp[n * 2] = data.get(n * 3 + 2);
          tmp[n * 2 + 1] = data.get(n * 3);
        }
      }

      List<Integer> triangles =
          tmp.length > 6
              ? Earcut.earcut(
                  tmp,
                  holeIndices.isEmpty()
                      ? null
                      : holeIndices.stream().mapToInt(Integer::intValue).toArray(),
                  2)
              : new ArrayList<>(ImmutableList.of(0, 1, 2));

      /* TODO remove
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Triangles: {}", triangles);
        LOGGER.trace("Data: {}, {}", data, holeIndices);
      }
       */
      if (triangles.isEmpty()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Cannot triangulate a polygon of feature '{}', the polygon is ignored: {}",
              name,
              data);
        }

        continue;
      }

      for (int i = 0; i < triangles.size() / 3; i++) {
        Integer p0 = triangles.get(i * 3);
        Integer p1 = triangles.get(i * 3 + 1);
        Integer p2 = triangles.get(i * 3 + 2);
        ImmutableList<Geometry.Coordinate> triangle =
            ImmutableList.of(
                Geometry.Coordinate.of(
                    data.get(p0 * 3), data.get(p0 * 3 + 1), data.get(p0 * 3 + 2)),
                Geometry.Coordinate.of(
                    data.get(p1 * 3), data.get(p1 * 3 + 1), data.get(p1 * 3 + 2)),
                Geometry.Coordinate.of(
                    data.get(p2 * 3), data.get(p2 * 3 + 1), data.get(p2 * 3 + 2)));
        boolean ccwTriangle =
            axes == AXES.XYZ
                ? GltfHelper.computeAreaTriangle(triangle, 0, 1) < 0
                : axes == AXES.YZX
                    ? GltfHelper.computeAreaTriangle(triangle, 1, 2) < 0
                    : GltfHelper.computeAreaTriangle(triangle, 2, 0) < 0;
        boolean exterior = holeIndices.isEmpty() || p0 >= holeIndices.get(0);
        if ((exterior && ccwTriangle != ccw) || (!exterior && ccwTriangle == ccw)) {
          // switch orientation, if the triangle has the wrong orientation
          triangles.set(i * 3, p2);
          triangles.set(i * 3 + 2, p0);
          /* TODO remove
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Switching triangle orientation: {}", triangle);
          }
           */
        }
      }

      vertices.addAll(data);

      for (int ringIndex : triangles) {
        indices.add(indexCount + ringIndex);
      }

      indexCount += data.size() / 3;
    }

    if (indices.size() > 0) {

      if (LOGGER.isTraceEnabled()) {
        for (int i = 0; i < indices.size() / 3; i++) {
          Integer p0 = indices.get(i * 3);
          Integer p1 = indices.get(i * 3 + 1);
          Integer p2 = indices.get(i * 3 + 2);

          LOGGER.trace("Indices: {},{},{}", p0, p1, p2);
          LOGGER.trace(
              "Triangle: ({},{},{}) ({},{},{}) ({},{},{}) - ({},{},{})",
              vertices.get(p0 * 3),
              vertices.get(p0 * 3 + 1),
              vertices.get(p0 * 3 + 2),
              vertices.get(p1 * 3),
              vertices.get(p1 * 3 + 1),
              vertices.get(p1 * 3 + 2),
              vertices.get(p2 * 3),
              vertices.get(p2 * 3 + 1),
              vertices.get(p2 * 3 + 2),
              withNormals ? normals.get(p0 * 3) : "-",
              withNormals ? normals.get(p0 * 3 + 1) : "-",
              withNormals ? normals.get(p0 * 3 + 2) : "-");
        }
      }

      for (int v : indices) {
        bufferIndices.write(GltfHelper.intToLittleEndianShort(v));
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
      bytesIndices += indices.size() * getByteStrideIndices();
      accessorId++;

      if (indices.size() % 2 == 1) {
        // pad for alignment, all offsets must be divisible by 4
        bufferIndices.write(GltfHelper.BIN_PADDING);
        bufferIndices.write(GltfHelper.BIN_PADDING);
        bytesIndices += 2;
      }

      // vertices are ECEF coordinates

      // translate to the origin
      for (int n = 0; n < vertices.size() / 3; n++) {
        vertices.set(n * 3, vertices.get(n * 3) - originEcef[0]);
        vertices.set(n * 3 + 1, vertices.get(n * 3 + 1) - originEcef[1]);
        vertices.set(n * 3 + 2, vertices.get(n * 3 + 2) - originEcef[2]);
      }

      final double[] scale;
      if (quantizeMesh) {
        // scale vertices to SHORT
        double[] maxAbs = new double[] {0d, 0d, 0d};
        for (int n = 0; n < vertices.size(); n++) {
          if (Math.abs(vertices.get(n)) > maxAbs[n % 3]) {
            maxAbs[n % 3] = Math.abs(vertices.get(n));
          }
        }
        double[] finalMaxAbs = maxAbs;
        scale =
            IntStream.range(0, 3).mapToDouble(n -> finalMaxAbs[n] / GltfHelper.MAX_SHORT).toArray();
        for (int n = 0; n < vertices.size() / 3; n++) {
          vertices.set(n * 3, vertices.get(n * 3) / scale[0]);
          vertices.set(n * 3 + 1, vertices.get(n * 3 + 1) / scale[1]);
          vertices.set(n * 3 + 2, vertices.get(n * 3 + 2) / scale[2]);
        }

        // scale normals to BYTE
        for (int n = 0; n < normals.size() / 3; n++) {
          normals.set(n * 3, normals.get(n * 3) * GltfHelper.MAX_BYTE);
          normals.set(n * 3 + 1, normals.get(n * 3 + 1) * GltfHelper.MAX_BYTE);
          normals.set(n * 3 + 2, normals.get(n * 3 + 2) * GltfHelper.MAX_BYTE);
        }

      } else {
        scale = new double[] {1d, 1d, 1d};
      }

      final List<Double> verticesMin = GltfHelper.getMin(vertices);
      final List<Double> verticesMax = GltfHelper.getMax(vertices);

      builder.addAccessors(
          ImmutableAccessor.builder()
              .bufferView(1)
              .byteOffset(bytesVertices)
              .componentType(quantizeMesh ? SHORT : FLOAT)
              .normalized(false)
              .max(
                  quantizeMesh
                      ? ImmutableList.of(
                          Math.round(verticesMax.get(0)),
                          Math.round(verticesMax.get(1)),
                          Math.round(verticesMax.get(2)))
                      : verticesMax)
              .min(
                  quantizeMesh
                      ? ImmutableList.of(
                          Math.round(verticesMin.get(0)),
                          Math.round(verticesMin.get(1)),
                          Math.round(verticesMin.get(2)))
                      : verticesMin)
              .count(vertices.size() / 3)
              .type("VEC3")
              .build());
      accessorId++;
      bytesVertices += vertices.size() / 3 * getByteStrideVertices();

      if (withNormals) {
        final List<Double> normalsMin = GltfHelper.getMin(normals);
        final List<Double> normalsMax = GltfHelper.getMax(normals);
        builder.addAccessors(
            ImmutableAccessor.builder()
                .bufferView(2)
                .byteOffset(bytesNormals)
                .componentType(quantizeMesh ? BYTE : FLOAT)
                .normalized(quantizeMesh ? true : false)
                .max(
                    quantizeMesh
                        ? ImmutableList.of(
                            Math.round(normalsMax.get(0)),
                            Math.round(normalsMax.get(1)),
                            Math.round(normalsMax.get(2)))
                        : normalsMax)
                .min(
                    quantizeMesh
                        ? ImmutableList.of(
                            Math.round(normalsMin.get(0)),
                            Math.round(normalsMin.get(1)),
                            Math.round(normalsMin.get(2)))
                        : normalsMin)
                .count(normals.size() / 3)
                .type("VEC3")
                .build());
        accessorId++;
        bytesNormals += normals.size() / 3 * getByteStrideNormals();
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
              .matrix(
                  ImmutableList.of(
                      scale[0],
                      0d,
                      0d,
                      0d,
                      0d,
                      scale[1],
                      0d,
                      0d,
                      0d,
                      0d,
                      scale[2],
                      0d,
                      originEcef[0],
                      originEcef[1],
                      originEcef[2],
                      1d))
              .build());
      meshId++;
      nodeId++;

      if (quantizeMesh) {
        for (int n = 0; n < vertices.size() / 3; n++) {
          bufferVertices.write(GltfHelper.doubleToLittleEndianShort(vertices.get(n * 3)));
          bufferVertices.write(GltfHelper.doubleToLittleEndianShort(vertices.get(n * 3 + 1)));
          bufferVertices.write(GltfHelper.doubleToLittleEndianShort(vertices.get(n * 3 + 2)));
          // 3 shorts are 6 bytes, add 2 bytes to be aligned with 4-byte boundaries
          bufferVertices.write(GltfHelper.BIN_PADDING);
          bufferVertices.write(GltfHelper.BIN_PADDING);
        }
      } else {
        for (Double v : vertices) {
          bufferVertices.write(GltfHelper.doubleToLittleEndianFloat(v));
        }
      }

      if (withNormals) {
        if (quantizeMesh) {
          for (int n = 0; n < normals.size() / 3; n++) {
            bufferNormals.write(GltfHelper.doubleToLittleEndianByte(normals.get(n * 3)));
            bufferNormals.write(GltfHelper.doubleToLittleEndianByte(normals.get(n * 3 + 1)));
            bufferNormals.write(GltfHelper.doubleToLittleEndianByte(normals.get(n * 3 + 2)));
            // 3 bytes, add 1 byte to be aligned with 4-byte boundaries
            bufferNormals.write(GltfHelper.BIN_PADDING);
          }
        } else {
          for (Double v : normals) {
            bufferNormals.write(GltfHelper.doubleToLittleEndianFloat(v));
          }
        }
      }

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Geometry processing of feature '{}' is complete. Vertices: {}, Indices: {}",
            name,
            vertices.size(),
            indices.size());
      }
    }
    return true;
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
                                        GltfHelper.getCoordinates(ring.getNestedProperties())))
                            .collect(Collectors.toUnmodifiableList())))
            .collect(Collectors.toUnmodifiableList()));
  }
}
