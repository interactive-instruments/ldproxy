/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@AutoMultiBind
public interface OgcApiQueryParameter extends ParameterExtension {

  default String getStyle() {
    return "form";
  }

  boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method);

  default boolean isApplicable(
      OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
    return isApplicable(apiData, definitionPath, method);
  }

  default void updateOpenApiDefinition(
      OgcApiDataV2 apiData, Optional<String> collectionId, OpenAPI openAPI, Operation op) {
    String id = getId(collectionId);
    op.addParametersItem(new Parameter().$ref("#/components/parameters/" + id));
    if (Objects.isNull(openAPI.getComponents().getParameters().get(id))) {
      openAPI.getComponents().addParameters(id, newQueryParameter(apiData, collectionId));
    }
  }

  private Parameter newQueryParameter(OgcApiDataV2 apiData, Optional<String> collectionId) {
    return new io.swagger.v3.oas.models.parameters.QueryParameter()
        .name(getName())
        .description(getDescription())
        .required(getRequired(apiData, collectionId))
        .schema(getSchema(apiData, collectionId))
        .style(Parameter.StyleEnum.valueOf(getStyle().toUpperCase(Locale.ROOT)))
        .explode(getExplode());
  }
}
