/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureProvider;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

class IdTransform implements PropertyTransformations {

  private final Map<String, List<PropertyTransformation>> transformations;

  IdTransform(FeatureProvider featureProvider, String featureTypeId, String collectionId) {
    FeatureSchema schema = featureProvider.info().getSchema(featureTypeId).get();
    String idProperty = Objects.requireNonNull(findIdProperty(schema));
    transformations =
        ImmutableMap.of(
            idProperty,
            ImmutableList.of(
                new ImmutablePropertyTransformation.Builder()
                    .stringFormat(String.format("%s.{{value}}", collectionId))
                    .build()));
  }

  private String findIdProperty(FeatureSchema schema) {
    return schema.getProperties().stream()
        .flatMap(
            property -> {
              Collection<FeatureSchema> nestedProperties = property.getAllNestedProperties();
              if (!nestedProperties.isEmpty()) {
                return nestedProperties.stream();
              }
              return Stream.of(property);
            })
        .filter(
            property ->
                property.getRole().isPresent() && property.getRole().get() == SchemaBase.Role.ID)
        .findFirst()
        .map(FeatureSchema::getFullPathAsString)
        .orElse(null);
  }

  @Override
  public Map<String, List<PropertyTransformation>> getTransformations() {
    return transformations;
  }
}
