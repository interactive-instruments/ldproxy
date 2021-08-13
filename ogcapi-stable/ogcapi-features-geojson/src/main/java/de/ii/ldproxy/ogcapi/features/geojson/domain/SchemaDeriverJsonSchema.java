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
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaDocument.VERSION;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaArray;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaBoolean;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaDocument;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaDocumentV7;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaInteger;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaNull;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaNumber;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaObject;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaObject.Builder;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaOneOf;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaRef;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaRefV7;
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableJsonSchemaString;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SchemaDeriverJsonSchema implements SchemaVisitorTopDown<FeatureSchema, JsonSchema> {

  protected final VERSION version;
  private final Optional<String> schemaUri;
  private final String label;
  private final Optional<String> description;
  private final List<Codelist> codelists;

  public SchemaDeriverJsonSchema(
      VERSION version, Optional<String> schemaUri, String label,
      Optional<String> description,
      List<Codelist> codelists) {
    this.version = version;
    this.schemaUri = schemaUri;
    this.label = label;
    this.description = description;
    this.codelists = codelists;
  }

  @Override
  public JsonSchema visit(FeatureSchema schema, List<FeatureSchema> parents,
      List<JsonSchema> visitedProperties) {
    if (parents.isEmpty()) {
      return processFeatureSchema(schema, visitedProperties);
    }
    if (schema.isValue()) {
      return processValueSchema(schema);
    }

    return processObjectSchema(schema, visitedProperties);
  }

  protected void visitFeatureSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      Map<String, JsonSchema> defs,
      List<String> required, JsonSchemaDocument.Builder builder) {

  }
  protected void visitObjectSchema(FeatureSchema schema, Map<String, JsonSchema> properties,
      List<String> required, Builder builder) {
    builder.properties(properties);
    builder.required(required);
  }

  protected JsonSchema processFeatureSchema(FeatureSchema schema, List<JsonSchema> visitedProperties) {

    JsonSchemaDocument.Builder builder = version == VERSION.V7 ? ImmutableJsonSchemaDocumentV7.builder() : ImmutableJsonSchemaDocument
        .builder();
    builder
        .id(schemaUri)
        .title(label)
        .description(description.orElse(schema.getDescription().orElse("")));

    Map<String, JsonSchema> defs = extractDefinitions(visitedProperties);

    Map<String, JsonSchema> properties = visitedProperties.stream()
        .filter(property -> Objects.nonNull(property) && property.getName().isPresent())
        .map(property -> new SimpleEntry<>(getNameWithoutRole(property.getName().get()), property))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    List<String> required = visitedProperties.stream()
        .filter(property -> Objects.nonNull(property) && property.getName().isPresent() && property.isRequired())
        .map(property -> property.getName().get())
        .collect(Collectors.toList());
    Map<String, JsonSchema> patternProperties = new LinkedHashMap<>();

    visitFeatureSchema(schema, properties, defs, required, builder);

    return builder.build();
  }

  Map<String, JsonSchema> extractDefinitions(List<JsonSchema> properties) {
    return extractDefinitions(properties.stream())
        .collect(ImmutableMap.toImmutableMap(def -> def.getName().get(), def -> def, (first, second) -> second));
  }

  private Stream<JsonSchema> extractDefinitions(Stream<JsonSchema> properties) {
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

  protected JsonSchema processObjectSchema(FeatureSchema schema, List<JsonSchema> visitedProperties) {

    Map<String, JsonSchema> properties = visitedProperties.stream()
        .filter(property -> Objects.nonNull(property) && property.getName().isPresent())
        .map(property -> new SimpleEntry<>(getNameWithoutRole(property.getName().get()), property))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    List<String> required = visitedProperties.stream()
        .filter(property -> Objects.nonNull(property) && property.getName().isPresent() && property.isRequired())
        .map(property -> property.getName().get())
        .collect(Collectors.toList());
    Map<String, JsonSchema> patternProperties = new LinkedHashMap<>();


    Builder builder = ImmutableJsonSchemaObject.builder()
        .name(schema.getName())
        .title(schema.getLabel())
        .description(schema.getDescription());

    visitObjectSchema(schema, properties, required, builder);

    JsonSchema objectSchema = builder.build();

    String objectType = schema.getObjectType().orElse(getObjectType(schema));

    objectSchema = asRef((JsonSchemaObject) objectSchema, objectType);

    if (schema.isArray()) {
      return withConstraints(withArrayWrapper(objectSchema), schema.getConstraints());
    }

    return objectSchema;
  }

  private JsonSchema asRef(JsonSchemaObject schema, String objectType) {
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

  private JsonSchema withArrayWrapper(JsonSchema schema) {
    return ImmutableJsonSchemaArray.builder()
        .name(schema.getName())
        .items(schema)
        .build();
  }

  private JsonSchema withConstraints(JsonSchema schema, Optional<SchemaConstraints> constraints) {
    if (constraints.isPresent()) {
      if (schema instanceof JsonSchemaArray) {
        return ImmutableJsonSchemaArray.builder()
            .from(schema)
            .minItems(constraints.get().getMinOccurrence())
            .maxItems(constraints.get().getMaxOccurrence())
            .build();
      }
    }

    return schema;
  }

  protected JsonSchema processValueSchema(FeatureSchema schema) {
    JsonSchema jsonSchema = null;
    SchemaBase.Type propType = schema.getType();
    String propertyPath = String.join(".", schema.getFullPath());
    String propertyName = schema.getName();
    Optional<String> label = /*flatten ? Optional.of(nameTitleMap.get(propertyPath)) :*/ schema.getLabel();
    Optional<String> description = schema.getDescription() ;

    switch (propType) {
      case FLOAT:
      case INTEGER:
      case STRING:
      case BOOLEAN:
      case DATETIME:
      case DATE:
        jsonSchema = getJsonSchemaForLiteralType(propType, label, description);
        break;
      case VALUE_ARRAY:
        jsonSchema = getJsonSchemaForLiteralType(schema.getValueType().orElse(SchemaBase.Type.UNKNOWN), label, description);
        break;
      case GEOMETRY:
        jsonSchema = getJsonSchemaForGeometry(schema);
        break;
      case UNKNOWN:
      default:
        break;
    }

    if (propType == Type.GEOMETRY) {
      //TODO: SchemaBase.isPrimaryGeometry
      jsonSchema = withName(jsonSchema,  schema.getRole().filter(role -> role == Role.PRIMARY_GEOMETRY).isPresent() ? getNameWithRole(Role.PRIMARY_GEOMETRY, propertyName) : propertyName);
    } else {
      jsonSchema = withName(jsonSchema,  schema.isId() ? getNameWithRole(Role.ID, propertyName) : propertyName);
    }

    jsonSchema = processConstraintsJsonSchema(schema, codelists, jsonSchema, true);

    if (schema.isArray()) {
      jsonSchema = withConstraints(withArrayWrapper(jsonSchema), schema.getConstraints());
    }

    return jsonSchema;
  }

  private JsonSchema getJsonSchemaForGeometry(FeatureSchema schema) {
    JsonSchema jsonSchema;
    switch (schema.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
      case POINT:
        jsonSchema = GeoJsonSchema.POINT;
        break;
      case MULTI_POINT:
        jsonSchema = GeoJsonSchema.MULTI_POINT;
        break;
      case LINE_STRING:
        jsonSchema = GeoJsonSchema.LINE_STRING;
        break;
      case MULTI_LINE_STRING:
        jsonSchema = GeoJsonSchema.MULTI_LINE_STRING;
        break;
      case POLYGON:
        jsonSchema = GeoJsonSchema.POLYGON;
        break;
      case MULTI_POLYGON:
        jsonSchema = GeoJsonSchema.MULTI_POLYGON;
        break;
      case GEOMETRY_COLLECTION:
        jsonSchema = GeoJsonSchema.GEOMETRY_COLLECTION;
        break;
      case NONE:
        jsonSchema = GeoJsonSchema.NULL;
        break;
      case ANY:
      default:
        jsonSchema = GeoJsonSchema.GEOMETRY;
        break;
    }
    return visitGeometry(schema, jsonSchema);
  }

  protected JsonSchema visitGeometry(FeatureSchema schema, JsonSchema jsonSchema) {
    return jsonSchema;
  }

  //TODO: abstract builder, factor out case detection
  private JsonSchema withRequired(JsonSchema jsonSchema) {
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

  private JsonSchema withName(JsonSchema jsonSchema, String propertyName) {
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
    }
    return jsonSchema;
  }

  private JsonSchema getJsonSchemaForLiteralType(Type type, Optional<String> label,
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

  private JsonSchema processConstraintsJsonSchema(FeatureSchema property,
      List<Codelist> codelists,
      JsonSchema jsonSchema,
      boolean setRequired) {
    JsonSchema result = jsonSchema;
    if (property.getConstraints().isPresent()) {
      SchemaConstraints constraints = property.getConstraints().get();

      if (setRequired && constraints.getRequired().isPresent() && constraints.getRequired().get()) {
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
            builder.minimum(Math.round(constraints.getMax().get()));
          result = builder.build();
        } else if (result instanceof ImmutableJsonSchemaNumber) {
          ImmutableJsonSchemaNumber.Builder builder = ImmutableJsonSchemaNumber.builder()
              .from(result);
          if (constraints.getMin().isPresent())
            builder.minimum(constraints.getMin().get());
          if (constraints.getMax().isPresent())
            builder.minimum(constraints.getMax().get());
          result = builder.build();
        }
      }
    }

    return result;
  }

  protected final String getNameWithRole(Role role, String propertyName) {
    return String.format("_%s_ROLE_%s", role.name(), propertyName);
  }

  protected String getNameWithoutRole(String name) {
    int index = name.indexOf("_ROLE_");
    if (index > -1) {
      return name.substring(index + 6);
    }

    return name;
  }

  protected boolean nameHasRole(String name, Role role) {
    return name.startsWith(getNameWithRole(role, ""));
  }

  public static String getObjectType(FeatureSchema schema) {
    return "type_"+Integer.toHexString(schema.hashCode());
  }
}
