/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.text.search.app;

import de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.text.search.domain.ImmutableTextSearchConfiguration;
import de.ii.ogcapi.text.search.domain.TextSearchConfiguration;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityMigration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public class QueryablesMigrationV4 extends EntityMigration<OgcApiDataV2, OgcApiDataV2> {

  public QueryablesMigrationV4(EntityMigrationContext context) {
    super(context);
  }

  @Override
  public String getSubject() {
    return "building block FEATURES_CORE, property 'queryables'";
  }

  @Override
  public String getDescription() {
    return "is deprecated and will be migrated to the QUERYABLES and TEXT_SEARCH building blocks";
  }

  @Override
  public boolean isApplicable(EntityData entityData) {
    if (!(entityData instanceof OgcApiDataV2)) {
      return false;
    }

    OgcApiDataV2 apiData = (OgcApiDataV2) entityData;

    for (FeatureTypeConfigurationOgcApi collection : apiData.getCollections().values()) {
      Optional<FeaturesCoreConfiguration> featuresCoreConfiguration =
          collection.getExtension(FeaturesCoreConfiguration.class);

      if (featuresCoreConfiguration.isPresent()
          && featuresCoreConfiguration.get().getQueryables().isPresent()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public OgcApiDataV2 migrate(OgcApiDataV2 entityData) {
    Map<String, FeatureTypeConfigurationOgcApi> collections =
        entityData.getCollections().entrySet().stream()
            .map(
                entry -> {
                  FeatureTypeConfigurationOgcApi collectionOld = entry.getValue();
                  Optional<FeaturesCoreConfiguration> featuresCoreConfigurationOld =
                      collectionOld.getExtension(FeaturesCoreConfiguration.class);
                  Optional<QueryablesConfiguration> queryablesConfigurationOld =
                      collectionOld.getExtension(QueryablesConfiguration.class);
                  Optional<TextSearchConfiguration> textSearchConfigurationOld =
                      collectionOld.getExtension(TextSearchConfiguration.class);

                  if (featuresCoreConfigurationOld.isEmpty()
                      || featuresCoreConfigurationOld.get().getQueryables().isEmpty()) {
                    return Map.entry(entry.getKey(), collectionOld);
                  }

                  FeaturesCoreConfiguration featuresCoreConfiguration =
                      new Builder()
                          .from(featuresCoreConfigurationOld.get())
                          .queryables(Optional.empty())
                          .build();

                  QueryablesConfiguration queryablesConfiguration =
                      (queryablesConfigurationOld.isPresent()
                              ? new ImmutableQueryablesConfiguration.Builder()
                                  .from(queryablesConfigurationOld.get())
                              : new ImmutableQueryablesConfiguration.Builder().enabled(true))
                          .included(
                              featuresCoreConfigurationOld.get().getQueryables().get().getAll())
                          .build();

                  TextSearchConfiguration textSearchConfiguration =
                      (textSearchConfigurationOld.isPresent()
                              ? new ImmutableTextSearchConfiguration.Builder()
                                  .from(textSearchConfigurationOld.get())
                              : new ImmutableTextSearchConfiguration.Builder().enabled(true))
                          .properties(
                              featuresCoreConfigurationOld.get().getQueryables().get().getQ())
                          .build();

                  FeatureTypeConfigurationOgcApi collection =
                      FeatureTypeConfigurationOgcApi.replaceOrAddExtensions(
                          collectionOld,
                          featuresCoreConfiguration,
                          queryablesConfiguration,
                          textSearchConfiguration);

                  return Map.entry(entry.getKey(), collection);
                })
            .collect(
                Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

    return new ImmutableOgcApiDataV2.Builder().from(entityData).collections(collections).build();
  }
}
