/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FeaturesCoreValidator {

    public static List<String> getCollectionsWithoutType(OgcApiDataV2 apiData, Map<String, FeatureSchema> featureSchemas) {
        return apiData.getCollections()
                      .entrySet()
                      .stream()
                      // find all collections without a feature provider
                      .filter(collection -> {
                          return Objects.isNull(featureSchemas.get(collection.getKey()));
                      })
                      .map(collection -> collection.getKey())
                      .collect(Collectors.toUnmodifiableList());
    }

    public static List<String> getInvalidPropertyKeys(Collection<String> keys, FeatureSchema schema) {
        if (Objects.isNull(keys))
            return ImmutableList.of();

        return keys.stream()
                   // normalize property names
                   //.map(key -> key.replaceAll("\\[[^\\]]*\\]", ""))
                   .filter(key -> SchemaInfo.getPropertyNames(schema, false)
                                            .stream()
                                            .noneMatch(schemaProperty -> schemaProperty.equals(key)))
                   .collect(Collectors.toUnmodifiableList());
    }

    public static Map<String, Collection<String>> getInvalidPropertyKeys(Map<String, Collection<String>> keyMap, Map<String, FeatureSchema> featureSchemas) {
        return keyMap.entrySet()
                   .stream()
                   .map(entry -> {
                          // * identify unknown property keys in the configuration

                          final String collectionId = entry.getKey();
                          final FeatureSchema schema = featureSchemas.get(collectionId);
                          if (Objects.isNull(schema))
                              return null;

                          List<String> invalidKeys = getInvalidPropertyKeys(keyMap.get(collectionId), schema);
                          if (invalidKeys.isEmpty())
                              return null;

                          return new AbstractMap.SimpleImmutableEntry<>(collectionId, invalidKeys);
                      })
                      .filter(Objects::nonNull)
                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
