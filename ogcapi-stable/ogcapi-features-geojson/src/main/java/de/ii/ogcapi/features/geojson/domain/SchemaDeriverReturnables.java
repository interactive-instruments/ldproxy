/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaArray;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaObject;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaRef;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaRefV7;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaString;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaBuildingBlocks;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.features.core.domain.JsonSchemaRef;
import de.ii.ogcapi.features.core.domain.SchemaDeriverJsonSchema;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SchemaDeriverReturnables extends SchemaDeriverJsonSchema {

  public SchemaDeriverReturnables(
      VERSION version,
      Optional<String> schemaUri,
      String label,
      Optional<String> description,
      List<Codelist> codelists) {
    super(version, schemaUri, label, description, codelists, true);
  }

  @Override
  protected void adjustObjectSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      List<String> required,
      ImmutableJsonSchemaObject.Builder builder) {

    builder.properties(properties);
    builder.required(required);
  }

  @Override
  protected JsonSchema adjustGeometry(FeatureSchema schema, JsonSchema jsonSchema) {
    boolean required =
        schema.getConstraints().flatMap(SchemaConstraints::getRequired).orElse(false);

    if (!required) {
      return JsonSchemaBuildingBlocks.nullable(jsonSchema);
    }

    return jsonSchema;
  }

  @Override
  protected void adjustRootSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      Map<String, JsonSchema> defs,
      List<String> required,
      JsonSchemaDocument.Builder builder) {

    JsonSchemaRef linkRef =
        version == JsonSchemaDocument.VERSION.V7
            ? ImmutableJsonSchemaRefV7.builder().objectType("Link").build()
            : new ImmutableJsonSchemaRef.Builder().objectType("Link").build();

    builder.putDefinitions("Link", JsonSchemaBuildingBlocks.LINK_JSON);

    defs.forEach(
        (name, def) -> {
          if (!Objects.equals("Link", name)) {
            builder.putDefinitions(name, def);
          }
        });

    builder.putProperties(
        "type", new ImmutableJsonSchemaString.Builder().addEnums("Feature").build());

    builder.putProperties("links", new ImmutableJsonSchemaArray.Builder().items(linkRef).build());

    findByRole(properties, Role.ID)
        .ifPresent(
            jsonSchema -> {
              if (jsonSchema.isRequired()) {
                jsonSchema.getName().ifPresent(required::remove);
                builder.addRequired("id");
              }
              builder.putProperties("id", jsonSchema);
            });

    findByRole(properties, Role.PRIMARY_GEOMETRY)
        .or(() -> findByRole(properties, Role.SECONDARY_GEOMETRY))
        .ifPresentOrElse(
            jsonSchema -> {
              if (jsonSchema.isRequired()) {
                jsonSchema.getName().ifPresent(required::remove);
              }
              builder.putProperties("geometry", jsonSchema);
            },
            () -> builder.putProperties("geometry", JsonSchemaBuildingBlocks.NULL));

    Map<String, JsonSchema> propertiesWithoutRoles = withoutRoles(properties);

    builder.putProperties(
        "properties",
        new ImmutableJsonSchemaObject.Builder()
            .required(required)
            .properties(withoutFlattenedArrays(propertiesWithoutRoles))
            .patternProperties(onlyFlattenedArraysAsPatterns(propertiesWithoutRoles))
            .build());

    builder.addRequired("type", "geometry", "properties");
  }

  private Map<String, JsonSchema> onlyFlattenedArraysAsPatterns(
      Map<String, JsonSchema> properties) {
    return properties.entrySet().stream()
        .filter(entry -> entry.getValue().getName().filter(name -> name.contains("[]")).isPresent())
        .map(
            entry ->
                new SimpleImmutableEntry<>(
                    flattenedArrayNameAsPattern(entry.getKey()), entry.getValue()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String flattenedArrayNameAsPattern(String name) {
    return "^" + name.replace("[]", "\\.\\d+") + "$";
  }

  private Map<String, JsonSchema> withoutFlattenedArrays(Map<String, JsonSchema> properties) {
    return properties.entrySet().stream()
        .filter(
            entry -> entry.getValue().getName().filter(name -> !name.contains("[]")).isPresent())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
