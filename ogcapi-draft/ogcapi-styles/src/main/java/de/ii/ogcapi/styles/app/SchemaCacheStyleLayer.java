/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument.VERSION;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.geojson.domain.SchemaDeriverReturnables;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SchemaCacheStyleLayer extends JsonSchemaCache {

  private final Supplier<List<Codelist>> codelistSupplier;

  public SchemaCacheStyleLayer(Supplier<List<Codelist>> codelistSupplier) {
    this.codelistSupplier = codelistSupplier;
  }

  @Override
  protected JsonSchemaAbstractDocument deriveSchema(
      FeatureSchema schema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version) {

    WithTransformationsApplied schemaTransformer = new WithTransformationsApplied();

    SchemaDeriverReturnables schemaDeriverReturnables =
        new SchemaDeriverReturnables(
            version,
            schemaUri,
            collectionData.getLabel(),
            Optional.empty(),
            codelistSupplier.get());

    return (JsonSchemaAbstractDocument)
        schema.accept(schemaTransformer).accept(schemaDeriverReturnables);
  }
}
