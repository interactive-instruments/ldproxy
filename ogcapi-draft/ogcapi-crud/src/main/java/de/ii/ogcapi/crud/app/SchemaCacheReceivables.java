/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureSchemaBase;
import java.util.List;
import java.util.Optional;

public class SchemaCacheReceivables extends JsonSchemaCache {

  private final boolean removeId;
  private final boolean allOptional;
  private final boolean allNonRequiredNullable;
  private final boolean strict;

  public SchemaCacheReceivables(
      boolean removeId, boolean allOptional, boolean allNonRequiredNullable, boolean strict) {
    super(FeatureSchemaBase.Scope.MUTATIONS);
    this.removeId = removeId;
    this.allOptional = allOptional;
    this.allNonRequiredNullable = allNonRequiredNullable;
    this.strict = strict;
  }

  @Override
  protected JsonSchemaDocument deriveSchema(
      FeatureSchema schema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version) {

    SchemaDeriverReceivables schemaDeriverReceivables =
        new SchemaDeriverReceivables(
            version,
            schemaUri,
            collectionData.getLabel(),
            Optional.empty(),
            List.of(),
            removeId,
            allOptional,
            allNonRequiredNullable,
            strict);

    return (JsonSchemaDocument) schema.accept(schemaDeriverReceivables);
  }
}
