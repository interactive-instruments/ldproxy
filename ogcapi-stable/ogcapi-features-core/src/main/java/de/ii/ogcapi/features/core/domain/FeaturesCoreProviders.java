/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface FeaturesCoreProviders {

  String API_URI = "apiUri";
  String SERVICE_URL = "serviceUrl";
  Function<String, Map<String, String>> DEFAULT_SUBSTITUTIONS =
      apiUri -> ImmutableMap.of(API_URI, apiUri, SERVICE_URL, apiUri);
  BiFunction<String, Map<String, String>, Map<String, String>> DEFAULT_SUBSTITUTIONS_PLUS =
      (apiUri, plus) ->
          ImmutableMap.<String, String>builder()
              .putAll(DEFAULT_SUBSTITUTIONS.apply(apiUri))
              .putAll(plus)
              .build();

  boolean hasFeatureProvider(OgcApiDataV2 apiData);

  Optional<FeatureProvider2> getFeatureProvider(OgcApiDataV2 apiData);

  FeatureProvider2 getFeatureProviderOrThrow(OgcApiDataV2 apiData);

  boolean hasFeatureProvider(OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType);

  Optional<FeatureProvider2> getFeatureProvider(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType);

  FeatureProvider2 getFeatureProviderOrThrow(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType);

  default Optional<FeatureSchema> getFeatureSchema(
      OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType) {
    String featureTypeId =
        featureType
            .getExtension(FeaturesCoreConfiguration.class)
            .flatMap(FeaturesCoreConfiguration::getFeatureType)
            .orElse(featureType.getId());
    Optional<FeatureProvider2> featureProvider = getFeatureProvider(apiData, featureType);
    return featureProvider.map(provider -> provider.getData().getTypes().get(featureTypeId));
  }

  default Map<String, FeatureSchema> getFeatureSchemas(OgcApiDataV2 apiData) {
    return apiData.getCollections().entrySet().stream()
        .map(
            entry -> {
              FeatureTypeConfigurationOgcApi featureType = entry.getValue();
              String featureTypeId =
                  featureType
                      .getExtension(FeaturesCoreConfiguration.class)
                      .map(cfg -> cfg.getFeatureType().orElse(featureType.getId()))
                      .orElse(featureType.getId());
              Optional<FeatureProvider2> featureProvider = getFeatureProvider(apiData, featureType);
              Optional<FeatureSchema> schema =
                  featureProvider.map(provider -> provider.getData().getTypes().get(featureTypeId));
              if (schema.isEmpty()) return null;

              return new AbstractMap.SimpleImmutableEntry<>(featureType.getId(), schema.get());
            })
        .filter(Objects::nonNull)
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
