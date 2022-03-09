/**
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SchemaDeriverJsonSchema extends SchemaDeriver<JsonSchema> {

  protected final VERSION version;
  private final Optional<String> schemaUri;
  private final String label;
  private final Optional<String> description;

  public SchemaDeriverJsonSchema(
      VERSION version, Optional<String> schemaUri, String label,
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
        : ImmutableMap.of();
  }

  @Override
  protected JsonSchema buildRootSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      Map<String, JsonSchema> definitions, List<String> requiredProperties) {

    JsonSchemaDocument.Builder builder = version == VERSION.V7
        ? ImmutableJsonSchemaDocumentV7.builder()
        : ImmutableJsonSchemaDocument.builder();

    builder
        .id(schemaUri)
        .title(label)
        .description(description.orElse(schema.getDescription().orElse("")));

    adjustRootSchema(schema, properties, definitions, requiredProperties, builder);

    return builder.build();
  }

  protected void adjustRootSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      Map<String, JsonSchema> definitions,
      List<String> requiredProperties, JsonSchemaDocument.Builder rootBuilder) {

  }

  @Override
  protected Stream<JsonSchema> extractDefinitions(Stream<JsonSchema> properties) {
    return properties
        .filter(property -> Objects.nonNull(property) && property.getName().isPresent())
        .map(property -> property instanceof JsonSchemaArray ? ((JsonSchemaArray) property).getItems() : property)
        .filter(property -> property instanceof JsonSchemaRef && property.getName().isPresent())
        .filter(ref -> Objects.nonNull(((JsonSchemaRef) ref).getDef()) && ((JsonSchemaRef) ref).getDef().getName().isPresent())
        .map(ref -> ((JsonSchemaRef) ref).getDef())
        .flatMap(def -> def instanceof JsonSchemaObject
            ? Stream.concat(Stream.of(def), extractDefinitions(((JsonSchemaObject) def).getProperties().values()
            .stream()))
            : Stream.of(def));
  }

  @Override
  protected JsonSchema buildObjectSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      List<String> requiredProperties) {
    ImmutableJsonSchemaObject.Builder builder = ImmutableJsonSchemaObject.builder()
        .name(schema.getName())
        .title(schema.getLabel())
        .description(schema.getDescription());

    adjustObjectSchema(schema, properties, requiredProperties, builder);

    return builder.build();
  }

  protected void adjustObjectSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      List<String> requiredProperties, ImmutableJsonSchemaObject.Builder objectBuilder) {
    objectBuilder.properties(properties);
    objectBuilder.required(requiredProperties);
  }

  @Override
  protected JsonSchema getSchemaForLiteralType(Type type, Optional<String> label,
      Optional<String> description) {
    switch (type) {
      case INTEGER:
        return ImmutableJsonSchemaInteger.builder()
            .title(label)
            .description(description)
            .build();
      case FLOAT:
        return ImmutableJsonSchemaNumber.builder()
            .title(label)
            .description(description)
            .build();
      case BOOLEAN:
        return ImmutableJsonSchemaBoolean.builder()
            .title(label)
            .description(description)
            .build();
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
    if (jsonSchema instanceof JsonSchemaObject) {
      return ImmutableJsonSchemaObject.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    } else if (jsonSchema instanceof JsonSchemaOneOf) {
      return ImmutableJsonSchemaOneOf.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    } else if (jsonSchema instanceof JsonSchemaNull) {
      return ImmutableJsonSchemaNull.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    } else if (jsonSchema instanceof JsonSchemaInteger) {
      return ImmutableJsonSchemaInteger.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    } else if (jsonSchema instanceof JsonSchemaBoolean) {
      return ImmutableJsonSchemaBoolean.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    } else if (jsonSchema instanceof JsonSchemaNumber) {
      return ImmutableJsonSchemaNumber.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    } else if (jsonSchema instanceof JsonSchemaString) {
      return ImmutableJsonSchemaString.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    } else if (jsonSchema instanceof JsonSchemaRefExternal) {
      return ImmutableJsonSchemaRefExternal.builder()
          .from(jsonSchema)
          .name(propertyName)
          .build();
    }
    return jsonSchema;
  }

  //TODO: abstract builder, factor out case detection
  @Override
  protected JsonSchema withRequired(JsonSchema jsonSchema) {
    if (jsonSchema instanceof JsonSchemaObject) {
      return ImmutableJsonSchemaObject.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaOneOf) {
      return ImmutableJsonSchemaOneOf.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaArray) {
      return ImmutableJsonSchemaArray.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaRefV7) {
      return ImmutableJsonSchemaRefV7.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaRef) {
      return ImmutableJsonSchemaRef.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaNull) {
      return ImmutableJsonSchemaNull.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaInteger) {
      return ImmutableJsonSchemaInteger.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaBoolean) {
      return ImmutableJsonSchemaBoolean.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaNumber) {
      return ImmutableJsonSchemaNumber.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    } else if (jsonSchema instanceof JsonSchemaString) {
      return ImmutableJsonSchemaString.builder()
          .from(jsonSchema)
          .isRequired(true)
          .build();
    }
    return jsonSchema;
  }

  protected JsonSchema withConstraints(JsonSchema schema, SchemaConstraints constraints,
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

    if (constraints.getRequired().isPresent() && constraints.getRequired().get()) {
      result = withRequired(result);
    }

    if (!constraints.getEnumValues().isEmpty()) {
      // if enum is specified in the configuration, it wins over codelist
      boolean string = property.isArray() ?
          property.getValueType().orElse(SchemaBase.Type.UNKNOWN)!=SchemaBase.Type.INTEGER :
          property.getType()!=SchemaBase.Type.INTEGER;
      result = string ?
          ImmutableJsonSchemaString.builder()
              .from(result)
              .enums(constraints.getEnumValues())
              .build() :
          ImmutableJsonSchemaInteger.builder()
              .from(result)
              .enums(constraints.getEnumValues()
                  .stream()
                  .map(val -> Integer.parseInt(val))
                  .collect(Collectors.toList()))
              .build();
    } else if (constraints.getCodelist().isPresent()) {
      Optional<Codelist> codelist = codelists.stream()
          .filter(cl -> cl.getId().equals(constraints.getCodelist().get()))
          .findAny();
      if (codelist.isPresent() && !codelist.get().getData().getFallback().isPresent()) {
        boolean string = property.isArray() ?
            property.getValueType().orElse(SchemaBase.Type.UNKNOWN)!=SchemaBase.Type.INTEGER :
            property.getType()!=SchemaBase.Type.INTEGER;
        Set<String> values = codelist.get().getData().getEntries().keySet();
        result = string ?
            ImmutableJsonSchemaString.builder()
                .from(result)
                .enums(values)
                .build() :
            ImmutableJsonSchemaInteger.builder()
                .from(result)
                .enums(values.stream()
                    .map(val -> Integer.valueOf(val))
                    .sorted()
                    .collect(Collectors.toList()))
                .build();
      }
    }
    if (constraints.getRegex().isPresent() && result instanceof ImmutableJsonSchemaString) {
      result = ImmutableJsonSchemaString.builder()
          .from(result)
          .pattern(constraints.getRegex().get())
          .build();
    }
    if (constraints.getMin().isPresent() || constraints.getMax().isPresent()) {
      if (result instanceof ImmutableJsonSchemaInteger) {
        ImmutableJsonSchemaInteger.Builder builder = ImmutableJsonSchemaInteger.builder()
            .from(result);
        if (constraints.getMin().isPresent())
          builder.minimum(Math.round(constraints.getMin().get()));
        if (constraints.getMax().isPresent())
          builder.maximum(Math.round(constraints.getMax().get()));
        result = builder.build();
      } else if (result instanceof ImmutableJsonSchemaNumber) {
        ImmutableJsonSchemaNumber.Builder builder = ImmutableJsonSchemaNumber.builder()
            .from(result);
        if (constraints.getMin().isPresent())
          builder.minimum(constraints.getMin().get());
        if (constraints.getMax().isPresent())
          builder.maximum(constraints.getMax().get());
        result = builder.build();
      }
    }

    return result;
  }

  @Override
  protected JsonSchema withRefWrapper(JsonSchema schema, String objectType) {
    if (version == VERSION.V7) {
      return ImmutableJsonSchemaRefV7.builder()
          .name(schema.getName())
          .objectType(objectType)
          .def(ImmutableJsonSchemaObject.builder()
              .from(schema)
              .name(objectType)
              .build())
          .build();
    }

    return ImmutableJsonSchemaRef.builder()
        .name(schema.getName())
        .objectType(objectType)
        .def(ImmutableJsonSchemaObject.builder()
          .from(schema)
          .name(objectType)
          .build())
        .build();
  }

  @Override
  protected JsonSchema withArrayWrapper(JsonSchema schema) {
    return ImmutableJsonSchemaArray.builder()
        .name(schema.getName())
        .items(schema)
        .build();
  }

}
