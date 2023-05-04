/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaDeriver;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SchemaDeriverJsonSchema extends SchemaDeriver<JsonSchema> {

  protected final VERSION version;
  private final Optional<String> schemaUri;
  private final String label;
  private final Optional<String> description;
  private final boolean useCodelistKeys;

  public SchemaDeriverJsonSchema(
      VERSION version,
      Optional<String> schemaUri,
      String label,
      Optional<String> description,
      List<Codelist> codelists,
      boolean useCodelistKeys) {
    super(codelists);
    this.version = version;
    this.schemaUri = schemaUri;
    this.label = label;
    this.description = description;
    this.useCodelistKeys = useCodelistKeys;
  }

  @Override
  protected Optional<String> getPropertyName(JsonSchema property) {
    return property.getName();
  }

  @Override
  protected boolean isPropertyRequired(JsonSchema property) {
    return property.isRequired();
  }

  @Override
  protected Map<String, JsonSchema> getNestedProperties(JsonSchema property) {
    return property instanceof JsonSchemaObject
        ? ((JsonSchemaObject) property).getProperties()
        : ImmutableMap.of();
  }

  @Override
  protected JsonSchema buildRootSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      Map<String, JsonSchema> definitions,
      List<String> requiredProperties) {

    JsonSchemaDocument.Builder builder =
        version == VERSION.V7
            ? ImmutableJsonSchemaDocumentV7.builder()
            : ImmutableJsonSchemaDocument.builder();

    builder
        .id(schemaUri)
        .title(label)
        .description(description.orElse(schema.getDescription().orElse("")));

    adjustRootSchema(schema, properties, definitions, requiredProperties, builder);

    return builder.build();
  }

  protected void adjustRootSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      Map<String, JsonSchema> definitions,
      List<String> requiredProperties,
      JsonSchemaDocument.Builder rootBuilder) {}

  @Override
  protected Stream<JsonSchema> extractDefinitions(Stream<JsonSchema> properties) {
    return properties
        .filter(property -> Objects.nonNull(property) && property.getName().isPresent())
        .map(
            property ->
                property instanceof JsonSchemaArray
                    ? ((JsonSchemaArray) property).getItems()
                    : property)
        .filter(property -> property instanceof JsonSchemaRef && property.getName().isPresent())
        .filter(
            ref ->
                Objects.nonNull(((JsonSchemaRef) ref).getDef())
                    && ((JsonSchemaRef) ref).getDef().getName().isPresent())
        .map(ref -> ((JsonSchemaRef) ref).getDef())
        .flatMap(
            def ->
                def instanceof JsonSchemaObject
                    ? Stream.concat(
                        Stream.of(def),
                        extractDefinitions(
                            ((JsonSchemaObject) def).getProperties().values().stream()))
                    : Stream.of(def));
  }

  @Override
  protected JsonSchema buildObjectSchema(
      FeatureSchema schema, Map<String, JsonSchema> properties, List<String> requiredProperties) {
    ImmutableJsonSchemaObject.Builder builder =
        new ImmutableJsonSchemaObject.Builder()
            .name(schema.getName())
            .title(schema.getLabel())
            .description(schema.getDescription());

    adjustObjectSchema(schema, properties, requiredProperties, builder);

    return builder.build();
  }

  protected void adjustObjectSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      List<String> requiredProperties,
      ImmutableJsonSchemaObject.Builder objectBuilder) {
    objectBuilder.properties(properties);
    objectBuilder.required(requiredProperties);
  }

  @Override
  protected JsonSchema getSchemaForLiteralType(
      Type type, Optional<String> label, Optional<String> description, Optional<String> unit) {
    switch (type) {
      case INTEGER:
        return new ImmutableJsonSchemaInteger.Builder()
            .title(label)
            .description(description)
            .unit(unit)
            .build();
      case FLOAT:
        return new ImmutableJsonSchemaNumber.Builder()
            .title(label)
            .description(description)
            .unit(unit)
            .build();
      case BOOLEAN:
        return new ImmutableJsonSchemaBoolean.Builder()
            .title(label)
            .description(description)
            .build();
      case DATETIME:
        return new ImmutableJsonSchemaString.Builder()
            // validators will ignore this information as it isn't a well-known format value
            .format("date-time")
            .title(label)
            .description(description)
            .build();
      case DATE:
        return new ImmutableJsonSchemaString.Builder()
            // validators will ignore this information as it isn't a well-known format value
            .format("date")
            .title(label)
            .description(description)
            .build();
      case STRING:
      default:
        return new ImmutableJsonSchemaString.Builder()
            .title(label)
            .description(description)
            .unit(unit)
            .build();
    }
  }

  @Override
  protected JsonSchema getSchemaForGeometry(FeatureSchema schema) {
    JsonSchema jsonSchema;
    switch (schema.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
      case POINT:
        jsonSchema = JsonSchemaBuildingBlocks.POINT;
        break;
      case MULTI_POINT:
        jsonSchema = JsonSchemaBuildingBlocks.MULTI_POINT;
        break;
      case LINE_STRING:
        jsonSchema = JsonSchemaBuildingBlocks.LINE_STRING;
        break;
      case MULTI_LINE_STRING:
        jsonSchema = JsonSchemaBuildingBlocks.MULTI_LINE_STRING;
        break;
      case POLYGON:
        jsonSchema = JsonSchemaBuildingBlocks.POLYGON;
        break;
      case MULTI_POLYGON:
        jsonSchema = JsonSchemaBuildingBlocks.MULTI_POLYGON;
        break;
      case GEOMETRY_COLLECTION:
        jsonSchema = JsonSchemaBuildingBlocks.GEOMETRY_COLLECTION;
        break;
      case NONE:
        jsonSchema = JsonSchemaBuildingBlocks.NULL;
        break;
      case ANY:
      default:
        jsonSchema = JsonSchemaBuildingBlocks.GEOMETRY;
        break;
    }
    return adjustGeometry(schema, jsonSchema);
  }

  protected JsonSchema adjustGeometry(FeatureSchema schema, JsonSchema jsonSchema) {
    return jsonSchema;
  }

  @Override
  protected JsonSchema withName(JsonSchema jsonSchema, String propertyName) {
    return modify(jsonSchema, builder -> builder.name(propertyName));
  }

  @Override
  protected JsonSchema withRequired(JsonSchema jsonSchema) {
    return modify(jsonSchema, builder -> builder.isRequired(true));
  }

  protected JsonSchema modify(JsonSchema jsonSchema, Consumer<JsonSchema.Builder> modifier) {
    JsonSchema.Builder builder = null;

    if (jsonSchema instanceof JsonSchemaObject) {
      builder = new ImmutableJsonSchemaObject.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaOneOf) {
      builder = new ImmutableJsonSchemaOneOf.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaArray) {
      builder = new ImmutableJsonSchemaArray.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaRef) {
      builder = new ImmutableJsonSchemaRef.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaNull) {
      builder = new ImmutableJsonSchemaNull.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaInteger) {
      builder = new ImmutableJsonSchemaInteger.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaBoolean) {
      builder = new ImmutableJsonSchemaBoolean.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaNumber) {
      builder = new ImmutableJsonSchemaNumber.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaString) {
      builder = new ImmutableJsonSchemaString.Builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaGeometry) {
      builder = new ImmutableJsonSchemaGeometry.Builder().from(jsonSchema);
    }

    if (Objects.nonNull(builder)) {
      modifier.accept(builder);
      return builder.build();
    }

    return jsonSchema;
  }

  protected JsonSchema withConstraints(
      JsonSchema schema,
      SchemaConstraints constraints,
      FeatureSchema property,
      List<Codelist> codelists) {
    if (schema instanceof JsonSchemaArray) {
      return new ImmutableJsonSchemaArray.Builder()
          .from(schema)
          .minItems(constraints.getMinOccurrence())
          .maxItems(constraints.getMaxOccurrence())
          .build();
    }

    JsonSchema result = schema;

    if (constraints.getRequired().isPresent() && constraints.getRequired().get()) {
      result = withRequired(result);
    }

    if (!constraints.getEnumValues().isEmpty()) {
      // if enum is specified in the configuration, it wins over codelist
      boolean string =
          property.isArray()
              ? property.getValueType().orElse(SchemaBase.Type.UNKNOWN) != SchemaBase.Type.INTEGER
              : property.getType() != SchemaBase.Type.INTEGER;
      result =
          string
              ? new ImmutableJsonSchemaString.Builder()
                  .from(result)
                  .enums(constraints.getEnumValues())
                  .build()
              : new ImmutableJsonSchemaInteger.Builder()
                  .from(result)
                  .enums(
                      constraints.getEnumValues().stream()
                          .map(val -> Integer.parseInt(val))
                          .collect(Collectors.toList()))
                  .build();
    } else if (constraints.getCodelist().isPresent()) {
      Optional<Codelist> codelist =
          codelists.stream()
              .filter(cl -> cl.getId().equals(constraints.getCodelist().get()))
              .findAny();
      if (codelist.isPresent() && !codelist.get().getData().getFallback().isPresent()) {
        boolean string =
            property.isArray()
                ? property.getValueType().orElse(SchemaBase.Type.UNKNOWN) != SchemaBase.Type.INTEGER
                : property.getType() != SchemaBase.Type.INTEGER;
        Collection<String> values =
            useCodelistKeys
                ? codelist.get().getData().getEntries().keySet()
                : codelist.get().getData().getEntries().values();
        result =
            string
                ? new ImmutableJsonSchemaString.Builder().from(result).enums(values).build()
                : new ImmutableJsonSchemaInteger.Builder()
                    .from(result)
                    .enums(
                        values.stream()
                            .flatMap(
                                val -> {
                                  try {
                                    return Stream.of(Integer.valueOf(val));
                                  } catch (Throwable e) {
                                    return Stream.empty();
                                  }
                                })
                            .sorted()
                            .collect(Collectors.toList()))
                    .build();
      }
    }
    if (constraints.getRegex().isPresent() && result instanceof ImmutableJsonSchemaString) {
      result =
          new ImmutableJsonSchemaString.Builder()
              .from(result)
              .pattern(constraints.getRegex().get())
              .build();
    }
    if (constraints.getMin().isPresent() || constraints.getMax().isPresent()) {
      if (result instanceof ImmutableJsonSchemaInteger) {
        ImmutableJsonSchemaInteger.Builder builder =
            new ImmutableJsonSchemaInteger.Builder().from(result);
        if (constraints.getMin().isPresent())
          builder.minimum(Math.round(constraints.getMin().get()));
        if (constraints.getMax().isPresent())
          builder.maximum(Math.round(constraints.getMax().get()));
        result = builder.build();
      } else if (result instanceof ImmutableJsonSchemaNumber) {
        ImmutableJsonSchemaNumber.Builder builder =
            new ImmutableJsonSchemaNumber.Builder().from(result);
        if (constraints.getMin().isPresent()) builder.minimum(constraints.getMin().get());
        if (constraints.getMax().isPresent()) builder.maximum(constraints.getMax().get());
        result = builder.build();
      }
    }

    return result;
  }

  @Override
  protected JsonSchema withRefWrapper(JsonSchema schema, String objectType) {
    return new ImmutableJsonSchemaRef.Builder()
        .name(schema.getName())
        .ref(String.format("#/%s/%s", version == VERSION.V7 ? "definitions" : "$defs", objectType))
        .def(new ImmutableJsonSchemaObject.Builder().from(schema).name(objectType).build())
        .build();
  }

  @Override
  protected JsonSchema withArrayWrapper(JsonSchema schema) {
    return new ImmutableJsonSchemaArray.Builder().name(schema.getName()).items(schema).build();
  }
}
