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

  JsonSchemaArray COORDINATES_ARRAY =
      ImmutableJsonSchemaArray.builder()
          .minItems(2)
          .maxItems(3)
          .items(ImmutableJsonSchemaNumber.builder().build())
          .build();

  String TYPE = "type";
  String COORDINATES = "coordinates";
  JsonSchemaObject POINT =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON Point")
          .addRequired(TYPE, COORDINATES)
          .putProperties(TYPE, getEnum("Point"))
          .putProperties(COORDINATES, COORDINATES_ARRAY)
          .build();

  JsonSchemaObject MULTI_POINT =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON MultiPoint")
          .addRequired(TYPE, COORDINATES)
          .putProperties(TYPE, getEnum("MultiPoint"))
          .putProperties(
              COORDINATES, ImmutableJsonSchemaArray.builder().items(COORDINATES_ARRAY).build())
          .build();

  JsonSchemaObject LINE_STRING =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON LineString")
          .addRequired(TYPE, COORDINATES)
          .putProperties(TYPE, getEnum("LineString"))
          .putProperties(
              COORDINATES,
              ImmutableJsonSchemaArray.builder().minItems(2).items(COORDINATES_ARRAY).build())
          .build();

  JsonSchemaObject MULTI_LINE_STRING =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON MultiLineString")
          .addRequired(TYPE, COORDINATES)
          .putProperties(TYPE, getEnum("MultiLineString"))
          .putProperties(
              COORDINATES,
              ImmutableJsonSchemaArray.builder()
                  .items(
                      ImmutableJsonSchemaArray.builder()
                          .minItems(2)
                          .items(COORDINATES_ARRAY)
                          .build())
                  .build())
          .build();

  JsonSchemaObject POLYGON =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON Polygon")
          .addRequired(TYPE, COORDINATES)
          .putProperties(TYPE, getEnum("Polygon"))
          .putProperties(
              COORDINATES,
              ImmutableJsonSchemaArray.builder()
                  .minItems(1)
                  .items(
                      ImmutableJsonSchemaArray.builder()
                          .minItems(4)
                          .items(COORDINATES_ARRAY)
                          .build())
                  .build())
          .build();

  JsonSchemaObject MULTI_POLYGON =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON MultiPolygon")
          .addRequired(TYPE, COORDINATES)
          .putProperties(TYPE, getEnum("MultiPolygon"))
          .putProperties(
              COORDINATES,
              ImmutableJsonSchemaArray.builder()
                  .items(
                      ImmutableJsonSchemaArray.builder()
                          .minItems(1)
                          .items(
                              ImmutableJsonSchemaArray.builder()
                                  .minItems(4)
                                  .items(COORDINATES_ARRAY)
                                  .build())
                          .build())
                  .build())
          .build();

  JsonSchemaOneOf GEOMETRY =
      ImmutableJsonSchemaOneOf.builder()
          .title("GeoJSON Geometry")
          .addOneOf(POINT, MULTI_POINT, LINE_STRING, MULTI_LINE_STRING, POLYGON, MULTI_POLYGON)
          .build();

  JsonSchemaObject GEOMETRY_COLLECTION =
      ImmutableJsonSchemaObject.builder()
          .title("GeoJSON GeometryCollection")
          .addRequired(TYPE, "geometries")
          .putProperties(TYPE, getEnum("GeometryCollection"))
          .putProperties("geometries", ImmutableJsonSchemaArray.builder().items(GEOMETRY).build())
          .build();

  JsonSchemaObject LINK_JSON =
      ImmutableJsonSchemaObject.builder()
          .putProperties(
              "href", ImmutableJsonSchemaString.builder().format("uri-reference").build())
          .putProperties("rel", ImmutableJsonSchemaString.builder().build())
          .putProperties(TYPE, ImmutableJsonSchemaString.builder().build())
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
