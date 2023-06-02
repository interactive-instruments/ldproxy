/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.Geometry.Coordinate;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.json.domain.FeatureTokenDecoderGeoJson;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.threeten.extra.Interval;

public class GeoJsonHelper {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static Optional<BoundingBox> getSpatialExtent(JsonNode feature, EpsgCrs crs) {
    JsonNode node = feature;
    if (node.isObject() && node.has("geometry")) {
      node = node.get("geometry");
      if (node.isObject() && node.has("type")) {
        int level;
        switch (node.get("type").asText()) {
          case "Point":
            level = 0;
            break;
          case "MultiPoint":
          case "LineString":
            level = 1;
            break;
          case "MultiLineString":
          case "Polygon":
            level = 2;
            break;
          case "MultiPolygon":
            level = 3;
            break;
          default:
            return Optional.empty();
            // TODO support GeometryCollection?
        }
        if (node.isObject() && node.has("coordinates")) {
          node = node.get("coordinates");
          if (node.isArray()) {
            List<ArrayNode> nodes = ImmutableList.of((ArrayNode) node);
            while (level-- > 0) {
              nodes = flatten(nodes);
            }
            List<Coordinate> coords =
                nodes.stream()
                    .map(
                        n ->
                            n.size() == 2
                                ? Coordinate.of(n.get(0).asDouble(), n.get(1).asDouble())
                                : Coordinate.of(
                                    n.get(0).asDouble(), n.get(1).asDouble(), n.get(2).asDouble()))
                    .collect(Collectors.toUnmodifiableList());
            Optional<Coordinate> min =
                coords.stream()
                    .reduce(
                        (coord, coord2) ->
                            Coordinate.of(
                                Math.min(coord.x, coord2.x),
                                Math.min(coord.y, coord2.y),
                                Math.min(coord.z, coord2.z)));
            Optional<Coordinate> max =
                coords.stream()
                    .reduce(
                        (coord, coord2) ->
                            Coordinate.of(
                                Math.max(coord.x, coord2.x),
                                Math.max(coord.y, coord2.y),
                                Math.max(coord.z, coord2.z)));
            return min.map(
                m ->
                    m.size() == 2
                        ? BoundingBox.of(m.x, m.y, max.get().x, max.get().y, crs)
                        : BoundingBox.of(
                            m.x, m.y, m.z, max.get().x, max.get().y, max.get().z, crs));
          }
        }
      }
    }
    return Optional.empty();
  }

  public static Optional<Interval> getTemporalExtent(
      JsonNode feature, Optional<FeatureSchema> schema, Optional<String> flatten) {
    return schema
        .flatMap(SchemaBase::getPrimaryInstant)
        .map(
            instant -> {
              List<String> t =
                  flatten
                      .map(
                          separator ->
                              (List<String>)
                                  ImmutableList.of(instant.getFullPathAsString(separator)))
                      .orElse(instant.getFullPath());
              return Tuple.of(t, t);
            })
        .or(
            () ->
                schema
                    .flatMap(SchemaBase::getPrimaryInterval)
                    .map(
                        interval -> {
                          List<String> t1 =
                              flatten
                                  .map(
                                      separator ->
                                          (List<String>)
                                              ImmutableList.of(
                                                  interval.first().getFullPathAsString(separator)))
                                  .orElse(interval.first().getFullPath());
                          List<String> t2 =
                              flatten
                                  .map(
                                      separator ->
                                          (List<String>)
                                              ImmutableList.of(
                                                  interval.second().getFullPathAsString(separator)))
                                  .orElse(interval.second().getFullPath());
                          return Tuple.of(t1, t2);
                        }))
        .map(
            beginEnd -> {
              ObjectNode properties = (ObjectNode) feature.get("properties");
              return Interval.of(
                  getInstant(properties, beginEnd.first()),
                  getInstant(properties, beginEnd.second()));
            });
  }

  private static Instant getInstant(ObjectNode properties, List<String> path) {
    ObjectNode current = properties;
    JsonNode next;
    int i = 0;
    while (i < path.size()) {
      next = current.get(path.get(i++));
      if (next == null) {
        return null;
      }
      try {
        if (i == path.size()) {
          return Instant.parse(next.textValue());
        } else {
          current = (ObjectNode) next;
        }
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  static List<ArrayNode> flatten(List<ArrayNode> nodes) {
    ImmutableList.Builder<ArrayNode> builder = ImmutableList.builder();
    return nodes.stream()
        .map(
            node -> {
              for (int i = 0; i < node.size(); i++) {
                builder.add((ArrayNode) node.get(i));
              }
              return builder.build();
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  static Optional<Interval> convertTemporalExtent(Optional<Tuple<Long, Long>> interval) {
    if (interval.isEmpty()) {
      return Optional.empty();
    }

    Long begin = interval.get().first();
    Long end = interval.get().second();

    Instant beginInstant = Objects.nonNull(begin) ? Instant.ofEpochMilli(begin) : Instant.MIN;
    Instant endInstant = Objects.nonNull(end) ? Instant.ofEpochMilli(end) : Instant.MAX;

    return Optional.of(Interval.of(beginInstant, endInstant));
  }

  // TODO: to InputFormat extension matching the mediaType
  static FeatureTokenSource getFeatureSource(
      ApiMediaType mediaType, InputStream requestBody, Optional<String> nullValue) {

    FeatureTokenDecoderGeoJson featureTokenDecoderGeoJson =
        new FeatureTokenDecoderGeoJson(nullValue);

    return Source.inputStream(requestBody).via(featureTokenDecoderGeoJson);
  }

  static JsonNode parseFeatureResponse(Response feature) {
    JsonNode content;
    try {
      content = MAPPER.readTree((byte[]) feature.getEntity());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return content;
  }
}
