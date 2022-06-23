/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

public interface JsonSchemaBuildingBlocks {

  JsonSchemaNull NULL = ImmutableJsonSchemaNull.builder().build();

  JsonSchemaArray COORDINATES =
      ImmutableJsonSchemaArray.builder()
          .minItems(2)
          .maxItems(3)
          .items(ImmutableJsonSchemaNumber.builder().build())
          .build();

  JsonSchemaObject POINT =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON Point")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("Point"))
          .putProperties("coordinates", COORDINATES)
          .build();

  JsonSchemaObject MULTI_POINT =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON MultiPoint")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("MultiPoint"))
          .putProperties(
              "coordinates", ImmutableJsonSchemaArray.builder().items(COORDINATES).build())
          .build();

  JsonSchemaObject LINE_STRING =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON LineString")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("LineString"))
          .putProperties(
              "coordinates",
              ImmutableJsonSchemaArray.builder().minItems(2).items(COORDINATES).build())
          .build();

  JsonSchemaObject MULTI_LINE_STRING =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON MultiLineString")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("MultiLineString"))
          .putProperties(
              "coordinates",
              ImmutableJsonSchemaArray.builder()
                  .items(ImmutableJsonSchemaArray.builder().minItems(2).items(COORDINATES).build())
                  .build())
          .build();

  JsonSchemaObject POLYGON =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON Polygon")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("Polygon"))
          .putProperties(
              "coordinates",
              ImmutableJsonSchemaArray.builder()
                  .minItems(1)
                  .items(ImmutableJsonSchemaArray.builder().minItems(4).items(COORDINATES).build())
                  .build())
          .build();

  JsonSchemaObject MULTI_POLYGON =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON MultiPolygon")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("MultiPolygon"))
          .putProperties(
              "coordinates",
              ImmutableJsonSchemaArray.builder()
                  .items(
                      ImmutableJsonSchemaArray.builder()
                          .minItems(1)
                          .items(
                              ImmutableJsonSchemaArray.builder()
                                  .minItems(4)
                                  .items(COORDINATES)
                                  .build())
                          .build())
                  .build())
          .build();

  // TODO add additional JSON-FG geometries

  JsonSchemaOneOf GEOMETRY =
      ImmutableJsonSchemaOneOf.builder()
          .title("GeoJSON Geometry")
          .addOneOf(POINT, MULTI_POINT, LINE_STRING, MULTI_LINE_STRING, POLYGON, MULTI_POLYGON)
          .build();

  JsonSchemaObject GEOMETRY_COLLECTION =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON GeometryCollection")
          .addRequired("type", "geometries")
          .putProperties("type", getEnum("GeometryCollection"))
          .putProperties("geometries", ImmutableJsonSchemaArray.builder().items(GEOMETRY).build())
          .build();

  JsonSchemaObject LINK_JSON =
      ImmutableJsonSchemaObject.builder()
          .putProperties(
              "href", ImmutableJsonSchemaString.builder().format("uri-reference").build())
          .putProperties("rel", ImmutableJsonSchemaString.builder().build())
          .putProperties("type", ImmutableJsonSchemaString.builder().build())
          .putProperties("title", ImmutableJsonSchemaString.builder().build())
          .addRequired("href")
          .build();

  static JsonSchemaString getEnum(String value) {
    return ImmutableJsonSchemaString.builder().addEnums(value).build();
  }

  static JsonSchemaOneOf nullable(JsonSchema schema) {
    if (schema instanceof JsonSchemaOneOf) {
      return ImmutableJsonSchemaOneOf.builder()
          .addOneOf(NULL)
          .addAllOneOf(((JsonSchemaOneOf) schema).getOneOf())
          .build();
    }

    return ImmutableJsonSchemaOneOf.builder().addOneOf(NULL, schema).build();
  }
}
