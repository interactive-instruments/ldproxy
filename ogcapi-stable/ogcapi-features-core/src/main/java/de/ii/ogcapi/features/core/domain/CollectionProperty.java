/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(as = ImmutableCollectionProperty.class)
public abstract class CollectionProperty {

  public static CollectionProperty of(String name, ObjectNode schema) {
    ImmutableCollectionProperty.Builder builder = ImmutableCollectionProperty.builder().id(name);
    if (schema.has("title")) {
      builder.title(schema.get("title").textValue());
    }
    if (schema.has("description")) {
      builder.description(schema.get("description").textValue());
    }
    if (schema.has("default")) {
      JsonNode defaultValue = schema.get("default");
      if (defaultValue.isTextual()) {
        builder.defaultValue(defaultValue.textValue());
      } else if (defaultValue.isArray()) {
        Iterable<JsonNode> iterable = defaultValue::elements;
        builder.defaultValue(
            StreamSupport.stream(iterable.spliterator(), false)
                .map(element -> element.isTextual() ? element.textValue() : element.toString())
                .collect(Collectors.joining("; ")));

      } else {
        builder.defaultValue(defaultValue.toString());
      }
    }

    String type = schema.has("type") ? schema.get("type").textValue() : null;
    if ("array".equals(type)) {
      builder.isArray(true);
      schema = (ObjectNode) schema.get("items");
      type = schema.has("type") ? schema.get("type").textValue() : null;
    } else {
      builder.isArray(false);
    }

    if ("string".equals(type)) {
      if (schema.has("format")) {
        String format = schema.get("format").textValue();
        if ("date-time".equals(format)) {
          builder.type("Timestamp");
        } else if ("date".equals(format)) {
          builder.type("Date");
        } else if ("uri".equals(format)) {
          builder.type("URI");
        } else {
          builder.type("String");
        }
      } else {
        builder.type("String");
      }
      if (schema.has("pattern")) {
        builder.pattern(schema.get("pattern").textValue());
      }
      if (schema.has("enum")) {
        Iterator<JsonNode> enums = schema.get("enum").elements();
        while (enums.hasNext()) {
          builder.addValues(enums.next().textValue());
        }
      }
    } else if ("number".equals(type)) {
      builder.type("Number");
      if (schema.has("min")) {
        builder.min(String.valueOf(schema.get("min").numberValue()));
      }
      if (schema.has("max")) {
        builder.max(String.valueOf(schema.get("max").numberValue()));
      }
    } else if ("integer".equals(type)) {
      builder.type("Integer");
      if (schema.has("min")) {
        builder.min(String.valueOf(schema.get("min").numberValue()));
      }
      if (schema.has("max")) {
        builder.max(String.valueOf(schema.get("max").numberValue()));
      }
      if (schema.has("enum")) {
        Iterator<JsonNode> enums = schema.get("enum").elements();
        while (enums.hasNext()) {
          builder.addValues(String.valueOf(enums.next().intValue()));
        }
      }
    } else if ("boolean".equals(type)) {
      builder.type("Boolean");
    } else if ("object".equals(type)) {
      builder.type("Object");
    } else {
      builder.type("String");
    }
    return builder.build();
  }

  public abstract String getId();

  public abstract String getType();

  public abstract boolean isArray();

  public abstract Optional<String> getTitle();

  public abstract Optional<String> getDescription();

  public abstract Optional<Boolean> getRequired();

  public abstract Optional<String> getPattern();

  public abstract Optional<Object> getMin();

  public abstract Optional<Object> getMax();

  public abstract List<String> getValues();

  @Value.Derived
  public Optional<String> getValueList() {
    return !getValues().isEmpty() ? Optional.of(String.join("; ", getValues())) : Optional.empty();
  }

  public abstract Optional<String> getDefaultValue();
}
