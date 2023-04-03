/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureSchemaBase;
import de.ii.xtraplatform.features.domain.FeatureSchemaBase.Scope;
import de.ii.xtraplatform.features.domain.transform.WithScope;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class JsonSchemaCache {

  private final ConcurrentMap<
          Integer, ConcurrentMap<String, ConcurrentMap<VERSION, JsonSchemaDocument>>>
      cache;
  private final WithScope withScope;

  protected JsonSchemaCache() {
    this(FeatureSchemaBase.Scope.QUERIES);
  }

  protected JsonSchemaCache(Scope scope) {
    this.cache = new ConcurrentHashMap<>();
    this.withScope = new WithScope(scope);
  }

  public final JsonSchemaDocument getSchema(
      FeatureSchema featureSchema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri) {
    return getSchema(featureSchema, apiData, collectionData, schemaUri, VERSION.current());
  }

  public final JsonSchemaDocument getSchema(
      FeatureSchema featureSchema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version) {
    int apiHashCode = apiData.hashCode();
    if (!cache.containsKey(apiHashCode)) {
      cache.put(apiHashCode, new ConcurrentHashMap<>());
    }
    if (!cache.get(apiHashCode).containsKey(collectionData.getId())) {
      cache.get(apiHashCode).put(collectionData.getId(), new ConcurrentHashMap<>());
    }
    if (!cache.get(apiHashCode).get(collectionData.getId()).containsKey(version)) {
      cache
          .get(apiHashCode)
          .get(collectionData.getId())
          .put(
              version,
              deriveSchema(
                  featureSchema.accept(withScope), apiData, collectionData, schemaUri, version));
    }

    return cache.get(apiHashCode).get(collectionData.getId()).get(version);
  }

  protected abstract JsonSchemaDocument deriveSchema(
      FeatureSchema schema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version);
}
