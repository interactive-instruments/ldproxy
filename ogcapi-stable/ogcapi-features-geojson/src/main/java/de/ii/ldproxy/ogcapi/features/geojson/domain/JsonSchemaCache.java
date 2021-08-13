/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.geojson.domain;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.geojson.domain.JsonSchemaDocument.VERSION;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface JsonSchemaCache {

  default JsonSchemaDocument getSchemaWithCache(
      OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri,
      FeaturesCoreProviders providers,
      ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<VERSION, JsonSchemaDocument>>> cache) {
    return getSchemaWithCache(apiData, collectionId, schemaUri, VERSION.current(), providers, cache);
  }

  default JsonSchemaDocument getSchemaWithCache(
      OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, VERSION version,
      FeaturesCoreProviders providers,
      ConcurrentMap<Integer, ConcurrentMap<String, ConcurrentMap<VERSION, JsonSchemaDocument>>> cache) {
    int apiHashCode = apiData.hashCode();
    if (!cache.containsKey(apiHashCode)) {
      cache.put(apiHashCode, new ConcurrentHashMap<>());
    }
    if (!cache.get(apiHashCode).containsKey(collectionId)) {
      cache.get(apiHashCode).put(collectionId, new ConcurrentHashMap<>());
    }
    if (!cache.get(apiHashCode).get(collectionId).containsKey(version)) {
      cache.get(apiHashCode)
          .get(collectionId)
          .put(version, getSchema(apiData, collectionId, schemaUri, version, providers));
    }

    return cache.get(apiHashCode).get(collectionId).get(version);
  }

  default JsonSchemaDocument getSchema(
      OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri,
      FeaturesCoreProviders providers) {
    return getSchema(apiData, collectionId, schemaUri, VERSION.current(), providers);
  }

  default JsonSchemaDocument getSchema(
      OgcApiDataV2 apiData, String collectionId, Optional<String> schemaUri, VERSION version,
      FeaturesCoreProviders providers) {
    FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections()
        .get(collectionId);
    String featureTypeId = apiData.getCollections()
        .get(collectionId)
        .getExtension(FeaturesCoreConfiguration.class)
        .map(cfg -> cfg.getFeatureType().orElse(collectionId))
        .orElse(collectionId);
    FeatureProvider2 featureProvider = providers.getFeatureProvider(apiData, collectionData);
    FeatureSchema featureType = featureProvider.getData()
        .getTypes()
        .get(featureTypeId);

    return deriveSchema(featureType, collectionData, schemaUri, version);
  }

  JsonSchemaDocument deriveSchema(FeatureSchema schema,
      FeatureTypeConfigurationOgcApi collectionData, Optional<String> schemaUri, VERSION version);

}
