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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeatures;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext.Builder;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.tiles3d.domain.QueriesHandler3dTiles.QueryInputSubtree;
import de.ii.ogcapi.tiles3d.domain.Subtree;
import de.ii.xtraplatform.cql.domain.Geometry.Envelope;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.SIntersects;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO cleanup, consolidate, harmonize code

public class Tiles3dHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(Tiles3dHelper.class);
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          // for debugging
          // .enable(SerializationFeature.INDENT_OUTPUT)
          .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

  public static final byte[] MAGIC_SUBT = new byte[] {0x73, 0x75, 0x62, 0x74};
  public static final byte[] VERSION_1 = new byte[] {0x01, 0x00, 0x00, 0x00};
  public static final byte[] JSON = new byte[] {0x4a, 0x53, 0x4f, 0x4e};
  public static final byte[] BIN = new byte[] {0x42, 0x49, 0x4e, 0x00};
  public static final byte[] JSON_PADDING = new byte[] {0x20};
  public static final byte[] BIN_PADDING = new byte[] {0x00};

  private static byte[] getJson(Object obj) throws JsonProcessingException {
    return MAPPER.writeValueAsBytes(obj);
  }

  public static void write3dTilesSubtreeBinary(
      Subtree subtree,
      ByteArrayOutputStream bufferTilesAvailability,
      ByteArrayOutputStream bufferContentAvailability,
      ByteArrayOutputStream bufferChildSubtreeAvailability,
      OutputStream outputStream) {

    byte[] json;
    try {
      json = getJson(subtree);
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

      outputStream.write(intToLittleEndianLong(json.length + jsonPadding));
      outputStream.write(intToLittleEndianLong(bufferLength + bufferPadding));

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
      throw new IllegalStateException("Could not write 3D Tiles Subtree output", e);
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

  public static double degToRad(double degree) {
    return degree / 180.0 * Math.PI;
  }
}
