/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaArray;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaRef;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaRefV7;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaString;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaDocument.VERSION;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class SchemaDeriverReturnables extends SchemaDeriverJsonSchema {

  public SchemaDeriverReturnables(
      VERSION version, Optional<String> schemaUri,
      String label, Optional<String> description,
      List<Codelist> codelists) {
    super(version, schemaUri, label, description, codelists);
  }

  @Override
  protected void visitObjectSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      List<String> required, ImmutableJsonSchemaObject.Builder builder) {

    builder.properties(properties);
    builder.required(required);
  }

  @Override
  protected JsonSchema visitGeometry(FeatureSchema schema, JsonSchema jsonSchema) {
    boolean required = schema.getConstraints().flatMap(SchemaConstraints::getRequired)
        .orElse(false);

    if (!required) {
      return GeoJsonSchema.nullable(jsonSchema);
    }

    return jsonSchema;
  }

  @Override
  protected void visitFeatureSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      Map<String, JsonSchema> defs,
      List<String> required, JsonSchemaDocument.Builder builder) {

    JsonSchemaRef linkRef = version == JsonSchemaDocument.VERSION.V7
        ? ImmutableJsonSchemaRefV7.builder()
            .objectType("Link")
            .build()
        : ImmutableJsonSchemaRef.builder()
            .objectType("Link")
            .build();

    defs.forEach((name, def) -> {
      if ("Link".equals(name)) {
        builder.putDefinitions("Link", SchemaGeneratorFeatureGeoJson.LINK_JSON);
      } else {
        builder.putDefinitions(name, def);
      }
    });

    builder.putProperties("type", ImmutableJsonSchemaString.builder()
        .addEnums("Feature")
        .build());

    builder.putProperties("links", ImmutableJsonSchemaArray.builder()
        .items(linkRef)
        .build());

    findByRole(properties, Role.ID)
        .ifPresent(jsonSchema -> builder.putProperties("id", jsonSchema));

    findByRole(properties, Role.PRIMARY_GEOMETRY)
        .ifPresentOrElse(
            jsonSchema -> builder.putProperties("geometry", jsonSchema),
            () -> builder.putProperties("geometry", GeoJsonSchema.NULL));

    Map<String, JsonSchema> propertiesWithoutRoles = withoutRoles(properties, Role.ID,
        Role.PRIMARY_GEOMETRY);

    builder.putProperties("properties", ImmutableJsonSchemaObject.builder()
        .required(required)
        .properties(withoutFlattenedArrays(propertiesWithoutRoles))
        .patternProperties(onlyFlattenedArraysAsPatterns(propertiesWithoutRoles))
        .build());

    builder.addRequired("type", "geometry", "properties");
  }

  private Map<String, JsonSchema> onlyFlattenedArraysAsPatterns(
      Map<String, JsonSchema> properties) {
    return properties.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getName().filter(name -> name.contains("[]")).isPresent())
        .map(entry -> new SimpleImmutableEntry<>(flattenedArrayNameAsPattern(entry.getKey()), entry.getValue()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String flattenedArrayNameAsPattern(String name) {
    return "^" + name.replace("[]","\\.\\d+") + "$";
  }

  private Map<String, JsonSchema> withoutFlattenedArrays(
      Map<String, JsonSchema> properties) {
    return properties.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getName().filter(name -> !name.contains("[]")).isPresent())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  //TODO: nested
  private Map<String, JsonSchema> withoutRoles(Map<String, JsonSchema> properties, Role... roles) {
    return properties.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getName().filter(name -> Arrays.stream(roles).noneMatch(role -> nameHasRole(name, role))).isPresent())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Optional<JsonSchema> findByRole(Map<String, JsonSchema> properties, Role role) {
    return properties.values().stream()
        .flatMap(jsonSchema -> {
          if (jsonSchema instanceof JsonSchemaObject) {
            return ((JsonSchemaObject) jsonSchema).getProperties()
                .values()
                .stream();
          }
          return Stream.of(jsonSchema);
        })
        .filter(jsonSchema -> jsonSchema.getName().filter(name -> nameHasRole(name, role)).isPresent())
        .findFirst();
  }
}
