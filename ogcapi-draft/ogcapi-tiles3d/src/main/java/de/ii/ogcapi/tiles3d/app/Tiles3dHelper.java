/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.features.core.domain.FeaturesQuery;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ogcapi.features.gltf.domain.ImmutableBuffer;
import de.ii.ogcapi.features.gltf.domain.ImmutableBufferView;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles3d.domain.ImmutableAvailability;
import de.ii.ogcapi.tiles3d.domain.ImmutableSubtree;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles.QueryInputSubtree;
import de.ii.ogcapi.tiles3d.domain.Subtree;
import de.ii.ogcapi.tiles3d.domain.TileResource;
import de.ii.ogcapi.tiles3d.domain.TileResourceCache;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql.Format;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.SIntersects;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.locationtech.jts.shape.fractal.MortonCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO cleanup, consolidate, harmonize code

public class Tiles3dHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(Tiles3dHelper.class);
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new GuavaModule())
          .configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          // for debugging
          // .enable(SerializationFeature.INDENT_OUTPUT)
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

  private static final byte[] EMPTY = new byte[0];

  public static final byte[] MAGIC_SUBT = new byte[] {0x73, 0x75, 0x62, 0x74};
  public static final byte[] VERSION_1 = new byte[] {0x01, 0x00, 0x00, 0x00};
  public static final byte[] JSON = new byte[] {0x4a, 0x53, 0x4f, 0x4e};
  public static final byte[] JSON_PADDING = new byte[] {0x20};
  public static final byte[] BIN_PADDING = new byte[] {0x00};

  private static byte[] getJson(Object obj) throws JsonProcessingException {
    return MAPPER.writeValueAsBytes(obj);
  }

  public static void write3dTilesSubtreeBinary(
      Subtree subtree,
      byte[] tileAvailability,
      byte[] contentAvailability,
      byte[] childSubtreeAvailability,
      OutputStream outputStream) {

    byte[] json;
    try {
      json = getJson(subtree);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          String.format("Could not write 3D Tiles subtree. Reason: %s", e.getMessage()));
    }

    int bufferLength =
        tileAvailability.length + contentAvailability.length + childSubtreeAvailability.length;
    int jsonPadding = (4 - json.length % 4) % 4;
    int bufferPadding = (4 - bufferLength % 4) % 4;

    try {
      outputStream.write(MAGIC_SUBT);
      outputStream.write(VERSION_1);

      outputStream.write(intToLittleEndianLong(json.length + jsonPadding));
      outputStream.write(intToLittleEndianLong(bufferLength + bufferPadding));

      outputStream.write(json);
      for (int i = 0; i < jsonPadding; i++) {
        outputStream.write(JSON_PADDING);
      }

      if (tileAvailability.length > 0) {
        outputStream.write(tileAvailability);
      }
      if (contentAvailability.length > 0) {
        outputStream.write(contentAvailability);
      }
      if (childSubtreeAvailability.length > 0) {
        outputStream.write(childSubtreeAvailability);
      }
      for (int i = 0; i < bufferPadding; i++) {
        outputStream.write(BIN_PADDING);
      }

      outputStream.flush();
    } catch (Exception e) {
      throw new IllegalStateException("Could not write 3D Tiles Subtree output", e);
    }
  }

  public static boolean hasData(
      FeaturesCoreQueriesHandler queriesHandler,
      QueryInputSubtree queryInput,
      BoundingBox bbox,
      Optional<Cql2Expression> additionalFilter) {
    Envelope envelope =
        Envelope.of(
            bbox.getXmin(), bbox.getYmin(), bbox.getXmax(), bbox.getYmax(), bbox.getEpsgCrs());

    Cql2Expression filter =
        SIntersects.of(Property.of(queryInput.getGeometryProperty()), SpatialLiteral.of(envelope));

    if (additionalFilter.isPresent()) {
      filter = And.of(filter, additionalFilter.get());
    }

    FeatureQuery query =
        ImmutableFeatureQuery.builder()
            .type(queryInput.getFeatureType())
            .hitsOnly(true)
            .limit(1)
            .filter(filter)
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

    byte[] entity = (byte[]) response.getEntity();
    return entity.length == 1 && entity[0] == '1';
  }

  public static byte[] getSubtree(
      FeaturesCoreQueriesHandler queriesHandler,
      QueryInputSubtree queryInput,
      TileResource subtree) {
    byte[] result;

    int size = 0;
    for (int i = 0; i < queryInput.getSubtreeLevels(); i++) {
      size += MortonCode.size(i);
    }
    byte[] tileAvailability = new byte[(size - 1) / 8 + 1];
    Arrays.fill(tileAvailability, (byte) 0);
    byte[] contentAvailability = new byte[(size - 1) / 8 + 1];
    Arrays.fill(contentAvailability, (byte) 0);
    byte[] childSubtreeAvailability =
        new byte[(MortonCode.size(queryInput.getSubtreeLevels()) - 1) / 8 + 1];
    Arrays.fill(childSubtreeAvailability, (byte) 0);

    processZ(
        queryInput,
        queriesHandler,
        subtree.getLevel(),
        subtree.getX(),
        subtree.getY(),
        subtree.getLevel(),
        subtree.getX(),
        subtree.getY(),
        tileAvailability,
        contentAvailability,
        childSubtreeAvailability);

    Boolean tileAvailabilityConstantValue = null;
    Boolean contentAvailabilityConstantValue = null;
    Boolean childSubtreeAvailabilityConstantValue = null;

    int tileAvailabilityCount =
        getAvailabilityCount(tileAvailability, 0, queryInput.getSubtreeLevels());
    if (tileAvailabilityCount == 0) {
      tileAvailabilityConstantValue = false;
      contentAvailabilityConstantValue = false;
      childSubtreeAvailabilityConstantValue = false;
    } else if (tileAvailabilityCount == size) {
      tileAvailabilityConstantValue = true;
    }

    int contentAvailabilityCount = 0;
    int childSubtreeAvailabilityCount = 0;

    if (tileAvailabilityCount > 0) {
      contentAvailabilityCount =
          getAvailabilityCount(contentAvailability, 0, queryInput.getSubtreeLevels());
      if (contentAvailabilityCount == 0) {
        contentAvailabilityConstantValue = false;
      } else if (contentAvailabilityCount == size) {
        contentAvailabilityConstantValue = true;
      }

      childSubtreeAvailabilityCount =
          getAvailabilityCount(childSubtreeAvailability, queryInput.getSubtreeLevels(), 1);
      if (childSubtreeAvailabilityCount == 0) {
        childSubtreeAvailabilityConstantValue = false;
      } else if (childSubtreeAvailabilityCount == MortonCode.size(queryInput.getSubtreeLevels())) {
        childSubtreeAvailabilityConstantValue = true;
      }
    }

    ImmutableSubtree.Builder builder = ImmutableSubtree.builder();

    int length =
        (Objects.nonNull(tileAvailabilityConstantValue) ? 0 : tileAvailability.length)
            + (Objects.nonNull(contentAvailabilityConstantValue) ? 0 : contentAvailability.length)
            + (Objects.nonNull(childSubtreeAvailabilityConstantValue)
                ? 0
                : childSubtreeAvailability.length);
    builder.addBuffers(ImmutableBuffer.builder().byteLength(length).build());

    int bitstream = 0;
    int byteOffset = 0;
    if (Objects.nonNull(tileAvailabilityConstantValue)) {
      builder.tileAvailability(
          ImmutableAvailability.builder().constant(tileAvailabilityConstantValue ? 1 : 0).build());
    } else {
      builder
          .tileAvailability(
              ImmutableAvailability.builder()
                  .bitstream(bitstream++)
                  .availabilityCount(tileAvailabilityCount)
                  .build())
          .addBufferViews(
              ImmutableBufferView.builder()
                  .buffer(0)
                  .byteOffset(byteOffset)
                  .byteLength(tileAvailability.length)
                  .build());
      byteOffset += tileAvailability.length;
    }
    if (Objects.nonNull(contentAvailabilityConstantValue)) {
      builder.contentAvailability(
          ImmutableList.of(
              ImmutableAvailability.builder()
                  .constant(contentAvailabilityConstantValue ? 1 : 0)
                  .build()));
    } else {
      builder
          .contentAvailability(
              ImmutableList.of(
                  ImmutableAvailability.builder()
                      .bitstream(bitstream++)
                      .availabilityCount(contentAvailabilityCount)
                      .build()))
          .addBufferViews(
              ImmutableBufferView.builder()
                  .buffer(0)
                  .byteOffset(byteOffset)
                  .byteLength(contentAvailability.length)
                  .build());
      byteOffset += contentAvailability.length;
    }
    if (Objects.nonNull(childSubtreeAvailabilityConstantValue)) {
      builder.childSubtreeAvailability(
          ImmutableAvailability.builder()
              .constant(childSubtreeAvailabilityConstantValue ? 1 : 0)
              .build());
    } else {
      builder
          .childSubtreeAvailability(
              ImmutableAvailability.builder()
                  .bitstream(bitstream)
                  .availabilityCount(childSubtreeAvailabilityCount)
                  .build())
          .addBufferViews(
              ImmutableBufferView.builder()
                  .buffer(0)
                  .byteOffset(byteOffset)
                  .byteLength(childSubtreeAvailability.length)
                  .build());
    }

    // TODO
    // List<PropertyTable> getPropertyTables();
    // Optional<Integer> getTileMetadata();
    // List<Integer> getContentMetadata();
    // MetadataEntity getSubtreeMetadata();

    logAvailability(
        tileAvailability,
        contentAvailability,
        childSubtreeAvailability,
        queryInput.getSubtreeLevels());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Tiles3dHelper.write3dTilesSubtreeBinary(
        builder.build(),
        Objects.nonNull(tileAvailabilityConstantValue) ? EMPTY : tileAvailability,
        Objects.nonNull(contentAvailabilityConstantValue) ? EMPTY : contentAvailability,
        Objects.nonNull(childSubtreeAvailabilityConstantValue) ? EMPTY : childSubtreeAvailability,
        baos);
    result = baos.toByteArray();
    return result;
  }

  private static void logAvailability(
      byte[] tileAvailability,
      byte[] contentAvailability,
      byte[] childSubtreeAvailability,
      int subtreeLevels) {
    LOGGER.debug(
        "Tile Availability: {}", getAvailabilityString(tileAvailability, 0, subtreeLevels - 1));
    LOGGER.debug(
        "Content Availability: {}",
        getAvailabilityString(contentAvailability, 0, subtreeLevels - 1));
    LOGGER.debug(
        "Child Subtree Availability: {}",
        getAvailabilityString(childSubtreeAvailability, subtreeLevels, subtreeLevels));
  }

  private static String getAvailabilityString(byte[] availability, int minLevel, int maxLevel) {
    StringBuilder s = new StringBuilder();
    int i0 = 0;
    for (int level = minLevel; level <= maxLevel; level++) {
      for (int i = 0; i < MortonCode.size(level); i++) {
        s.append(getAvailability(availability, minLevel, level, i) ? "1" : "0");
        if (i % 4 == 3) {
          s.append(" ");
        }
      }
      i0 += MortonCode.size(level);
      s.append(" / ");
    }
    return s.toString();
  }

  private static int getAvailabilityCount(byte[] availability, int minLevel, int levels) {
    int count = 0;
    for (int i = 0; i < levels; i++) {
      for (int j = 0; j < MortonCode.size(minLevel + i); j++) {
        if (getAvailability(availability, minLevel, minLevel + i, j)) {
          count++;
        }
      }
    }
    return count;
  }

  private static void processZ(
      QueryInputSubtree queryInput,
      FeaturesCoreQueriesHandler queriesHandler,
      int baseLevel,
      int xBase,
      int yBase,
      int level,
      int x0,
      int y0,
      byte[] tileAvailability,
      byte[] contentAvailability,
      byte[] childSubtreeAvailability) {

    if (level > queryInput.getMaxLevel() || level - baseLevel > queryInput.getSubtreeLevels()) {
      return;
    }

    Optional<Cql2Expression> additionalFilter;
    int i0 = MortonCode.encode(x0 - xBase, y0 - yBase);
    for (int i = 0; i < ((level - baseLevel) == 0 ? 1 : 4); i++) {
      int x1 = x0 + (i % 2);
      int y1 = y0 + (i / 2);
      BoundingBox bbox =
          Tiles3dHelper.computeBbox(
              queryInput.getApi(), queryInput.getCollectionId(), level, x1, y1);
      int relativeLevel = level - queryInput.getFirstLevelWithContent();
      additionalFilter =
          (relativeLevel >= 0 && queryInput.getTileFilters().size() > relativeLevel)
              ? Optional.of(queryInput.getTileFilters().get(relativeLevel))
              : Optional.empty();
      boolean hasData = Tiles3dHelper.hasData(queriesHandler, queryInput, bbox, additionalFilter);
      if (hasData) {
        // LOGGER.debug("Processing {}/{}/{}: TRUE", level, x1, y1);
        if (level - baseLevel < queryInput.getSubtreeLevels()) {
          setAvailability(tileAvailability, 0, level - baseLevel, i0 + i);
          if (relativeLevel >= 0) {
            if (queryInput.getContentFilters().isEmpty()) {
              setAvailability(contentAvailability, 0, level - baseLevel, i0 + i);
            } else {
              additionalFilter =
                  (queryInput.getContentFilters().size() > relativeLevel)
                      ? Optional.of(queryInput.getContentFilters().get(relativeLevel))
                      : Optional.empty();
              hasData = Tiles3dHelper.hasData(queriesHandler, queryInput, bbox, additionalFilter);
              if (hasData) {
                setAvailability(contentAvailability, 0, level - baseLevel, i0 + i);
              }
            }
          }

          processZ(
              queryInput,
              queriesHandler,
              baseLevel,
              xBase * 2,
              yBase * 2,
              level + 1,
              x1 * 2,
              y1 * 2,
              tileAvailability,
              contentAvailability,
              childSubtreeAvailability);
        } else {
          setAvailability(
              childSubtreeAvailability,
              queryInput.getSubtreeLevels(),
              queryInput.getSubtreeLevels(),
              i0 + i);
        }
      } else {
        // LOGGER.debug("Processing {}/{}/{}: FALSE", level, x1, y1);
      }
    }
  }

  private static void setAvailability(
      byte[] availability, int startLevel, int level, int idxLevel) {
    int idx = idxLevel;
    // add shift from previous levels
    for (int i = startLevel; i < level; i++) {
      idx += MortonCode.size(i);
    }
    int byteIndex = idx / 8;
    int bitIndex = idx % 8;
    availability[byteIndex] |= 1 << bitIndex;
  }

  private void clearAvailability(byte[] availability, int toLevel) {
    int idx = 0;
    for (int i = 0; i <= toLevel; i++) {
      idx += MortonCode.size(i);
    }
    int byteIndex = idx / 8;
    int bitIndex = idx % 8;
    for (int i = 0; i < byteIndex; i++) {
      availability[i] = 0;
    }
    for (int i = 0; i < bitIndex; i++) {
      availability[byteIndex] &= ~(1 << i);
    }
  }

  public static boolean getAvailability(
      byte[] availability, int startLevel, int level, int idxLevel) {
    int idx = idxLevel;
    // add shift from previous levels
    for (int i = startLevel; i < level; i++) {
      idx += MortonCode.size(i);
    }
    int byteIndex = idx / 8;
    int bitIndex = idx % 8;
    int bitValue = (availability[byteIndex] >> bitIndex) & 1;
    return bitValue == 1;
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

  public static byte[] intToLittleEndianLong(int v) {
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(v);
    return bb.array();
  }

  public static int littleEndianLongToInt(byte[] array, int offset) {
    ByteBuffer bb = ByteBuffer.allocate(8);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < 8; i++) {
      bb.put(array[offset + i]);
    }
    bb.rewind();
    return (int) bb.getLong();
  }

  public static Subtree readSubtree(byte[] content) throws IOException {
    return MAPPER.readValue(content, Subtree.class);
  }

  public static Subtree readSubtree(InputStream content) throws IOException {
    return MAPPER.readValue(content, Subtree.class);
  }

  public static double degToRad(double degree) {
    return degree / 180.0 * Math.PI;
  }

  public static Response getContent(
      FeaturesQuery featuresQuery,
      FeaturesCoreProviders providers,
      FeaturesCoreQueriesHandler queriesHandlerFeatures,
      TileResourceCache tileResourceCache,
      URICustomizer uriCustomizer,
      Tiles3dConfiguration cfg,
      TileResource r,
      Cql cql,
      Optional<QueryInput> queryInputGeneric,
      ApiMediaType mediaType)
      throws URISyntaxException {
    FeatureTypeConfigurationOgcApi collectionData =
        r.getApiData().getCollectionData(r.getCollectionId()).orElseThrow();

    BoundingBox bboxTile =
        Tiles3dHelper.computeBbox(
            r.getApi(), r.getCollectionId(), r.getLevel(), r.getX(), r.getY());

    String bboxString =
        String.format(
            Locale.US,
            "%f,%f,%f,%f",
            bboxTile.getXmin(),
            bboxTile.getYmin(),
            bboxTile.getXmax(),
            bboxTile.getYmax());

    FeatureQuery query =
        featuresQuery.requestToFeatureQuery(
            r.getApi(),
            collectionData,
            OgcCrs.CRS84h,
            ImmutableMap.of(),
            1,
            100000, // TODO
            100000, // TODO
            ImmutableMap.of("bbox", bboxString),
            ImmutableList.of());

    if (!cfg.getContentFilters().isEmpty()) {
      query =
          getFinalQuery(
              cfg.getContentFilters().get(r.getLevel() - cfg.getFirstLevelWithContent()),
              query,
              cql);
    }

    FeaturesCoreQueriesHandler.QueryInputFeatures queryInput;
    try {
      ImmutableQueryInputFeatures.Builder builder = new ImmutableQueryInputFeatures.Builder();
      queryInputGeneric.ifPresent(builder::from);
      queryInput =
          builder
              .collectionId(r.getCollectionId())
              .query(query)
              .featureProvider(providers.getFeatureProviderOrThrow(r.getApiData(), collectionData))
              .defaultCrs(OgcCrs.CRS84h)
              .defaultPageSize(Optional.of(100000)) // TODO
              .showsFeatureSelfLink(false)
              .saveContentAsFile(Optional.of(tileResourceCache.getFile(r)))
              .build();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    ApiRequestContext requestContextGltf =
        new Builder()
            .api(r.getApi())
            .requestUri(
                uriCustomizer
                    .copy()
                    .removeLastPathSegments(5)
                    .ensureLastPathSegment("items")
                    .clearParameters()
                    .addParameter("f", "glb")
                    .addParameter("bbox", bboxString)
                    .build())
            .mediaType(mediaType)
            .alternateMediaTypes(ImmutableList.of())
            .build();

    return queriesHandlerFeatures.handle(Query.FEATURES, queryInput, requestContextGltf);
  }

  private static FeatureQuery getFinalQuery(String filter, FeatureQuery query, Cql cql) {
    return ImmutableFeatureQuery.builder()
        .from(query)
        .filter(And.of(query.getFilter().orElseThrow(), cql.read(filter, Format.TEXT)))
        .build();
  }
}
