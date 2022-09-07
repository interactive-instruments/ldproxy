/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Funnel;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableQueryExpression.Builder.class)
public interface QueryExpression {

  enum FilterOperator {
    AND,
    OR
  }

  @SuppressWarnings("UnstableApiUsage")
  Funnel<QueryExpression> FUNNEL =
      (from, into) -> {
        // TODO
      };

  ObjectMapper MAPPER =
      new ObjectMapper()
          .registerModule(new Jdk8Module())
          .registerModule(new GuavaModule())
          .configure(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY, true)
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  static QueryExpression of(byte[] requestBody) throws IOException {
    return MAPPER.readValue(requestBody, QueryExpression.class);
  }

  static QueryExpression of(InputStream requestBody) throws IOException {
    return MAPPER.readValue(requestBody, QueryExpression.class);
  }

  static void toFile(QueryExpression query, Path path) throws IOException {
    MAPPER.writeValue(path.toFile(), query);
  }

  @JsonIgnore String SCHEMA_REF = "#/components/schemas/QueryExpression";

  Optional<String> getId();

  Optional<String> getTitle();

  Optional<String> getDescription();

  List<SingleQuery> getQueries();

  List<String> getCollections();

  Map<String, Object> getFilter();

  @Value.Default
  default FilterOperator getFilterOperator() {
    return FilterOperator.AND;
  }

  List<String> getSortby();

  List<String> getProperties();

  Optional<String> getCrs();

  Optional<Double> getMaxAllowableOffset();

  Optional<Integer> getLimit();

  Optional<Integer> getOffset();

  @Value.Check
  default void check() {
    Preconditions.checkState(
        (getQueries().isEmpty() && getCollections().size() == 1)
            || (!getQueries().isEmpty() && getCollections().isEmpty()),
        "Either one or more queries must be provided or a single collection. Query: %s. Collections: %s.",
        getQueries(),
        getCollections());
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Set<String> getParameterNames() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    builder.addAll(getParameterNamesFromFilter(getFilterAsNode(getFilter())));
    for (SingleQuery q : getQueries()) {
      builder.addAll(getParameterNamesFromFilter(getFilterAsNode(q.getFilter())));
    }
    return builder.build();
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Map<String, JsonNode> getParametersAsNodes() {
    Map<String, JsonNode> params = getParametersFromFilter(getFilterAsNode(getFilter()));
    for (SingleQuery q : getQueries()) {
      params = mergeMaps(params, getParametersFromFilter(getFilterAsNode(q.getFilter())));
    }
    return params;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default Map<String, Schema<?>> getParameters() {
    Builder<String, Schema<?>> paramBuilder = ImmutableMap.builder();
    getParametersAsNodes()
        .forEach(
            (key, value) -> {
              paramBuilder.put(key, deriveSchema(value));
            });
    return paramBuilder.build();
  }

  default QueryExpression resolveParameters(
      Map<String, String> requestParameters, SchemaValidator schemaValidator) {
    Map<String, JsonNode> params = getParametersAsNodes();
    if (!params.isEmpty()) {
      ImmutableMap.Builder<String, JsonNode> builder = ImmutableMap.builder();
      for (Map.Entry<String, JsonNode> entry : params.entrySet()) {

        // get the JSON Schema of the parameter as a string for validation
        String schemaAsString;
        try {
          schemaAsString = MAPPER.writeValueAsString(entry.getValue());
        } catch (JsonProcessingException e) {
          throw new IllegalStateException(
              String.format(
                  "Could not read the schema of parameter '%s' in a query.", entry.getKey()),
              e);
        }

        // get value
        StringBuilder valueAsString;
        JsonNode valueAsNode;
        if (!requestParameters.containsKey(entry.getKey())) {
          // no value provided, use default or throw an exception
          valueAsNode = entry.getValue().get("default");
          if (Objects.isNull(valueAsNode) || valueAsNode.isNull()) {
            throw new BadRequestException(
                String.format("No value provided for parameter '%s'.", entry.getKey()));
          }
          try {
            // convert to string for validation
            valueAsString = new StringBuilder(MAPPER.writeValueAsString(valueAsNode));
          } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                String.format(
                    "The default value provided for parameter '%s' could not be converted to a value for the parameter.",
                    entry.getKey()),
                e);
          }
        } else {
          String value = requestParameters.get(entry.getKey());
          Schema<?> schema = deriveSchema(entry.getValue());
          if (schema instanceof ArraySchema) {
            Schema<?> itemsSchema = ((ArraySchema) schema).getItems();
            if (itemsSchema instanceof StringSchema) {
              valueAsString =
                  new StringBuilder(
                      String.format(
                          "[\"%s\"]",
                          Splitter.on(',')
                              .trimResults()
                              .splitToStream(value)
                              .map(s -> s.replace("\"", "\\\""))
                              .collect(Collectors.joining("\",\""))));
            } else {
              valueAsString =
                  new StringBuilder(
                      String.format(
                          "[%s]",
                          Splitter.on(',')
                              .trimResults()
                              .splitToStream(value)
                              .map(s -> s.replace("\"", "\\\""))
                              .collect(Collectors.joining(","))));
            }
          } else if (schema instanceof ObjectSchema) {
            Map<String, Schema> properties = schema.getProperties();
            valueAsString = new StringBuilder("{");
            String key = null;
            for (String s : Splitter.on(',').trimResults().split(value)) {
              if (Objects.isNull(key)) {
                key = s;
                valueAsString.append(String.format("\"%s\":", s));
              } else {
                if (properties.get(key) instanceof StringSchema) {
                  valueAsString.append(String.format("\"%s\"", s));
                } else {
                  valueAsString.append(s);
                }
                key = null;
              }
            }
            valueAsString.append("}");

          } else {
            if (schema instanceof StringSchema) {
              valueAsString =
                  new StringBuilder(String.format("\"%s\"", value.replace("\"", "\\\"")));
            } else {
              valueAsString = new StringBuilder(String.format("%s", value.replace("\"", "\\\"")));
            }
          }
          try {
            // convert to a JSON Node for processing
            valueAsNode = MAPPER.readTree(valueAsString.toString());
            // valueAsObject = MAPPER.treeToValue(valueAsNode, Object.class);
          } catch (JsonProcessingException e) {
            throw new BadRequestException(
                String.format(
                    "The value '%s' provided for parameter '%s' could not be converted to a value for the parameter.",
                    requestParameters.get(entry.getKey()), entry.getKey()),
                e);
          }
        }

        // validate parameter
        try {
          String finalValueAsString = valueAsString.toString();
          schemaValidator
              .validate(schemaAsString, valueAsString.toString())
              .ifPresent(
                  error -> {
                    throw new BadRequestException(
                        String.format(
                            "Parameter '%s' is invalid, the value '%s' does not conform to the schema '%s'. Reason: %s",
                            entry.getKey(), finalValueAsString, schemaAsString, error));
                  });
        } catch (IOException e) {
          throw new IllegalStateException(
              String.format(
                  "Could not validate value '%s' of parameter '%s' against its schema '%s'",
                  valueAsString.toString(), entry.getKey(), schemaAsString),
              e);
        }

        builder.put(entry.getKey(), valueAsNode);
      }

      ObjectNode queryNode = MAPPER.valueToTree(this);
      replaceParameters(queryNode, builder.build());
      try {
        return MAPPER.treeToValue(queryNode, QueryExpression.class);
      } catch (JsonProcessingException e) {
        throw new IllegalStateException(e);
      }
    }
    return this;
  }

  private Schema<?> deriveSchema(@NotNull JsonNode schemaNode) {
    Schema<?> schema = null;
    if (schemaNode.isObject() && "object".equals(schemaNode.get("type").asText())) {
      schema = new ObjectSchema();
      ObjectNode properties = (ObjectNode) schemaNode.get("properties");
      if (Objects.nonNull(properties)) {
        JsonNode node = schemaNode.get("required");
        if (Objects.nonNull(node)) {
          Iterator<JsonNode> iter = node.elements();
          while (iter.hasNext()) {
            schema.addRequiredItem(iter.next().asText());
          }
        }
        Iterator<Entry<String, JsonNode>> iter = properties.fields();
        while (iter.hasNext()) {
          Map.Entry<String, JsonNode> entry = iter.next();
          schema.addProperties(entry.getKey(), deriveSchema(entry.getValue()));
        }
        node = schemaNode.get("minProperties");
        if (Objects.nonNull(node)) {
          schema.minProperties(node.asInt());
        }
        node = schemaNode.get("maxProperties");
        if (Objects.nonNull(node)) {
          schema.maxProperties(node.asInt());
        }
      }
      // TODO patternProperties, additionalProperties, allOf, oneOf
    } else if (schemaNode.isObject() && "array".equals(schemaNode.get("type").asText())) {
      schema = new ArraySchema();
      JsonNode node = schemaNode.get("items");
      if (Objects.nonNull(node)) {
        ((ArraySchema) schema).items(deriveSchema(node));
      }
      node = schemaNode.get("minItems");
      if (Objects.nonNull(node)) {
        schema.minItems(node.asInt());
      }
      node = schemaNode.get("maxItems");
      if (Objects.nonNull(node)) {
        schema.maxItems(node.asInt());
      }
      node = schemaNode.get("uniqueObject");
      if (Objects.nonNull(node)) {
        schema.uniqueItems(node.asBoolean());
      }
      // TODO prefixItems, additionalItems, items:false
    } else if (schemaNode.isObject() && "string".equals(schemaNode.get("type").asText())) {
      schema = new StringSchema();
      JsonNode node = schemaNode.get("format");
      if (Objects.nonNull(node)) {
        schema.format(node.asText());
      }
      node = schemaNode.get("minLength");
      if (Objects.nonNull(node)) {
        schema.minLength(node.asInt());
      }
      node = schemaNode.get("maxLength");
      if (Objects.nonNull(node)) {
        schema.maxLength(node.asInt());
      }
      node = schemaNode.get("pattern");
      if (Objects.nonNull(node)) {
        schema.pattern(node.asText());
      }
      JsonNode enums = schemaNode.get("enum");
      if (Objects.nonNull(enums)) {
        Iterator<JsonNode> iter = enums.elements();
        while (iter.hasNext()) {
          ((StringSchema) schema).addEnumItem(iter.next().asText());
        }
      }
    } else if (schemaNode.isObject() && "number".equals(schemaNode.get("type").asText())) {
      schema = new NumberSchema();
      JsonNode node = schemaNode.get("multipleOf");
      if (Objects.nonNull(node)) {
        schema.multipleOf(node.decimalValue());
      }
      node = schemaNode.get("minimum");
      if (Objects.nonNull(node)) {
        schema.minimum(node.decimalValue());
      }
      node = schemaNode.get("exclusiveMinimum");
      if (Objects.nonNull(node)) {
        schema.minimum(node.decimalValue());
        schema.exclusiveMinimum(true);
      }
      node = schemaNode.get("maximum");
      if (Objects.nonNull(node)) {
        schema.maximum(node.decimalValue());
      }
      node = schemaNode.get("exclusiveMaximum");
      if (Objects.nonNull(node)) {
        schema.maximum(node.decimalValue());
        schema.exclusiveMaximum(true);
      }
      JsonNode enums = schemaNode.get("enum");
      if (Objects.nonNull(enums)) {
        Iterator<JsonNode> iter = enums.elements();
        while (iter.hasNext()) {
          ((NumberSchema) schema).addEnumItem(iter.next().decimalValue());
        }
      }
    } else if (schemaNode.isObject() && "integer".equals(schemaNode.get("type").asText())) {
      schema = new IntegerSchema();
      JsonNode node = schemaNode.get("multipleOf");
      if (Objects.nonNull(node)) {
        schema.multipleOf(node.decimalValue());
      }
      node = schemaNode.get("minimum");
      if (Objects.nonNull(node)) {
        schema.minimum(node.decimalValue());
      }
      node = schemaNode.get("exclusiveMinimum");
      if (Objects.nonNull(node)) {
        schema.minimum(node.decimalValue());
        schema.exclusiveMinimum(true);
      }
      node = schemaNode.get("maximum");
      if (Objects.nonNull(node)) {
        schema.maximum(node.decimalValue());
      }
      node = schemaNode.get("exclusiveMaximum");
      if (Objects.nonNull(node)) {
        schema.maximum(node.decimalValue());
        schema.exclusiveMaximum(true);
      }
      JsonNode enums = schemaNode.get("enum");
      if (Objects.nonNull(enums)) {
        Iterator<JsonNode> iter = enums.elements();
        while (iter.hasNext()) {
          ((IntegerSchema) schema).addEnumItem(iter.next().decimalValue());
        }
      }
    } else if (schemaNode.isObject() && "boolean".equals(schemaNode.get("type").asText())) {
      schema = new BooleanSchema();
    }

    if (Objects.nonNull(schema)) {
      JsonNode node = schemaNode.get("title");
      if (Objects.nonNull(node)) {
        schema.title(node.asText());
      }
      node = schemaNode.get("description");
      if (Objects.nonNull(node)) {
        schema.description(node.asText());
      }
      node = schemaNode.get("example");
      if (Objects.nonNull(node)) {
        try {
          schema.example(MAPPER.treeToValue(node, Object.class));
        } catch (JsonProcessingException e) {
          throw new IllegalStateException(e);
        }
      }
      node = schemaNode.get("default");
      if (Objects.nonNull(node)) {
        try {
          schema.setDefault(MAPPER.treeToValue(node, Object.class));
        } catch (JsonProcessingException e) {
          throw new IllegalStateException(e);
        }
      }
    }

    return schema;
  }

  private boolean isParameterNode(JsonNode node) {
    return Objects.nonNull(node) && node.isObject() && Objects.nonNull(node.get("$parameter"));
  }

  private String getParameterName(JsonNode node) {
    return node.get("$parameter").fields().next().getKey();
  }

  // move to QueryExpression
  private void replaceParameters(JsonNode node, Map<String, JsonNode> params) {
    if (Objects.isNull(node)) {
      return;
    } else if (node.isArray()) {
      ArrayNode arrayNode = (ArrayNode) node;
      for (int i = 0; i < arrayNode.size(); i++) {
        if (isParameterNode(arrayNode.get(i))) {
          arrayNode.set(i, params.get(getParameterName(arrayNode.get(i))));
        } else {
          replaceParameters(arrayNode.get(i), params);
        }
      }
    } else if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      Iterator<Entry<String, JsonNode>> iter = objectNode.fields();
      while (iter.hasNext()) {
        Map.Entry<String, JsonNode> entry = iter.next();
        if (isParameterNode(entry.getValue())) {
          objectNode.set(entry.getKey(), params.get(getParameterName(entry.getValue())));
        } else {
          replaceParameters(entry.getValue(), params);
        }
      }
    } else if (!node.isNull() && !node.isValueNode()) {
      throw new IllegalStateException(
          String.format("Support for schema type %s not implemented.", node.getNodeType()));
    }
  }

  private Map<String, JsonNode> getParametersFromFilter(JsonNode node) {
    if (Objects.isNull(node)) {
      return ImmutableMap.of();
    }

    Map<String, JsonNode> params = ImmutableMap.of();
    if (node.isArray()) {
      ArrayNode arrayNode = (ArrayNode) node;
      for (int i = 0; i < arrayNode.size(); i++) {
        params = mergeMaps(params, getParametersFromFilter(arrayNode.get(i)));
      }
    } else if (node.isObject()) {
      JsonNode param = node.get("$parameter");
      if (Objects.isNull(param)) {
        ObjectNode objectNode = (ObjectNode) node;
        Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
        while (iter.hasNext()) {
          Map.Entry<String, JsonNode> entry = iter.next();
          params = mergeMaps(params, getParametersFromFilter(entry.getValue()));
        }
      } else if (param.isObject()) {
        Map.Entry<String, JsonNode> entry = param.fields().next();
        if (!params.containsKey(entry.getKey())) {
          // ignore multiple definitions of the same parameter
          params = mergeMaps(params, ImmutableMap.of(entry.getKey(), entry.getValue()));
        }
      }
    }
    return params;
  }

  private Map<String, JsonNode> mergeMaps(
      Map<String, JsonNode> current, Map<String, JsonNode> additional) {
    return Stream.of(current, additional)
        .flatMap(map -> map.entrySet().stream())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (n1, n2) -> n1));
  }

  private Set<String> getParameterNamesFromFilter(JsonNode node) {
    if (Objects.isNull(node)) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
    if (node.isArray()) {
      ArrayNode arrayNode = (ArrayNode) node;
      for (int i = 0; i < arrayNode.size(); i++) {
        builder.addAll(getParameterNamesFromFilter(arrayNode.get(i)));
      }
    } else if (node.isObject()) {
      JsonNode param = node.get("$parameter");
      if (Objects.isNull(param)) {
        ObjectNode objectNode = (ObjectNode) node;
        Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
        while (iter.hasNext()) {
          Map.Entry<String, JsonNode> entry = iter.next();
          builder.addAll(getParameterNamesFromFilter(entry.getValue()));
        }
      } else if (param.isObject()) {
        Map.Entry<String, JsonNode> entry = param.fields().next();
        builder.add(entry.getKey());
      }
    }
    return builder.build();
  }

  private JsonNode getFilterAsNode(Map<String, Object> filter) {
    return MAPPER.valueToTree(filter);
  }
}
