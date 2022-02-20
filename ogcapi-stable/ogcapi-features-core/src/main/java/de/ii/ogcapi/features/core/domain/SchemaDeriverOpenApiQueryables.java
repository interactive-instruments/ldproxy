/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchemaDeriverOpenApiQueryables extends SchemaDeriverOpenApi{

  private final List<String> queryables;

  public SchemaDeriverOpenApiQueryables(String label, Optional<String> description,
      List<Codelist> codelists, List<String> queryables) {
    super(label, description, codelists);
    this.queryables = queryables;
  }

  @Override
  protected Schema<?> buildRootSchema(FeatureSchema schema, Map<String, Schema<?>> properties,
      Map<String, Schema<?>> definitions, List<String> requiredProperties) {
    Schema<?> rootSchema = new ObjectSchema()
        .properties(new LinkedHashMap<>())
        .title(label)
        .description(this.description.orElse(schema.getDescription().orElse("")));

    properties.forEach((propertyName, propertySchema) -> {
      String cleanName = propertyName.replaceAll("\\[\\]","");
      if (queryables.contains(cleanName)) {
        rootSchema.addProperties(cleanName, propertySchema);
      }
    });

    return rootSchema;
  }
}
