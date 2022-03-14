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
import de.ii.ogcapi.features.core.domain.JsonSchemaBuildingBlocks;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaArray;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaDocumentV7;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaInteger;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaRef;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaRefExternal;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaRefV7;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaString;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.features.core.domain.JsonSchemaRef;
import de.ii.ogcapi.features.core.domain.JsonSchemaRefExternal;
import de.ii.xtraplatform.features.domain.FeatureSchema;

import java.util.Optional;

public class SchemaCacheReturnablesCollection extends JsonSchemaCache {

  @Override
  protected JsonSchemaDocument deriveSchema(FeatureSchema schema, OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData, Optional<String> schemaUri,
      VERSION version) {

    JsonSchemaDocument.Builder builder = version == VERSION.V7
            ? ImmutableJsonSchemaDocumentV7.builder()
            : ImmutableJsonSchemaDocument.builder();

    JsonSchemaRef linkRef = version == JsonSchemaDocument.VERSION.V7
            ? ImmutableJsonSchemaRefV7.builder()
                                      .objectType("Link")
                                      .build()
            : ImmutableJsonSchemaRef.builder()
                                    .objectType("Link")
                                    .build();

    String featureSchemaUri = schemaUri.map(uri -> uri.replace("/schemas/collection", "/schemas/feature"))
                                       .orElse("https://geojson.org/schema/Feature.json");
    JsonSchemaRefExternal featureRef = ImmutableJsonSchemaRefExternal.builder()
                                                                     .ref(featureSchemaUri)
                                                                     .build();

    builder.id(schemaUri)
           .putDefinitions("Link", JsonSchemaBuildingBlocks.LINK_JSON)
           .putProperties("type", ImmutableJsonSchemaString.builder()
                                                           .addEnums("FeatureCollection")
                                                           .build())
           .putProperties("links", ImmutableJsonSchemaArray.builder()
                                                           .items(linkRef)
                                                           .build())
           .putProperties("timeStamp", ImmutableJsonSchemaString.builder()
                                                                .format("date-time")
                                                                .build())
           .putProperties("numberMatched", ImmutableJsonSchemaInteger.builder()
                                                                      .minimum(0)
                                                                      .build())
           .putProperties("numberReturned", ImmutableJsonSchemaInteger.builder()
                                                                       .minimum(0)
                                                                       .build())
           .putProperties("features", ImmutableJsonSchemaArray.builder()
                                                              .items(featureRef)
                                                              .build())
           .addRequired("type", "features");

    return builder.build();
  }
}
