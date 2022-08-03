/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import static de.ii.ogcapi.features.gltf.app.Helper.EPSILON;
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
import de.ii.ogcapi.features.gltf.domain.Material;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import de.ii.ogcapi.features.html.domain.Geometry.MultiPolygon;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import earcut4j.Earcut;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    boolean polygonOrientationIsNotGuaranteed =
        transformationContext
            .getConfiguration(GltfConfiguration.class)
            .polygonOrientationIsNotGuaranteed();

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
            .doubleSided(polygonOrientationIsNotGuaranteed)
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
            .doubleSided(polygonOrientationIsNotGuaranteed)
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
            .doubleSided(polygonOrientationIsNotGuaranteed)
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
            .doubleSided(polygonOrientationIsNotGuaranteed)
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
                                    }
                                  },
                                  () ->
                                      feature
                                          .findPropertyByPath(LOD_1_SOLID)
                                          .ifPresent(
                                              solidProperty -> {
                                                if (processSolid(fid, solidProperty)) {
                                                  buildingNodes.add(nodeId - 1);
                                                }
                                              })));
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

  private boolean processSolid(String fid, PropertyGltf solidProperty) {
    try {
      addMultiPolygon(fid, solidProperty, 0, null, null);
    } catch (Exception e) {
      LOGGER.error(
          "Error while processing property '{}' of feature '{}', the property is ignored: {}",
          solidProperty.getName(),
          fid,
          e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Stacktrace: ", e);
      }
      return false;
    }
    return true;
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
      originBuilding = new double[] {(minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0};
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
                      "Error while processing property 'lod2MultiSurface' of feature '{}', the property is ignored: {}",
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
    withNormals = transformationContext.getConfiguration(GltfConfiguration.class).isWithNormals();
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

    Helper.writeGltfBinary(builder.build(), bufferIndices, bufferVertices, outputStream);
  }

  // TODO support other geometric primitives

  private void addMultiPolygon(
      String name,
      PropertyGltf geometryProperty,
      int materialId,
      double[] originBuilding,
      Double minZ)
      throws IOException {

    // determine the origin to use; eventually we will translate vertices to the center of the
    // feature to have smaller values (glTF uses float)
    double[] originFeature;
    double minZFeature;
    Geometry.MultiPolygon geometry = getMultiPolygon(geometryProperty);
    List<Coordinate> coordList = geometry.getCoordinatesFlat();
    if (Objects.isNull(originBuilding)) {
      ImmutableList<Double> min =
          ImmutableList.of(
              coordList.stream().mapToDouble(coord -> coord.get(0)).min().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(1)).min().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(2)).min().orElseThrow());
      ImmutableList<Double> max =
          ImmutableList.of(
              coordList.stream().mapToDouble(coord -> coord.get(0)).max().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(1)).max().orElseThrow(),
              coordList.stream().mapToDouble(coord -> coord.get(2)).max().orElseThrow());

      originFeature =
          new double[] {
            (min.get(0) + max.get(0)) / 2.0,
            (min.get(1) + max.get(1)) / 2.0,
            (min.get(2) + max.get(2)) / 2.0
          };
      minZFeature = min.get(2);
    } else {
      originFeature = originBuilding;
      minZFeature = minZ;
    }
    double[] originFeatureEcef = crs84hToEcef.transform(originFeature, 1, 3);

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
    for (Geometry.Polygon polygon : geometry.getCoordinates()) {
      numRing = 0;
      data.clear();
      holeIndices.clear();

      // change axis order, if we have a vertical polygon; ensure we still have a right-handed CRS
      for (Geometry.LineString ring : polygon.getCoordinates()) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Ring: {}", ring);
        }

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
        if (coordList.size() < 4 || Helper.find3rdPoint(coordList)[1] == -1) {
          if (numRing == 0) {
            // skip polygon, if exterior boundary
            break;
          } else {
            // skip ring, if a hole
            continue;
          }
        }

        // do not copy the last point, same as first point
        double[] coords = new double[(coordList.size() - 1) * 3];
        for (int n = 0; n < coordList.size() - 1; n++) {
          coords[n * 3] = coordList.get(n).get(0);
          coords[n * 3 + 1] = coordList.get(n).get(1);
          coords[n * 3 + 2] =
              clampToGround ? coordList.get(n).get(2) - minZFeature : coordList.get(n).get(2);
        }

        // transform to ECEF coordinates and add translation to the origin
        double[] coordsEcef = crs84hToEcef.transform(coords, coords.length / 3, 3);
        for (int n = 0; n < coordsEcef.length / 3; n++) {
          coordsEcef[n * 3] -= originFeatureEcef[0];
          coordsEcef[n * 3 + 1] -= originFeatureEcef[1];
          coordsEcef[n * 3 + 2] -= originFeatureEcef[2];
        }

        /* TODO
        boolean coplanar = Helper.isCoplanar(coords);
        if (!coplanar) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                "Feature '{}' has a ring that is not coplanar. The glTF mesh may be invalid. Coordinates: {}",
                name,
                coords);
          }
        }
         */

        if (numRing == 0) {

          final double area01 = Math.abs(Helper.computeArea(coordsEcef, 0, 1));
          final double area12 = Math.abs(Helper.computeArea(coordsEcef, 1, 2));
          final double area20 = Math.abs(Helper.computeArea(coordsEcef, 2, 0));
          if (area01 > area12 && area01 > area20) {
            axes = AXES.XYZ;
            ccw = area01 < 0;
          } else if (area12 > area20) {
            axes = AXES.YZX;
            ccw = area12 < 0;
          } else if (Math.abs(area20) > EPSILON) {
            axes = AXES.ZXY;
            ccw = area20 < 0;
          } else {
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace(
                  "The area of the exterior ring is too small, the polygon is ignored: {} {} {} - {}",
                  area01,
                  area12,
                  area20,
                  coordsEcef);
            }
            break;
          }
        } else {
          holeIndices.add((data.size() / 3) + 1);
        }

        /* TODO what to do for polgons that are not coplanar
        if (withNormals) {
          Geometry.Coordinate normal = Helper.computeNormal(coordsArr);
          for (int i = 0; i < coordsArr.length/3; i++) {
            normals.addAll(normal);
          }
        }
         */

        data.addAll(Arrays.stream(coordsEcef).boxed().collect(Collectors.toUnmodifiableList()));

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

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Triangles: {}", triangles);
        LOGGER.trace("Data: {}, {}", data, holeIndices);
      }
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
        if (p0 * 3 + 2 > data.size() - 1
            || p1 * 3 + 2 > data.size() - 1
            || p2 * 3 + 2 > data.size() - 1) {
          LOGGER.debug("Illegal access to a non-existent vertex:");
          LOGGER.debug("Geometry: {}", geometry);
          LOGGER.debug("Polygon: {}", polygon);
          LOGGER.debug("Triangles: {}", triangles);
          LOGGER.debug("Data: {}, {}", data, holeIndices);
          break;
        }
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
                ? Helper.computeAreaTriangle(triangle, 0, 1) < 0
                : axes == AXES.YZX
                    ? Helper.computeAreaTriangle(triangle, 1, 2) < 0
                    : Helper.computeAreaTriangle(triangle, 2, 0) < 0;
        boolean exterior = holeIndices.isEmpty() || p0 >= holeIndices.get(0);
        if ((exterior && ccwTriangle != ccw) || (!exterior && ccwTriangle == ccw)) {
          // switch orientation, if the triangle has the wrong orientation
          triangles.set(i * 3, p2);
          triangles.set(i * 3 + 2, p0);
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Switching triangle orientation: {}", triangle);
          }
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
        bufferIndices.write(Helper.intToLittleEndianShort(v));
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
        bufferIndices.write(Helper.BIN_PADDING);
        bufferIndices.write(Helper.BIN_PADDING);
        bytesIndices += 2;
      }

      builder.addAccessors(
          ImmutableAccessor.builder()
              .bufferView(1)
              .byteOffset(bytesVectors)
              .componentType(FLOAT)
              .addAllMax(Helper.getMax(vertices))
              .addAllMin(Helper.getMin(vertices))
              .count(vertices.size() / 3)
              .type("VEC3")
              .build());
      accessorId++;
      bytesVectors += vertices.size() * 4;
      if (withNormals) {
        builder.addAccessors(
            ImmutableAccessor.builder()
                .bufferView(1)
                .byteOffset(bytesVectors)
                .componentType(FLOAT)
                .addAllMax(Helper.getMax(normals))
                .addAllMin(Helper.getMin(normals))
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

      for (double v : vertices) {
        bufferVertices.write(Helper.doubleToLittleEndianFloat(v));
      }

      if (withNormals) {
        for (double v : normals) {
          bufferVertices.write(Helper.doubleToLittleEndianFloat(v));
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
                                        Helper.getCoordinates(ring.getNestedProperties())))
                            .collect(Collectors.toUnmodifiableList())))
            .collect(Collectors.toUnmodifiableList()));
  }
}
