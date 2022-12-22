/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.features.domain.FeatureTypeConfiguration;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class PathParameterCollectionIdTiles extends AbstractPathParameterCollectionId {

  @Inject
  PathParameterCollectionIdTiles(SchemaValidator schemaValidator) {
    super(schemaValidator);
  }

  @Override
  public List<String> getValues(OgcApiDataV2 apiData) {
    if (!apiCollectionMap.containsKey(apiData.hashCode())) {
      apiCollectionMap.put(
          apiData.hashCode(),
          apiData.getCollections().values().stream()
              .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
              .filter(
                  collection ->
                      collection
                          .getExtension(TilesConfiguration.class)
                          .filter(ExtensionConfiguration::isEnabled)
                          .isPresent())
              .map(FeatureTypeConfiguration::getId)
              .collect(Collectors.toUnmodifiableList()));
    }

    return apiCollectionMap.get(apiData.hashCode());
  }

  @Override
  public boolean isExplodeInOpenApi(OgcApiDataV2 apiData) {
    return false;
  }

  @Override
  public String getId() {
    return "collectionIdTiles";
  }

  @Override
  public boolean matchesPath(String definitionPath) {
    return definitionPath.startsWith("/collections/{collectionId}/tiles");
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return TilesConfiguration.class;
  }
}
