/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

public interface JsonSchemaBuildingBlocks {

  JsonSchemaNull NULL = new ImmutableJsonSchemaNull.Builder().build();

  JsonSchemaArray COORDINATES =
      new ImmutableJsonSchemaArray.Builder()
          .minItems(2)
          .maxItems(3)
          .items(new ImmutableJsonSchemaNumber.Builder().build())
          .build();

  JsonSchemaObject POINT =
      new ImmutableJsonSchemaObject.Builder()
          .title("GeoJSON Point")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("Point"))
          .putProperties("coordinates", COORDINATES)
          .build();

  JsonSchemaObject MULTI_POINT =
      new ImmutableJsonSchemaObject.Builder()
          .title("GeoJSON MultiPoint")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("MultiPoint"))
          .putProperties(
              "coordinates", new ImmutableJsonSchemaArray.Builder().items(COORDINATES).build())
          .build();

  JsonSchemaObject LINE_STRING =
      new ImmutableJsonSchemaObject.Builder()
          .title("GeoJSON LineString")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("LineString"))
          .putProperties(
              "coordinates",
              new ImmutableJsonSchemaArray.Builder().minItems(2).items(COORDINATES).build())
          .build();

  JsonSchemaObject MULTI_LINE_STRING =
      new ImmutableJsonSchemaObject.Builder()
          .title("GeoJSON MultiLineString")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("MultiLineString"))
          .putProperties(
              "coordinates",
              new ImmutableJsonSchemaArray.Builder()
                  .items(
                      new ImmutableJsonSchemaArray.Builder().minItems(2).items(COORDINATES).build())
                  .build())
          .build();

  JsonSchemaObject POLYGON =
      new ImmutableJsonSchemaObject.Builder()
          .title("GeoJSON Polygon")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("Polygon"))
          .putProperties(
              "coordinates",
              new ImmutableJsonSchemaArray.Builder()
                  .minItems(1)
                  .items(
                      new ImmutableJsonSchemaArray.Builder().minItems(4).items(COORDINATES).build())
                  .build())
          .build();

  JsonSchemaObject MULTI_POLYGON =
      new ImmutableJsonSchemaObject.Builder()
          .title("GeoJSON MultiPolygon")
          .addRequired("type", "coordinates")
          .putProperties("type", getEnum("MultiPolygon"))
          .putProperties(
              "coordinates",
              new ImmutableJsonSchemaArray.Builder()
                  .items(
                      new ImmutableJsonSchemaArray.Builder()
                          .minItems(1)
                          .items(
                              new ImmutableJsonSchemaArray.Builder()
                                  .minItems(4)
                                  .items(COORDINATES)
                                  .build())
                          .build())
                  .build())
          .build();

  // TODO add additional JSON-FG geometries

  JsonSchemaOneOf GEOMETRY =
      new ImmutableJsonSchemaOneOf.Builder()
          .title("GeoJSON Geometry")
          .addOneOf(POINT, MULTI_POINT, LINE_STRING, MULTI_LINE_STRING, POLYGON, MULTI_POLYGON)
          .build();

  JsonSchemaObject GEOMETRY_COLLECTION =
      new ImmutableJsonSchemaObject.Builder()
          .title("GeoJSON GeometryCollection")
          .addRequired("type", "geometries")
          .putProperties("type", getEnum("GeometryCollection"))
          .putProperties(
              "geometries", new ImmutableJsonSchemaArray.Builder().items(GEOMETRY).build())
          .build();

  JsonSchemaObject LINK_JSON =
      new ImmutableJsonSchemaObject.Builder()
          .putProperties(
              "href", new ImmutableJsonSchemaString.Builder().format("uri-reference").build())
          .putProperties("rel", new ImmutableJsonSchemaString.Builder().build())
          .putProperties("type", new ImmutableJsonSchemaString.Builder().build())
          .putProperties("title", new ImmutableJsonSchemaString.Builder().build())
          .addRequired("href")
          .build();

  static JsonSchemaString getEnum(String value) {
    return new ImmutableJsonSchemaString.Builder().addEnums(value).build();
  }

  static JsonSchemaOneOf nullable(JsonSchema schema) {
    if (schema instanceof JsonSchemaOneOf) {
      return new ImmutableJsonSchemaOneOf.Builder()
          .addOneOf(NULL)
          .addAllOneOf(((JsonSchemaOneOf) schema).getOneOf())
          .build();
    }

    return new ImmutableJsonSchemaOneOf.Builder().addOneOf(NULL, schema).build();
  }
}
