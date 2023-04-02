/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.app;

import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaArray;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaDocumentV7;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaInteger;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaRef;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaString;
import de.ii.ogcapi.features.core.domain.JsonSchemaBuildingBlocks;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.features.core.domain.JsonSchemaRef;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.Optional;

public class SchemaCacheReturnablesCollection extends JsonSchemaCache {

  @Override
  protected JsonSchemaDocument deriveSchema(
      FeatureSchema schema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version) {

    JsonSchemaDocument.Builder builder =
        version == VERSION.V7
            ? ImmutableJsonSchemaDocumentV7.builder()
            : ImmutableJsonSchemaDocument.builder();

    JsonSchemaRef linkRef =
        new ImmutableJsonSchemaRef.Builder()
            .ref(
                String.format(
                    "#/%s/Link",
                    version == JsonSchemaDocument.VERSION.V7 ? "definitions" : "$defs"))
            .build();

    String featureSchemaUri =
        schemaUri
            .map(uri -> uri.replace("/schemas/collection", "/schemas/feature"))
            .orElse("https://geojson.org/schema/Feature.json");
    JsonSchemaRef featureRef = new ImmutableJsonSchemaRef.Builder().ref(featureSchemaUri).build();

    builder
        .id(schemaUri)
        .putDefinitions("Link", JsonSchemaBuildingBlocks.LINK_JSON)
        .putProperties(
            "type", new ImmutableJsonSchemaString.Builder().addEnums("FeatureCollection").build())
        .putProperties("links", new ImmutableJsonSchemaArray.Builder().items(linkRef).build())
        .putProperties(
            "timeStamp", new ImmutableJsonSchemaString.Builder().format("date-time").build())
        .putProperties("numberMatched", new ImmutableJsonSchemaInteger.Builder().minimum(0).build())
        .putProperties(
            "numberReturned", new ImmutableJsonSchemaInteger.Builder().minimum(0).build())
        .putProperties("features", new ImmutableJsonSchemaArray.Builder().items(featureRef).build())
        .addRequired("type", "features");

    return builder.build();
  }
}
