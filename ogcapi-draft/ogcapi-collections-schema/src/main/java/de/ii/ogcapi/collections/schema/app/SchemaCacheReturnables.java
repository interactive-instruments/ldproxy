/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.app;

import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.JsonSchemaCache;
import de.ii.ogcapi.features.geojson.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.geojson.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.features.geojson.domain.SchemaDeriverReturnables;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SchemaCacheReturnables extends JsonSchemaCache {

  private static final String DEFAULT_FLATTENING_SEPARATOR = ".";

  private final Supplier<List<Codelist>> codelistSupplier;

  public SchemaCacheReturnables(
          Supplier<List<Codelist>> codelistSupplier) {
    this.codelistSupplier = codelistSupplier;
  }

  @Override
  protected JsonSchemaDocument deriveSchema(FeatureSchema schema, OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData, Optional<String> schemaUri,
      VERSION version) {

    Optional<PropertyTransformations> propertyTransformations = collectionData
        .getExtension(GeoJsonConfiguration.class)
        .map(geoJsonConfiguration -> (PropertyTransformations) geoJsonConfiguration);

    WithTransformationsApplied schemaTransformer = propertyTransformations
        .map(WithTransformationsApplied::new)
        .orElse(new WithTransformationsApplied());

    SchemaDeriverReturnables schemaDeriverReturnables = new SchemaDeriverReturnables(
        version, schemaUri, collectionData.getLabel(),
        Optional.empty(), codelistSupplier.get());

    return (JsonSchemaDocument) schema
        .accept(schemaTransformer)
        .accept(schemaDeriverReturnables);
  }
}
