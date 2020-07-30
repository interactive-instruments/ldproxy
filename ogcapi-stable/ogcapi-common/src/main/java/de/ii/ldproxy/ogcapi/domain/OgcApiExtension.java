/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.Optional;

public interface OgcApiExtension {

    boolean isEnabledForApi(OgcApiApiDataV2 apiData);

    default boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isEnabledForApi(apiData);
    }

    default boolean isEnabledForApi(OgcApiApiDataV2 apiData, Optional<String> collectionId) {
        return collectionId.isPresent() ? isEnabledForApi(apiData, collectionId.get()) : isEnabledForApi(apiData);
    }

    default <T extends ExtensionConfiguration> Optional<T> getExtensionConfiguration(
            ExtendableConfiguration extendableConfiguration, Class<T> clazz) {

        return extendableConfiguration.getExtension(clazz);
    }

    default <T extends ExtensionConfiguration> Optional<T> getExtensionConfiguration(ExtendableConfiguration defaultExtendableConfiguration, ExtendableConfiguration extendableConfiguration, Class<T> clazz) {

        Optional<T> defaultExtensionConfiguration = defaultExtendableConfiguration.getExtension(clazz);

        Optional<T> extensionConfiguration = extendableConfiguration.getExtension(clazz);

        if (defaultExtensionConfiguration.isPresent() && !extensionConfiguration.isPresent()) {
            return defaultExtensionConfiguration;
        } else if (!defaultExtensionConfiguration.isPresent() && extensionConfiguration.isPresent()) {
            return extensionConfiguration;
        } else if (defaultExtensionConfiguration.isPresent() && extensionConfiguration.isPresent()) {
            return Optional.of((T)extensionConfiguration.get().mergeInto(defaultExtensionConfiguration.get()));
        }

        return Optional.empty();
    }

    default <T extends ExtensionConfiguration> boolean isExtensionEnabled(ExtendableConfiguration defaultExtendableConfiguration, ExtendableConfiguration extendableConfiguration, Class<T> clazz) {

        return getExtensionConfiguration(defaultExtendableConfiguration, extendableConfiguration, clazz).filter(ExtensionConfiguration::getEnabled).isPresent();
    }

    default <T extends ExtensionConfiguration> boolean isExtensionEnabled(ExtendableConfiguration extendableConfiguration, Class<T> clazz) {

        return getExtensionConfiguration(extendableConfiguration, clazz).filter(ExtensionConfiguration::getEnabled).isPresent();
    }
}
