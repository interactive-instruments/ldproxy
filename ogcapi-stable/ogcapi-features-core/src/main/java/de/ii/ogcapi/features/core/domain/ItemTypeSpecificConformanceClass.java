/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;

public interface ItemTypeSpecificConformanceClass extends ConformanceClass {

  default boolean isItemTypeUsed(
      OgcApiDataV2 apiData, FeaturesCoreConfiguration.ItemType itemType) {
    return apiData.getCollections().values().stream()
        .anyMatch(
            collection ->
                collection.getEnabled()
                    && collection
                        .getExtension(FeaturesCoreConfiguration.class)
                        .flatMap(FeaturesCoreConfiguration::getItemType)
                        .orElse(FeaturesCoreConfiguration.ItemType.FEATURE)
                        .equals(itemType));
  }

  @Override
  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FeaturesCoreConfiguration.class;
  }
}
