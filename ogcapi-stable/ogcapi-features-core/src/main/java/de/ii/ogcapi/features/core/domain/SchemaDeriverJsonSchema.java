/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import static de.ii.xtraplatform.features.domain.FeatureSchema.LOGGER;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument.VERSION;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaDeriver;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public abstract class SchemaDeriverJsonSchema extends SchemaDeriver<JsonSchema> {

  protected final VERSION version;
  private final Optional<String> schemaUri;
  private final String label;
  private final Optional<String> description;

  public SchemaDeriverJsonSchema(
      VERSION version,
      Optional<String> schemaUri,
      String label,
      Optional<String> description,
      List<Codelist> codelists) {
    super(codelists);
    this.version = version;
    this.schemaUri = schemaUri;
    this.label = label;
    this.description = description;
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
        : property instanceof JsonSchemaAbstractDocument
            ? ((JsonSchemaAbstractDocument) property).getProperties()
            : ImmutableMap.of();
  }

  @Override
  protected JsonSchema buildRootSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      Map<String, JsonSchema> definitions,
      List<String> requiredProperties) {

    JsonSchemaAbstractDocument.Builder builder =
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
      JsonSchemaAbstractDocument.Builder rootBuilder) {}

  @Override
  protected Stream<JsonSchema> extractDefinitions(Stream<JsonSchema> properties) {
    return properties
        .filter(property -> Objects.nonNull(property) && property.getName().isPresent())
        .map(
            property ->
                property instanceof JsonSchemaArray
                    ? ((JsonSchemaArray) property).getItems()
                    : property)
        .filter(
            property -> property instanceof JsonSchemaRefInternal && property.getName().isPresent())
        .filter(
            ref ->
                Objects.nonNull(((JsonSchemaRefInternal) ref).getDef())
                    && ((JsonSchemaRefInternal) ref).getDef().getName().isPresent())
        .map(ref -> ((JsonSchemaRefInternal) ref).getDef())
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
        ImmutableJsonSchemaObject.builder()
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
        return ImmutableJsonSchemaInteger.builder()
            .title(label)
            .description(description)
            .unit(unit)
            .build();
      case FLOAT:
        return ImmutableJsonSchemaNumber.builder()
            .title(label)
            .description(description)
            .unit(unit)
            .build();
      case BOOLEAN:
        return ImmutableJsonSchemaBoolean.builder().title(label).description(description).build();
      case DATETIME:
        return ImmutableJsonSchemaString.builder()
            // validators will ignore this information as it isn't a well-known format value
            .format("date-time")
            .title(label)
            .description(description)
            .build();
      case DATE:
        return ImmutableJsonSchemaString.builder()
            // validators will ignore this information as it isn't a well-known format value
            .format("date")
            .title(label)
            .description(description)
            .build();
      case STRING:
      default:
        return ImmutableJsonSchemaString.builder()
            .title(label)
            .description(description)
            .unit(unit)
            .build();
    }
  }

  @Override
  @SuppressWarnings("PMD.CyclomaticComplexity")
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

  @SuppressWarnings("PMD.CyclomaticComplexity")
  protected JsonSchema modify(JsonSchema jsonSchema, Consumer<JsonSchema.Builder> modifier) {
    JsonSchema.Builder builder = null;

    if (jsonSchema instanceof JsonSchemaObject) {
      builder = ImmutableJsonSchemaObject.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaOneOf) {
      builder = ImmutableJsonSchemaOneOf.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaArray) {
      builder = ImmutableJsonSchemaArray.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaRefV7) {
      builder = ImmutableJsonSchemaRefV7.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaRef) {
      builder = ImmutableJsonSchemaRef.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaRefExternal) {
      builder = ImmutableJsonSchemaRefExternal.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaNull) {
      builder = ImmutableJsonSchemaNull.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaInteger) {
      builder = ImmutableJsonSchemaInteger.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaBoolean) {
      builder = ImmutableJsonSchemaBoolean.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaNumber) {
      builder = ImmutableJsonSchemaNumber.builder().from(jsonSchema);
    } else if (jsonSchema instanceof JsonSchemaString) {
      builder = ImmutableJsonSchemaString.builder().from(jsonSchema);
    } else {
      LOGGER.error("!!!");
    }

    if (Objects.nonNull(builder)) {
      modifier.accept(builder);
      return builder.build();
    }

    return jsonSchema;
  }

  @Override
  protected JsonSchema withConstraints(
      JsonSchema schema,
      SchemaConstraints constraints,
      FeatureSchema property,
      List<Codelist> codelists) {
    if (schema instanceof JsonSchemaArray) {
      return ImmutableJsonSchemaArray.builder()
          .from(schema)
          .minItems(constraints.getMinOccurrence())
          .maxItems(constraints.getMaxOccurrence())
          .build();
    }
    JsonSchema result = schema;
    result = processRequired(constraints, result);
    result = processEnumeratedValues(constraints, property, codelists, result);
    result = processRegex(constraints, result);
    result = processMinMax(constraints, result);
    return result;
  }

  private JsonSchema processRequired(SchemaConstraints constraints, JsonSchema result) {
    if (constraints.getRequired().isPresent() && constraints.getRequired().get()) {
      return withRequired(result);
    }
    return result;
  }

  private JsonSchema processRegex(SchemaConstraints constraints, JsonSchema result) {
    if (constraints.getRegex().isPresent() && result instanceof ImmutableJsonSchemaString) {
      return ImmutableJsonSchemaString.builder()
          .from(result)
          .pattern(constraints.getRegex().get())
          .build();
    }
    return result;
  }

  private JsonSchema processMinMax(SchemaConstraints constraints, JsonSchema result) {
    if (constraints.getMin().isPresent() || constraints.getMax().isPresent()) {
      if (result instanceof ImmutableJsonSchemaInteger) {
        return processMinMaxInteger(constraints, (JsonSchemaInteger) result);
      } else if (result instanceof ImmutableJsonSchemaNumber) {
        return processMinMaxNumber(constraints, (JsonSchemaNumber) result);
      }
    }
    return result;
  }

  private ImmutableJsonSchemaNumber processMinMaxNumber(
      SchemaConstraints constraints, JsonSchemaNumber result) {
    ImmutableJsonSchemaNumber.Builder builder = ImmutableJsonSchemaNumber.builder().from(result);
    if (constraints.getMin().isPresent()) {
      builder.minimum(constraints.getMin().get());
    }
    if (constraints.getMax().isPresent()) {
      builder.maximum(constraints.getMax().get());
    }
    return builder.build();
  }

  private ImmutableJsonSchemaInteger processMinMaxInteger(
      SchemaConstraints constraints, JsonSchemaInteger result) {
    ImmutableJsonSchemaInteger.Builder builder = ImmutableJsonSchemaInteger.builder().from(result);
    if (constraints.getMin().isPresent()) {
      builder.minimum(Math.round(constraints.getMin().get()));
    }
    if (constraints.getMax().isPresent()) {
      builder.maximum(Math.round(constraints.getMax().get()));
    }
    return builder.build();
  }

  @SuppressWarnings("PMD.ConfusingTernary")
  private JsonSchema processEnumeratedValues(
      SchemaConstraints constraints,
      FeatureSchema property,
      List<Codelist> codelists,
      JsonSchema result) {
    // if enum is specified in the configuration, it wins over codelist
    if (!constraints.getEnumValues().isEmpty()) {
      return processEnum(constraints.getEnumValues(), property, result);
    } else if (constraints.getCodelist().isPresent()) {
      return processCodelist(constraints.getCodelist().get(), property, codelists, result);
    }
    return result;
  }

  private JsonSchema processEnum(
      List<String> enumValues, FeatureSchema property, JsonSchema result) {
    boolean string =
        property.isArray()
            ? property.getValueType().orElse(Type.UNKNOWN) != Type.INTEGER
            : property.getType() != Type.INTEGER;
    return string
        ? ImmutableJsonSchemaString.builder().from(result).enums(enumValues).build()
        : ImmutableJsonSchemaInteger.builder()
            .from(result)
            .enums(enumValues.stream().map(Integer::parseInt).collect(Collectors.toList()))
            .build();
  }

  private JsonSchema processCodelist(
      String codelistId, FeatureSchema property, List<Codelist> codelists, JsonSchema result) {
    Optional<Codelist> codelist =
        codelists.stream().filter(cl -> cl.getId().equals(codelistId)).findAny();
    if (codelist.isPresent() && codelist.get().getData().getFallback().isEmpty()) {
      boolean string =
          property.isArray()
              ? property.getValueType().orElse(Type.UNKNOWN) != Type.INTEGER
              : property.getType() != Type.INTEGER;
      Set<String> values = codelist.get().getData().getEntries().keySet();
      return string
          ? ImmutableJsonSchemaString.builder().from(result).enums(values).build()
          : ImmutableJsonSchemaInteger.builder()
              .from(result)
              .enums(values.stream().map(Integer::valueOf).sorted().collect(Collectors.toList()))
              .build();
    }
    return result;
  }

  @Override
  protected JsonSchema withRefWrapper(JsonSchema schema, String objectType) {
    if (version == VERSION.V7) {
      return ImmutableJsonSchemaRefV7.builder()
          .name(schema.getName())
          .objectType(objectType)
          .def(ImmutableJsonSchemaObject.builder().from(schema).name(objectType).build())
          .build();
    }

    return ImmutableJsonSchemaRef.builder()
        .name(schema.getName())
        .objectType(objectType)
        .def(ImmutableJsonSchemaObject.builder().from(schema).name(objectType).build())
        .build();
  }

  @Override
  protected JsonSchema withArrayWrapper(JsonSchema schema) {
    return ImmutableJsonSchemaArray.builder().name(schema.getName()).items(schema).build();
  }
}
