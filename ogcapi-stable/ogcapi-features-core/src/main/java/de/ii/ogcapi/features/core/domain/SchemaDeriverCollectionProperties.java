/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SchemaDeriverCollectionProperties extends SchemaDeriverJsonSchema {

  private final List<String> properties;
  private final boolean wildcard;

  public SchemaDeriverCollectionProperties(
      VERSION version,
      Optional<String> schemaUri,
      String label,
      Optional<String> description,
      List<Codelist> codelists,
      List<String> properties) {
    super(version, schemaUri, label, description, codelists, true);
    this.properties = properties;
    this.wildcard = properties.size() == 1 && "*".equals(properties.get(0));
  }

  @Override
  protected void adjustRootSchema(
      FeatureSchema schema,
      Map<String, JsonSchema> properties,
      Map<String, JsonSchema> defs,
      List<String> required,
      JsonSchemaDocument.Builder builder) {
    properties.forEach(
        (propertyName, propertySchema) -> {
          String cleanName = propertyName.replaceAll("\\[]", "");
          if (wildcard || this.properties.contains(cleanName)) {
            builder.putProperties(cleanName, propertySchema);
          }
        });
    builder.additionalProperties(ImmutableJsonSchemaFalse.builder().build());
  }
}
