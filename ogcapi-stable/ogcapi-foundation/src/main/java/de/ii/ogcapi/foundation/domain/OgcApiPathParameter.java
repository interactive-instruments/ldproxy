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
import io.swagger.v3.oas.models.parameters.PathParameter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@AutoMultiBind
public interface OgcApiPathParameter extends ParameterExtension {
  default boolean isExplodeInOpenApi(OgcApiDataV2 apiData) {
    return false;
  }

  List<String> getValues(OgcApiDataV2 apiData);

  String getPattern();

  boolean isApplicable(OgcApiDataV2 apiData, String definitionPath);

  default boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
    return isApplicable(apiData, definitionPath);
  }

  default boolean isApplicable(
      OgcApiDataV2 apiData, String definitionPath, Optional<String> collectionId) {
    return collectionId
        .map(s -> isApplicable(apiData, definitionPath, s))
        .orElseGet(() -> isApplicable(apiData, definitionPath));
  }

  @Override
  default boolean getRequired(OgcApiDataV2 apiData) {
    return true;
  }

  default void updateOpenApiDefinition(
      OgcApiDataV2 apiData, Optional<String> collectionId, OpenAPI openAPI, Operation op) {
    String id = getId(collectionId);
    op.addParametersItem(new Parameter().$ref("#/components/parameters/" + id));
    if (Objects.isNull(openAPI.getComponents().getParameters().get(id))) {
      openAPI.getComponents().addParameters(id, newPathParameter(apiData, collectionId));
    }
  }

  private Parameter newPathParameter(OgcApiDataV2 apiData, Optional<String> collectionId) {
    Parameter param =
        new PathParameter()
            .name(getName())
            .required(getRequired(apiData, collectionId))
            .schema(getSchema(apiData, collectionId));
    setOpenApiDescription(apiData, param);
    return param;
  }
}
