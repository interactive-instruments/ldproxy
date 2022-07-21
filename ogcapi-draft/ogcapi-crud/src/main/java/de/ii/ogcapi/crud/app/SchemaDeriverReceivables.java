/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaFalse;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaObject;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaBuildingBlocks;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.features.core.domain.JsonSchemaObject;
import de.ii.ogcapi.features.geojson.domain.SchemaDeriverReturnables;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchemaDeriverReceivables extends SchemaDeriverReturnables {

  private final boolean removeId;
  private final boolean allOptional;
  private final boolean allNonRequiredNullable;
  private final boolean strict;

  // TODO strict parameter ("additionalProperties": false)
  public SchemaDeriverReceivables(
      VERSION version,
      Optional<String> schemaUri,
      String label,
      Optional<String> description,
      List<Codelist> codelists,
      boolean removeId,
      boolean allOptional,
      boolean allNonRequiredNullable,
      boolean strict) {
    super(version, schemaUri, label, description, codelists);
    this.removeId = removeId;
    this.allOptional = allOptional;
    this.allNonRequiredNullable = allNonRequiredNullable;
    this.strict = strict;
  }

  @Override
  protected void adjustObjectSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      List<String> required,
      ImmutableJsonSchemaObject.Builder builder) {
    super.adjustObjectSchema(schema, properties, required, builder);

    if (allOptional) {
      builder.required(List.of());
    }

    if (allNonRequiredNullable) {
      Map<String, JsonSchema> newProperties = new LinkedHashMap<>();
      properties.forEach(
          (key, value) -> {
            if (!required.contains(key)) {
              newProperties.put(key, JsonSchemaBuildingBlocks.nullable(value));
            } else {
              newProperties.put(key, value);
            }
          });
      builder.properties(newProperties);
    }

    if (strict) {
      builder.additionalProperties(ImmutableJsonSchemaFalse.builder().build());
    }
  }

  @Override
  protected void adjustRootSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      Map<String, JsonSchema> defs,
      List<String> required,
      JsonSchemaDocument.Builder builder) {
    super.adjustRootSchema(schema, properties, defs, required, builder);

    if (allOptional) {
      builder.required(List.of());
    }

    JsonSchemaDocument schemaDocument = builder.build();
    Map<String, JsonSchema> newProperties = new LinkedHashMap<>(schemaDocument.getProperties());
    newProperties.remove("links");

    if (removeId) {
      newProperties.remove("id");

      List<String> newRequired = new ArrayList<>(schemaDocument.getRequired());
      newRequired.remove("id");
      builder.required(newRequired);
    }

    if (allNonRequiredNullable) {
      JsonSchemaObject props = (JsonSchemaObject) newProperties.get("properties");
      Map<String, JsonSchema> newProps = new LinkedHashMap<>();

      props
          .getProperties()
          .forEach(
              (key, value) -> {
                if (!props.getRequired().contains(key)) {
                  JsonSchema prop =
                      modify(
                          value,
                          propBuilder ->
                              propBuilder.title(Optional.empty()).description(Optional.empty()));
                  JsonSchema nullableProp = JsonSchemaBuildingBlocks.nullable(prop);
                  nullableProp =
                      modify(
                          nullableProp,
                          propBuilder ->
                              propBuilder
                                  .title(value.getTitle())
                                  .description(value.getDescription()));

                  newProps.put(key, nullableProp);
                } else {
                  newProps.put(key, value);
                }
              });

      newProperties.put(
          "properties",
          ImmutableJsonSchemaObject.builder()
              .from(props)
              .required(allOptional ? List.of() : props.getRequired())
              .properties(newProps)
              .build());
    }

    if (strict) {
      builder.additionalProperties(ImmutableJsonSchemaFalse.builder().build());

      newProperties.put(
          "properties",
          ImmutableJsonSchemaObject.builder()
              .from(newProperties.get("properties"))
              .additionalProperties(ImmutableJsonSchemaFalse.builder().build())
              .build());
    }

    builder.properties(newProperties);
  }
}
