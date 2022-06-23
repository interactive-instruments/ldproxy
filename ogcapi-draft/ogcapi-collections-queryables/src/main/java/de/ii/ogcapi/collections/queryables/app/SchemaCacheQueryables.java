/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.features.core.domain.SchemaDeriverCollectionProperties;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation.Builder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class SchemaCacheQueryables extends JsonSchemaCache {

  private static final String DEFAULT_FLATTENING_SEPARATOR = ".";

  @Override
  protected JsonSchemaDocument deriveSchema(
      FeatureSchema schema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version) {
    List<String> queryables =
        collectionData
            .getExtension(FeaturesCoreConfiguration.class)
            .flatMap(FeaturesCoreConfiguration::getQueryables)
            .map(FeaturesCollectionQueryables::getAll)
            .orElse(ImmutableList.of());

    WithTransformationsApplied schemaFlattener =
        new WithTransformationsApplied(
            ImmutableMap.of(
                PropertyTransformations.WILDCARD,
                new Builder().flatten(DEFAULT_FLATTENING_SEPARATOR).build()));

    String flatteningSeparator =
        schemaFlattener.getFlatteningSeparator(schema).orElse(DEFAULT_FLATTENING_SEPARATOR);

    List<String> queryablesWithSeparator =
        Objects.equals(flatteningSeparator, DEFAULT_FLATTENING_SEPARATOR)
            ? queryables
            : queryables.stream()
                .map(
                    queryable ->
                        queryable.replaceAll(
                            Pattern.quote(DEFAULT_FLATTENING_SEPARATOR), flatteningSeparator))
                .collect(Collectors.toList());

    SchemaDeriverCollectionProperties schemaDeriverCollectionProperties =
        new SchemaDeriverCollectionProperties(
            version,
            schemaUri,
            collectionData.getLabel(),
            Optional.empty(),
            ImmutableList.of(),
            queryablesWithSeparator);

    return (JsonSchemaDocument)
        schema.accept(schemaFlattener).accept(schemaDeriverCollectionProperties);
  }
}
