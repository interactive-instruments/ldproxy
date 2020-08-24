/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

public interface ApiExtension {

    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(getBuildingBlockConfigurationType())
                      .map(ExtensionConfiguration::isEnabled)
                      .orElse(true);
    }

    default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(getBuildingBlockConfigurationType())
                      .map(ExtensionConfiguration::isEnabled)
                      .orElse(false);
    }

    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FoundationConfiguration.class;
    }

    default <T extends ExtensionConfiguration> boolean isExtensionEnabled(ExtendableConfiguration extendableConfiguration, Class<T> clazz) {
        return extendableConfiguration.getExtension(clazz).filter(ExtensionConfiguration::isEnabled).isPresent();
    }
}
