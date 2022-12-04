/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.base.domain.LogContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoMultiBind
public interface ParameterExtension extends ApiExtension {

  Logger LOGGER = LoggerFactory.getLogger(ParameterExtension.class);
  Schema<String> SCHEMA = new StringSchema();

  default String getId() {
    return getName();
  }

  default String getId(@NotNull String collectionId) {
    return getId();
  }

  default String getId(@NotNull Optional<String> collectionId) {
    return collectionId.isPresent() ? getId(collectionId.get()) : getId();
  }

  String getName();

  String getDescription();

  default boolean getRequired(@NotNull OgcApiDataV2 apiData) {
    return false;
  }

  default boolean getRequired(
      OgcApiDataV2 apiData, @SuppressWarnings("unused") String collectionId) {
    return getRequired(apiData);
  }

  default boolean getRequired(
      @NotNull OgcApiDataV2 apiData, @NotNull Optional<String> collectionId) {
    return collectionId.map(s -> getRequired(apiData, s)).orElseGet(() -> getRequired(apiData));
  }

  default Schema<?> getSchema(@NotNull OgcApiDataV2 apiData) {
    return SCHEMA;
  }

  default Schema<?> getSchema(@NotNull OgcApiDataV2 apiData, @NotNull String collectionId) {
    return getSchema(apiData);
  }

  default Schema<?> getSchema(
      @NotNull OgcApiDataV2 apiData, @NotNull Optional<String> collectionId) {
    return collectionId.isPresent() ? getSchema(apiData, collectionId.get()) : getSchema(apiData);
  }

  default boolean getExplode() {
    return false;
  }

  default Optional<String> validate(
      @NotNull OgcApiDataV2 apiData,
      @NotNull Optional<String> collectionId,
      @NotNull List<String> values) {
    // first validate against the schema
    Optional<String> result = validateSchema(apiData, collectionId, values);
    if (result.isPresent()) {
      return result;
    }
    // if the values are schema-valid, validate against any additional parameter-specific checks
    return validateOther(apiData, collectionId, values);
  }

  default Optional<String> validateOther(
      @NotNull OgcApiDataV2 apiData,
      @NotNull Optional<String> collectionId,
      @NotNull List<String> values) {
    return Optional.empty();
  }

  // we can only inject the schema validator in the leaf classes, but we need it here, so we need to
  // make it
  // available via an interface method, although only validateSchema() should use the validator
  SchemaValidator getSchemaValidator();

  default Optional<String> validateSchema(
      @NotNull OgcApiDataV2 apiData,
      @NotNull Optional<String> collectionId,
      @NotNull List<String> values) {
    try {
      Schema<?> schema = getSchema(apiData, collectionId);
      String schemaContent = Json.mapper().writeValueAsString(schema);
      Optional<String> result1 = Optional.empty();
      List<String> effectiveValues = values;
      if (values.size() == 1) {
        // try non-array variant first
        result1 =
            getSchemaValidator().validate(schemaContent, getJsonContent(values.get(0), schema));
        if (result1.isEmpty()) {
          return Optional.empty();
        }
        if (!getExplode() && values.get(0).contains(",")) {
          effectiveValues =
              Splitter.on(",").trimResults().omitEmptyStrings().splitToList(values.get(0));
        }
      }
      Optional<String> resultn =
          getSchemaValidator()
              .validate(schemaContent, "[\"" + String.join("\",\"", effectiveValues) + "\"]");
      if (resultn.isPresent()) {
        return result1
            .map(
                s ->
                    String.format(
                        "Parameter value '%s' is invalid for parameter '%s': %s",
                        values, getName(), s))
            .or(
                () ->
                    Optional.of(
                        String.format(
                            "Parameter value '%s' is invalid for parameter '%s': %s",
                            values, getName(), resultn.get())));
      }
    } catch (IOException e) {
      if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
        LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
      }
      return Optional.of(
          String.format(
              "An exception occurred while validating the parameter value '%s' for parameter '%s'",
              values, getName()));
    }

    return Optional.empty();
  }

  private String getJsonContent(@NotNull String value, @NotNull Schema<?> schema) {
    return ("object".equals(schema.getType()) && value.trim().startsWith("{"))
            || ("array".equals(schema.getType()) && value.trim().startsWith("["))
        ? value
        : "\"" + value + "\"";
  }

  default Map<String, String> transformParameters(
      @NotNull FeatureTypeConfigurationOgcApi featureType,
      @NotNull Map<String, String> parameters,
      @NotNull OgcApiDataV2 apiData) {
    return parameters;
  }
}
