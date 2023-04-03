/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class JsonSchemaDocumentV7 extends JsonSchemaDocument {

  @Override
  @Value.Derived
  public String getSchema() {
    return VERSION.V7.url();
  }

  @Override
  @JsonProperty("definitions")
  public abstract Map<String, JsonSchema> getDefinitions();

  abstract static class Builder extends JsonSchemaDocument.Builder {}

  @Value.Check
  public JsonSchemaDocumentV7 adjustRefs() {
    if (hasRefsWithWrongVersion(this)) {
      Map<String, JsonSchema> adjustedProperties = adjustRefs(getProperties());
      Map<String, JsonSchema> adjustedDefinitions = adjustRefs(getDefinitions());

      return ImmutableJsonSchemaDocumentV7.builder()
          .from(this)
          .properties(adjustedProperties)
          .definitions(adjustedDefinitions)
          .build();
    }

    return this;
  }

  static Map<String, JsonSchema> adjustRefs(Map<String, JsonSchema> schemas) {
    return schemas.entrySet().stream()
        .map(
            property ->
                new SimpleImmutableEntry<>(
                    property.getKey(),
                    isRefWithWrongVersion(property.getValue())
                        ? adjustRef((JsonSchemaRef) property.getValue())
                        : property.getValue() instanceof JsonSchemaObject
                            ? adjustRefs((JsonSchemaObject) property.getValue())
                            : property.getValue() instanceof JsonSchemaArray
                                ? adjustRefs((JsonSchemaArray) property.getValue())
                                : property.getValue()))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  static JsonSchemaObject adjustRefs(JsonSchemaObject schema) {
    if (hasRefsWithWrongVersion(schema)) {
      Map<String, JsonSchema> adjustedProperties = adjustRefs(schema.getProperties());

      return new ImmutableJsonSchemaObject.Builder()
          .from(schema)
          .properties(adjustedProperties)
          .build();
    }

    return schema;
  }

  static JsonSchemaArray adjustRefs(JsonSchemaArray schema) {
    if (hasRefsWithWrongVersion(schema)) {
      return new ImmutableJsonSchemaArray.Builder()
          .from(schema)
          .items(adjustRef((JsonSchemaRef) schema.getItems()))
          .build();
    }

    return schema;
  }

  static JsonSchemaRef adjustRef(JsonSchemaRef schema) {
    if (isRefWithWrongVersion(schema)) {
      return new ImmutableJsonSchemaRef.Builder()
          .from(schema)
          .ref(schema.getRef().replace("#/$defs/", "#/definitions/"))
          .build();
    }

    return schema;
  }

  static boolean hasRefsWithWrongVersion(Map<String, JsonSchema> schemas) {
    return schemas.values().stream()
        .anyMatch(
            property ->
                isRefWithWrongVersion(property)
                    || (property instanceof JsonSchemaObject
                        && hasRefsWithWrongVersion((JsonSchemaObject) property))
                    || (property instanceof JsonSchemaArray
                        && isRefWithWrongVersion(((JsonSchemaArray) property).getItems())));
  }

  static boolean hasRefsWithWrongVersion(JsonSchemaObject schema) {
    return hasRefsWithWrongVersion(schema.getProperties());
  }

  static boolean hasRefsWithWrongVersion(JsonSchemaArray schema) {
    return isRefWithWrongVersion(schema.getItems());
  }

  static boolean hasRefsWithWrongVersion(JsonSchemaDocument schema) {
    return hasRefsWithWrongVersion(schema.getProperties())
        || hasRefsWithWrongVersion(schema.getDefinitions());
  }

  static boolean isRefWithWrongVersion(JsonSchema schema) {
    return schema instanceof JsonSchemaRef
        && ((JsonSchemaRef) schema).getRef().startsWith("#/$defs/");
  }
}
