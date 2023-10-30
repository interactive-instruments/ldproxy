/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.base.domain.LogContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoMultiBind
public interface ApiHeader extends ApiExtension {

  Logger LOGGER = LoggerFactory.getLogger(ApiHeader.class);

  Schema<String> SCHEMA = new StringSchema();
  String UNUSED = "unused";

  String getId();

  String getDescription();

  default boolean isRequestHeader() {
    return false;
  }

  default boolean isResponseHeader() {
    return false;
  }

  default Schema<?> getSchema(OgcApiDataV2 apiData) {
    return SCHEMA;
  }

  boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method);

  SchemaValidator getSchemaValidator();

  default Optional<String> validateSchema(OgcApiDataV2 apiData, String value) {
    try {
      String schemaContent = Json.mapper().writeValueAsString(getSchema(apiData));
      Optional<String> result = getSchemaValidator().validate(schemaContent, "\"" + value + "\"");
      return result.map(
          s -> String.format("Value '%s' is invalid for header '%s': %s", value, getId(), s));
    } catch (Exception e) {
      if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
        LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
      }
      return Optional.of(
          String.format(
              "An exception occurred while validating the value '%s' for header '%s'",
              value, getId()));
    }
  }

  default void setOpenApiDescription(OgcApiDataV2 apiData, Header header) {
    if (apiData
        .getExtension(FoundationConfiguration.class)
        .map(FoundationConfiguration::includesSpecificationInformation)
        .orElse(false)) {
      header.setDescription(
          getSpecificationMaturity()
              .map(
                  maturity ->
                      String.format(
                          "%s\n\n_%s_",
                          getDescription(), String.format(maturity.toString(), "header")))
              .orElse(getDescription()));
      getSpecificationMaturity()
          .ifPresent(
              maturity -> {
                header.setExtensions(ImmutableMap.of("x-maturity", maturity.name()));
                if (Objects.equals(maturity, SpecificationMaturity.DEPRECATED)) {
                  header.setDeprecated(true);
                }
              });
    } else {
      header.setDescription(getDescription());
    }
  }

  default void setOpenApiDescription(OgcApiDataV2 apiData, Parameter param) {
    if (apiData
        .getExtension(FoundationConfiguration.class)
        .map(FoundationConfiguration::includesSpecificationInformation)
        .orElse(false)) {
      param.setDescription(
          getSpecificationMaturity()
              .map(
                  maturity ->
                      String.format(
                          "%s\n\n_%s_",
                          getDescription(), String.format(maturity.toString(), "header")))
              .orElse(getDescription()));
      getSpecificationMaturity()
          .ifPresent(
              maturity -> {
                param.setExtensions(ImmutableMap.of("x-maturity", maturity.name()));
                if (Objects.equals(maturity, SpecificationMaturity.DEPRECATED)) {
                  param.setDeprecated(true);
                }
              });
    } else {
      param.setDescription(getDescription());
    }
  }
}
