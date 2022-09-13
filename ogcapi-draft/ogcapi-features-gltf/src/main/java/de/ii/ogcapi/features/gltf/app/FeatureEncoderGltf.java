/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gltf.app;

import static de.ii.ogcapi.features.gltf.domain.GltfProperty.GLTF_TYPE.STRING;
import static de.ii.ogcapi.features.gltf.domain.GltfProperty.GLTF_TYPE.UINT16;
import static de.ii.ogcapi.features.gltf.domain.GltfProperty.GLTF_TYPE.UINT32;
import static de.ii.ogcapi.features.gltf.domain.GltfProperty.GLTF_TYPE.UINT8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.gltf.domain.FeatureTransformationContextGltf;
import de.ii.ogcapi.features.gltf.domain.GltfConfiguration;
import de.ii.ogcapi.features.gltf.domain.GltfProperty;
import de.ii.ogcapi.features.gltf.domain.GltfProperty.GLTF_TYPE;
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
import de.ii.ogcapi.features.gltf.domain.ImmutableProperty;
import de.ii.ogcapi.features.gltf.domain.ImmutablePropertyTable;
import de.ii.ogcapi.features.gltf.domain.ImmutableScene;
import de.ii.ogcapi.features.gltf.domain.PropertyTable;
import de.ii.ogcapi.features.html.domain.Geometry;
import de.ii.ogcapi.features.html.domain.Geometry.Coordinate;
import de.ii.ogcapi.features.html.domain.Geometry.MultiPolygon;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.features.domain.FeatureObjectEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.streams.domain.OutputStreamToByteConsumer;
import earcut4j.Earcut;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
  private static final String EXT_STRUCTURAL_METADATA = "EXT_structural_metadata";
  private static final String EXT_MESH_FEATURES = "EXT_mesh_features";
  private static final int INITIAL_SIZE = 1_024;
  private static final int WARNING_THRESHOLD_FEATURES_PER_FILE = 5_000;
  private static final int MATERIAL = 0;
  private static final String FEATURE_ID = "_featureId";
  private static final String INDICES = "_indices";
  private static final String VERTICES = "_vertices";
  private static final String NORMALS = "_normals";
  private static final String PROPERTY_PREFIX = "_p_";
  private static final int BUFFER_VIEW_NORMALS = 2;
  private static final int BUFFER_VIEW_INDICES = 0;
  private static final int BUFFER_VIEW_VERTICES = 1;
  private static final String STRING_OFFSET = "_string_offset_";

  private enum AXES {
    XYZ,
    YZX,
    ZXY
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderGltf.class);

  private static final String GML_ID = "gml_id";
  private static final String ID = "id";
  static final String SURFACE_TYPE = "surfaceType";

  private static final int ARRAY_BUFFER = 34962;
  private static final int ELEMENT_ARRAY_BUFFER = 34963;

  private static final int BYTE = 5120;
  private static final int UNSIGNED_BYTE = 5121;
  private static final int SHORT = 5122;
  private static final int UNSIGNED_SHORT = 5123;
  private static final int UNSIGNED_INT = 5125;
  private static final int FLOAT = 5126;

  private static final int TRIANGLES = 4;

  private final FeatureTransformationContextGltf transformationContext;
  private final CrsTransformer crs84hToEcef;
  private final boolean clampToEllipsoid;
  private final OutputStream outputStream;
  private final Map<String, Byte> surfaceTypeEnums;
  private final boolean withSurfaceTypes;
  private final Map<String, ByteArrayOutputStream> buffers;
  private final Map<String, Integer> bufferViews;
  private final Map<String, Integer> currentBufferViewOffsets;
  private final boolean polygonOrientationIsNotGuaranteed;
  private final GltfConfiguration configuration;
  private final Optional<FeatureSchema> featureSchema;

  private ImmutableGltfAsset.Builder builder;

  private Map<String, Map<String, Integer>> enums;
  private int nextNodeId;
  private int nextMeshId;
  private int nextAccessorId;
  private final boolean withNormals;
  private final boolean withProperties;
  private final boolean quantizeMesh;
  private List<Integer> buildingNodes;
  private final long transformerStart = System.nanoTime();
  private long processingStart;
  private long featureFetched;
  private int buildingCount;
  private long featuresDuration;

  public FeatureEncoderGltf(FeatureTransformationContextGltf transformationContext) {
    this.transformationContext = transformationContext;
    this.crs84hToEcef = transformationContext.getCrsTransformerCrs84hToEcef();
    this.clampToEllipsoid = transformationContext.getClampToEllipsoid();
    this.featureSchema = transformationContext.getFeatureSchema();
    this.outputStream = new OutputStreamToByteConsumer(this::push);
    this.withNormals =
        transformationContext.getConfiguration(GltfConfiguration.class).writeNormals();
    this.quantizeMesh =
        transformationContext.getConfiguration(GltfConfiguration.class).useMeshQuantization();
    this.polygonOrientationIsNotGuaranteed =
        transformationContext
            .getConfiguration(GltfConfiguration.class)
            .polygonOrientationIsNotGuaranteed();
    this.configuration =
        transformationContext
            .getApiData()
            .getExtension(GltfConfiguration.class, transformationContext.getCollectionId())
            .orElseThrow();
    this.withProperties = !configuration.getProperties().isEmpty();
    this.buffers = new HashMap<>();
    this.bufferViews = new HashMap<>();
    this.currentBufferViewOffsets = new HashMap<>();
    this.withSurfaceTypes = featureSchema.map(GltfHelper::withSurfaceTypes).orElse(false);
    if (withSurfaceTypes) {
      this.surfaceTypeEnums = GltfHelper.getSurfaceTypeEnums();
    } else {
      this.surfaceTypeEnums = ImmutableMap.of();
    }
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
      if (featureFetched > WARNING_THRESHOLD_FEATURES_PER_FILE) {
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

    String fid =
        feature
            .findPropertyByPath(GML_ID)
            .map(PropertyGltf::getFirstValue)
            .orElse(feature.findPropertyByPath(ID).map(PropertyGltf::getFirstValue).orElse(null));

    List<MeshSurface> surfaces = MeshSurface.collectSolidSurfaces(feature);

    try {
      boolean added = addMultiPolygons(feature, fid, surfaces);
      if (added) {
        buildingNodes.add(nextNodeId - 1);
        this.buildingCount++;
      }
    } catch (Exception e) {
      LOGGER.error(
          "Error while processing solid geometries of feature '{}', the feature is ignored: {}",
          fid,
          e.getMessage());
      if (LOGGER.isDebugEnabled(MARKER.STACKTRACE)) {
        LOGGER.debug("Stacktrace: ", e);
      }
    }

    this.featuresDuration += System.nanoTime() - featureStart;
  }

  private void writeProperty(
      ByteArrayOutputStream buffer,
      ByteArrayOutputStream offsetBuffer,
      String propertyName,
      GltfProperty property,
      String value)
      throws IOException {
    String effectiveValue =
        (Objects.nonNull(value) && !value.isBlank())
            ? enums.containsKey(propertyName)
                ? String.valueOf(
                    Objects.requireNonNullElse(
                        enums.get(propertyName).get(value), property.getNoData().orElse("0")))
                : value
            : property.getNoData().orElse("0");
    try {
      switch (property.getType()) {
        case INT8:
        case UINT8:
          buffer.write(Byte.parseByte(effectiveValue));
          break;
        case INT16:
        case UINT16:
          buffer.write(GltfHelper.intToLittleEndianShort(Integer.parseInt(effectiveValue)));
          break;
        case INT32:
        case UINT32:
          buffer.write(GltfHelper.intToLittleEndianInt(Integer.parseInt(effectiveValue)));
          break;
        case FLOAT32:
          buffer.write(GltfHelper.doubleToLittleEndianFloat(Double.parseDouble(effectiveValue)));
          break;
        case INT64:
        case UINT64:
          buffer.write(GltfHelper.longToLittleEndianLong(Long.parseLong(effectiveValue)));
          break;
        case FLOAT64:
          buffer.write(GltfHelper.doubleToLittleEndianDouble(Double.parseDouble(effectiveValue)));
          break;
        case STRING:
          if (!effectiveValue.isEmpty()) {
            buffer.write(effectiveValue.getBytes(StandardCharsets.UTF_8));
          }
          if (property.getOffsetType() == UINT8) {
            offsetBuffer.write((byte) buffer.size());
          } else if (property.getOffsetType() == UINT16) {
            offsetBuffer.write(GltfHelper.intToLittleEndianShort(buffer.size()));
          } else if (property.getOffsetType() == UINT32) {
            offsetBuffer.write(GltfHelper.intToLittleEndianInt(buffer.size()));
          }
          break;
        case BOOLEAN:
        default:
          throw new IllegalStateException(
              String.format(
                  "The glTF type of the feature property is currently not supported: %s",
                  property.getType().toString()));
      }
    } catch (NumberFormatException e) {
      LOGGER.warn("Could not parse attribute value '{}' as a number, using '0'.", effectiveValue);
      writeProperty(buffer, offsetBuffer, propertyName, property, "0");
    }
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
              buildingCount,
              transformerDuration,
              processingDuration,
              toMilliseconds(featuresDuration),
              buildingCount == 0 ? 0 : toMilliseconds(featuresDuration / buildingCount),
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

    nextNodeId = 0;
    nextMeshId = 0;
    nextAccessorId = 0;
    buildingNodes = new ArrayList<>(INITIAL_SIZE);

    buffers.put(INDICES, new ByteArrayOutputStream(getByteStrideIndices() * INITIAL_SIZE));
    currentBufferViewOffsets.put(INDICES, 0);
    buffers.put(VERTICES, new ByteArrayOutputStream(getByteStrideVertices() * INITIAL_SIZE));
    currentBufferViewOffsets.put(VERTICES, 0);
    if (withNormals) {
      buffers.put(NORMALS, new ByteArrayOutputStream(getByteStrideNormals() * INITIAL_SIZE));
      currentBufferViewOffsets.put(NORMALS, 0);
    }
    buffers.put(FEATURE_ID, new ByteArrayOutputStream(getByteStrideFeatureId() * INITIAL_SIZE));
    currentBufferViewOffsets.put(FEATURE_ID, 0);
    buffers.put(
        PROPERTY_PREFIX + SURFACE_TYPE,
        new ByteArrayOutputStream(getByteStrideSurfaceTypes() * INITIAL_SIZE));
    currentBufferViewOffsets.put(PROPERTY_PREFIX + SURFACE_TYPE, 0);
    if (withProperties) {
      configuration
          .getProperties()
          .forEach(
              (key, value) -> {
                buffers.put(
                    PROPERTY_PREFIX + key,
                    new ByteArrayOutputStream(getInitialBufferSize(value.getType())));
                currentBufferViewOffsets.put(PROPERTY_PREFIX + key, 0);
                if (value.getType() == STRING) {
                  ByteArrayOutputStream buffer =
                      new ByteArrayOutputStream(getInitialBufferSize(UINT8));
                  try {
                    if (value.getOffsetType() == UINT8) {
                      buffer.write((byte) 0);
                    } else if (value.getOffsetType() == UINT16) {
                      buffer.write(GltfHelper.intToLittleEndianShort(0));
                    } else if (value.getOffsetType() == UINT32) {
                      buffer.write(GltfHelper.intToLittleEndianInt(0));
                    }
                  } catch (IOException e) {
                    throw new IllegalStateException("Cannot write to string offset buffer.", e);
                  }
                  buffers.put(PROPERTY_PREFIX + key + STRING_OFFSET, buffer);
                  currentBufferViewOffsets.put(
                      PROPERTY_PREFIX + key + STRING_OFFSET, buffer.size());
                }
              });

      enums = GltfHelper.getEnums(featureSchema.orElseThrow(), configuration.getProperties());
    }
  }

  private int getInitialBufferSize(GLTF_TYPE value) {
    switch (value) {
      case INT8:
      case UINT8:
      case BOOLEAN:
        return INITIAL_SIZE;
      case INT16:
      case UINT16:
        return 2 * INITIAL_SIZE;
      case INT32:
      case UINT32:
      case FLOAT32:
        return 4 * INITIAL_SIZE;
      case INT64:
      case UINT64:
      case FLOAT64:
      case STRING:
      default:
        return 8 * INITIAL_SIZE;
    }
  }

  private void finalizeModel() {

    int offset = 0;
    int size = buffers.get(INDICES).size();
    builder.addBufferViews(
        ImmutableBufferView.builder()
            .buffer(0)
            .byteLength(size)
            .byteOffset(offset)
            .target(ELEMENT_ARRAY_BUFFER)
            .build());
    offset += size;

    size = buffers.get(VERTICES).size();
    builder.addBufferViews(
        ImmutableBufferView.builder()
            .buffer(0)
            .byteLength(size)
            .byteOffset(offset)
            .byteStride(getByteStrideVertices())
            .target(ARRAY_BUFFER)
            .build());
    offset += size;

    int nextBufferViewId = 2;
    if (withNormals) {
      size = buffers.get(NORMALS).size();
      builder.addBufferViews(
          ImmutableBufferView.builder()
              .buffer(0)
              .byteLength(size)
              .byteOffset(offset)
              .byteStride(getByteStrideNormals())
              .target(ARRAY_BUFFER)
              .build());
      offset += size;
      nextBufferViewId++;
    }

    if (withProperties || withSurfaceTypes) {
      size = buffers.get(FEATURE_ID).size();
      builder.addBufferViews(
          ImmutableBufferView.builder()
              .buffer(0)
              .byteLength(size)
              .byteOffset(offset)
              // each *element* must align to 4-byte boundaries; UNSIGNED_INT is not allowed
              .byteStride(getByteStrideFeatureId())
              .build());
      offset += size;
      nextBufferViewId++;
    }

    for (Map.Entry<String, ByteArrayOutputStream> entry : buffers.entrySet()) {
      String bufferName = entry.getKey();
      if (bufferName.startsWith(PROPERTY_PREFIX)) {
        ByteArrayOutputStream buffer = entry.getValue();
        size = buffer.size();
        builder.addBufferViews(
            ImmutableBufferView.builder()
                .buffer(0)
                .byteLength(size)
                .byteOffset(offset)
                .target(ELEMENT_ARRAY_BUFFER)
                .build());
        offset += size;
        bufferViews.put(bufferName, nextBufferViewId++);
      }
    }

    if (offset > 0) {
      builder.addBuffers(ImmutableBuffer.builder().byteLength(offset).build());
      builder.addNodes(
          ImmutableNode.builder()
              .children(buildingNodes)
              // z-up (CRS) => y-up (glTF uses y-up)
              .addMatrix(1d, 0d, 0d, 0d, 0d, 0d, -1d, 0d, 0d, 1d, 0d, 0d, 0d, 0d, 0d, 1d)
              .build());
      builder.addScenes(ImmutableScene.builder().nodes(ImmutableList.of(nextNodeId)).build());
    }

    if (quantizeMesh) {
      builder.addExtensionsUsed(KHR_MESH_QUANTIZATION);
      builder.addExtensionsRequired(KHR_MESH_QUANTIZATION);
    }

    builder.addMaterials(
        ImmutableMaterial.builder()
            .pbrMetallicRoughness(
                ImmutablePbrMetallicRoughness.builder()
                    .baseColorFactor(ImmutableList.of(0.5f, 0.5f, 0.5f, 1.0f))
                    .metallicFactor(0.2f)
                    .roughnessFactor(1.0f)
                    .build())
            .doubleSided(polygonOrientationIsNotGuaranteed)
            .build());

    ImmutablePropertyTable.Builder propertyTableBuilder =
        ImmutablePropertyTable.builder()
            .class_("building")
            .count(buffers.get(PROPERTY_PREFIX + SURFACE_TYPE).size());

    for (Map.Entry<String, GltfProperty> entry : configuration.getProperties().entrySet()) {
      String propertyName = entry.getKey();
      Optional<Integer> stringOffset =
          Optional.ofNullable(bufferViews.get(PROPERTY_PREFIX + propertyName + STRING_OFFSET));
      Optional<String> stringOffsetType =
          stringOffset.isPresent()
              ? Optional.of(entry.getValue().getOffsetType().name())
              : Optional.empty();
      propertyTableBuilder.putProperties(
          propertyName,
          ImmutableProperty.builder()
              .values(bufferViews.get(PROPERTY_PREFIX + propertyName))
              .stringOffsets(stringOffset)
              .stringOffsetType(stringOffsetType)
              .build());
    }

    if (withSurfaceTypes) {
      propertyTableBuilder.putProperties(
          "surfaceType",
          ImmutableProperty.builder()
              .values(bufferViews.get(PROPERTY_PREFIX + SURFACE_TYPE))
              .build());
    }

    PropertyTable propertyTable = propertyTableBuilder.build();

    if (transformationContext.getSchemaUri().isPresent() && propertyTable.getCount() > 0) {
      builder
          .putExtensions(
              EXT_STRUCTURAL_METADATA,
              ImmutableMap.of(
                  "schemaUri",
                  transformationContext.getSchemaUri().get().toString(),
                  "propertyTables",
                  ImmutableList.of(propertyTable)))
          .addExtensionsUsed(EXT_STRUCTURAL_METADATA, EXT_MESH_FEATURES);
    }

    Builder<ByteArrayOutputStream> bufferList =
        ImmutableList.<ByteArrayOutputStream>builder()
            .add(buffers.get(INDICES))
            .add(buffers.get(VERTICES));
    if (withNormals) {
      bufferList.add(buffers.get(NORMALS));
    }
    if (withProperties || withSurfaceTypes) {
      bufferList.add(buffers.get(FEATURE_ID));
    }
    bufferList.addAll(
        bufferViews.entrySet().stream()
            .sorted(Comparator.comparingInt(Entry::getValue))
            .map(entry -> buffers.get(entry.getKey()))
            .collect(Collectors.toUnmodifiableList()));
    GltfHelper.writeGltfBinary(builder.build(), bufferList.build(), outputStream);
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

  private int getByteStrideFeatureId() {
    return 4;
  }

  private int getByteStrideSurfaceTypes() {
    return 1;
  }

  // TODO support other geometric primitives

  private boolean addMultiPolygons(FeatureGltf feature, String name, List<MeshSurface> meshSurfaces)
      throws IOException {

    // determine the origin to use; eventually we will translate vertices to the center of the
    // feature to have smaller values (glTF uses float)
    List<Coordinate> coordList =
        meshSurfaces.stream()
            .map(MeshSurface::getGeometry)
            .map(MultiPolygon::getCoordinatesFlat)
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableList());
    double[] min =
        new double[] {
          coordList.stream().mapToDouble(coord -> coord.get(0)).min().orElseThrow(),
          coordList.stream().mapToDouble(coord -> coord.get(1)).min().orElseThrow(),
          coordList.stream().mapToDouble(coord -> coord.get(2)).min().orElseThrow()
        };
    double[] max =
        new double[] {
          coordList.stream().mapToDouble(coord -> coord.get(0)).max().orElseThrow(),
          coordList.stream().mapToDouble(coord -> coord.get(1)).max().orElseThrow(),
          coordList.stream().mapToDouble(coord -> coord.get(2)).max().orElseThrow()
        };
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
    List<Integer> featureIds = new ArrayList<>();
    int indexCount = 0;
    int surfaceCount = 0;

    // write feature ids and a property table only if we have surface type information
    boolean hasSurfaceType = meshSurfaces.stream().anyMatch(s -> s.getSurfaceType().isPresent());

    for (MeshSurface surface : meshSurfaces) {
      int vertexCountSurface =
          triangulate(surface.getGeometry(), name, min[2], indices, indexCount, vertices, normals);
      indexCount += vertexCountSurface;

      if ((hasSurfaceType || withProperties) && vertexCountSurface >= 3) {
        ByteArrayOutputStream buffer = buffers.get(PROPERTY_PREFIX + SURFACE_TYPE);

        IntStream.range(0, vertexCountSurface).forEach(i -> featureIds.add(buffer.size()));
        buffer.write(surfaceTypeEnums.get(surface.getSurfaceType().orElse("unknown")));

        configuration
            .getProperties()
            .forEach(
                (key, value) -> {
                  try {
                    writeProperty(
                        buffers.get(PROPERTY_PREFIX + key),
                        buffers.get(PROPERTY_PREFIX + key + STRING_OFFSET),
                        key,
                        value,
                        feature
                            .findPropertyByPath(key)
                            .map(PropertyGltf::getFirstValue)
                            .orElse(null));
                  } catch (IOException e) {
                    throw new IllegalStateException(e);
                  }
                });

        surfaceCount++;
      }

      if ((hasSurfaceType || withProperties) && featureIds.size() != vertices.size() / 3) {
        LOGGER.error(
            "System error: Size of feature id array for structural metadata differs from size of vertices: {} vs. {}",
            featureIds.size(),
            vertices.size() / 3);
      }
    }

    // glTF output
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

      // write indices and add accessor
      ByteArrayOutputStream buffer = buffers.get(INDICES);
      for (int v : indices) {
        buffer.write(GltfHelper.intToLittleEndianShort(v));
      }

      ImmutableAttributes.Builder attributesBuilder = ImmutableAttributes.builder();
      int accessorIdIndices = nextAccessorId;
      builder.addAccessors(
          ImmutableAccessor.builder()
              .bufferView(BUFFER_VIEW_INDICES)
              .byteOffset(currentBufferViewOffsets.get(INDICES))
              .componentType(UNSIGNED_SHORT)
              .addMax(indices.stream().max(Comparator.naturalOrder()).orElseThrow())
              .addMin(indices.stream().min(Comparator.naturalOrder()).orElseThrow())
              .count(indices.size())
              .type("SCALAR")
              .build());

      // pad for alignment, all offsets must be divisible by 4
      while (buffers.get(INDICES).size() % 4 > 0) {
        buffers.get(INDICES).write(GltfHelper.BIN_PADDING);
      }

      nextAccessorId++;
      currentBufferViewOffsets.put(INDICES, buffer.size());

      // prepare vertices and add accessor

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
        scale = IntStream.range(0, 3).mapToDouble(n -> maxAbs[n] / GltfHelper.MAX_SHORT).toArray();
        for (int n = 0; n < vertices.size() / 3; n++) {
          vertices.set(n * 3, vertices.get(n * 3) / scale[0]);
          vertices.set(n * 3 + 1, vertices.get(n * 3 + 1) / scale[1]);
          vertices.set(n * 3 + 2, vertices.get(n * 3 + 2) / scale[2]);
        }

        buffer = buffers.get(VERTICES);
        for (int n = 0; n < vertices.size() / 3; n++) {
          buffer.write(GltfHelper.doubleToLittleEndianShort(vertices.get(n * 3)));
          buffer.write(GltfHelper.doubleToLittleEndianShort(vertices.get(n * 3 + 1)));
          buffer.write(GltfHelper.doubleToLittleEndianShort(vertices.get(n * 3 + 2)));
          // 3 shorts are 6 bytes, add 2 bytes to be aligned with 4-byte boundaries
          buffer.write(GltfHelper.BIN_PADDING);
          buffer.write(GltfHelper.BIN_PADDING);
        }
      } else {
        scale = new double[] {1d, 1d, 1d};

        for (Double v : vertices) {
          buffers.get(VERTICES).write(GltfHelper.doubleToLittleEndianFloat(v));
        }
      }

      final List<Double> verticesMin = GltfHelper.getMin(vertices);
      final List<Double> verticesMax = GltfHelper.getMax(vertices);
      builder.addAccessors(
          ImmutableAccessor.builder()
              .bufferView(BUFFER_VIEW_VERTICES)
              .byteOffset(currentBufferViewOffsets.get(VERTICES))
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
      attributesBuilder.position(nextAccessorId);
      nextAccessorId++;
      currentBufferViewOffsets.put(VERTICES, buffers.get(VERTICES).size());

      if (withNormals) {
        // write normals and add accessor
        buffer = buffers.get(NORMALS);
        if (quantizeMesh) {
          // scale normals to BYTE
          for (int n = 0; n < normals.size() / 3; n++) {
            normals.set(n * 3, normals.get(n * 3) * GltfHelper.MAX_BYTE);
            normals.set(n * 3 + 1, normals.get(n * 3 + 1) * GltfHelper.MAX_BYTE);
            normals.set(n * 3 + 2, normals.get(n * 3 + 2) * GltfHelper.MAX_BYTE);
          }

          for (int n = 0; n < normals.size() / 3; n++) {
            buffer.write(GltfHelper.doubleToLittleEndianByte(normals.get(n * 3)));
            buffer.write(GltfHelper.doubleToLittleEndianByte(normals.get(n * 3 + 1)));
            buffer.write(GltfHelper.doubleToLittleEndianByte(normals.get(n * 3 + 2)));
            // 3 bytes, add 1 byte to be aligned with 4-byte boundaries
            buffer.write(GltfHelper.BIN_PADDING);
          }
        } else {
          for (Double v : normals) {
            buffer.write(GltfHelper.doubleToLittleEndianFloat(v));
          }
        }

        final List<Double> normalsMin = GltfHelper.getMin(normals);
        final List<Double> normalsMax = GltfHelper.getMax(normals);
        builder.addAccessors(
            ImmutableAccessor.builder()
                .bufferView(BUFFER_VIEW_NORMALS)
                .byteOffset(currentBufferViewOffsets.get(NORMALS))
                .componentType(quantizeMesh ? BYTE : FLOAT)
                .normalized(quantizeMesh)
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
        attributesBuilder.normal(nextAccessorId);
        nextAccessorId++;
        currentBufferViewOffsets.put(NORMALS, buffers.get(NORMALS).size());
      }

      if (withProperties || hasSurfaceType) {
        buffer = buffers.get(FEATURE_ID);

        // write feature id and create accessor
        for (int i = 0; i < vertices.size() / 3; i++) {
          buffer.write(GltfHelper.intToLittleEndianShort(featureIds.get(i)));
          buffer.write(GltfHelper.BIN_PADDING);
          buffer.write(GltfHelper.BIN_PADDING);
        }

        builder.addAccessors(
            ImmutableAccessor.builder()
                .bufferView(withNormals ? 3 : 2)
                .byteOffset(currentBufferViewOffsets.get(FEATURE_ID))
                .componentType(UNSIGNED_SHORT)
                .count(featureIds.size())
                .type("SCALAR")
                .build());
        attributesBuilder.featureId0(nextAccessorId);
        nextAccessorId++;
        currentBufferViewOffsets.put(FEATURE_ID, buffer.size());
      }

      // add mesh and node for the building
      ImmutableList.Builder<Map<String, Object>> featureIdsBuilder = ImmutableList.builder();
      if (withProperties || hasSurfaceType) {
        featureIdsBuilder.add(
            ImmutableMap.of("featureCount", surfaceCount, "attribute", 0, "propertyTable", 0));
      }
      List<Map<String, Object>> meshFeatures = featureIdsBuilder.build();
      builder.addMeshes(
          ImmutableMesh.builder()
              .addPrimitives(
                  ImmutablePrimitive.builder()
                      .attributes(attributesBuilder.build())
                      .mode(TRIANGLES)
                      .indices(accessorIdIndices)
                      .material(MATERIAL)
                      .extensions(
                          !meshFeatures.isEmpty()
                              ? ImmutableMap.of(
                                  EXT_MESH_FEATURES, ImmutableMap.of("featureIds", meshFeatures))
                              : ImmutableMap.of())
                      .build())
              .build());

      builder.addNodes(
          ImmutableNode.builder()
              .name(Optional.ofNullable(name))
              .mesh(nextMeshId)
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
      nextMeshId++;
      nextNodeId++;

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Geometry processing of feature '{}' is complete. Vertices: {}, Indices: {}",
            name,
            vertices.size(),
            indices.size());
      }
    } else {
      return false;
    }

    return true;
  }

  private int triangulate(
      MultiPolygon multiPolygon,
      String featureName,
      double minZ,
      List<Integer> indices,
      int indexCountBase,
      List<Double> vertices,
      List<Double> normals) {
    // triangulate the polygons, translate relative to origin
    int vertexCountSurface = 0;
    int numRing;
    AXES axes = AXES.XYZ;
    boolean ccw = true;
    List<Double> data = new ArrayList<>();
    List<Integer> holeIndices = new ArrayList<>();
    double area;
    for (Geometry.Polygon polygon : multiPolygon.getCoordinates()) {
      numRing = 0;
      data.clear();
      holeIndices.clear();

      // change axis order, if we have a vertical polygon; ensure we still have a right-handed CRS
      for (Geometry.LineString ring : polygon.getCoordinates()) {
        List<Coordinate> coordsRing = ring.getCoordinates();

        // remove consecutive duplicate points
        List<Coordinate> coordList =
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
                  featureName,
                  coordList);
            }
            break;
          } else {
            // skip ring, if a hole
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace(
                  "Skipping hole of feature '{}', interior ring has no effective area: {}",
                  featureName,
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
              clampToEllipsoid ? coordList.get(n).get(2) - minZ : coordList.get(n).get(2);
        }

        // transform to ECEF coordinates
        double[] coordsEcef = crs84hToEcef.transform(coords, coords.length / 3, 3);

        if (LOGGER.isTraceEnabled()) {
          if (!GltfHelper.isCoplanar(coordList)) {
            LOGGER.trace(
                "Feature '{}' has a ring that is not coplanar. The glTF mesh may be invalid. Coordinates: {}",
                featureName,
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
              continue;
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
                    featureName,
                    coordList);
              }
              break;
            } else {
              // skip ring, if a hole
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "Skipping hole of feature '{}', could not compute normal for exterior ring: {}",
                    featureName,
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

      double[] coords2dForTriangulation = new double[data.size() / 3 * 2];
      if (axes == AXES.XYZ) {
        for (int n = 0; n < data.size() / 3; n++) {
          coords2dForTriangulation[n * 2] = data.get(n * 3);
          coords2dForTriangulation[n * 2 + 1] = data.get(n * 3 + 1);
        }
      } else if (axes == AXES.YZX) {
        for (int n = 0; n < data.size() / 3; n++) {
          coords2dForTriangulation[n * 2] = data.get(n * 3 + 1);
          coords2dForTriangulation[n * 2 + 1] = data.get(n * 3 + 2);
        }
      } else {
        for (int n = 0; n < data.size() / 3; n++) {
          coords2dForTriangulation[n * 2] = data.get(n * 3 + 2);
          coords2dForTriangulation[n * 2 + 1] = data.get(n * 3);
        }
      }

      List<Integer> triangles =
          coords2dForTriangulation.length > 6
              ? Earcut.earcut(
                  coords2dForTriangulation,
                  holeIndices.isEmpty()
                      ? null
                      : holeIndices.stream().mapToInt(Integer::intValue).toArray(),
                  2)
              : new ArrayList<>(ImmutableList.of(0, 1, 2));

      if (triangles.isEmpty()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Cannot triangulate a polygon of feature '{}', the polygon is ignored: {}",
              featureName,
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
        }
      }

      vertices.addAll(data);

      for (int ringIndex : triangles) {
        indices.add(indexCountBase + vertexCountSurface + ringIndex);
      }

      vertexCountSurface += data.size() / 3;
    }

    return vertexCountSurface;
  }
}
