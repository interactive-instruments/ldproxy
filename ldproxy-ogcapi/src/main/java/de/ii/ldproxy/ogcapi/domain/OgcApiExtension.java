/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import java.util.Optional;

/**
 * @author zahnen
 */
public interface OgcApiExtension {

    boolean isEnabledForDataset(OgcApiDatasetData dataset);

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
            return Optional.of(extensionConfiguration.get().mergeDefaults(defaultExtensionConfiguration.get()));
        }

        return Optional.empty();
    }

    default boolean isExtensionEnabled(ExtendableConfiguration defaultExtendableConfiguration, ExtendableConfiguration extendableConfiguration, Class<? extends ExtensionConfiguration> clazz) {

        return getExtensionConfiguration(defaultExtendableConfiguration, extendableConfiguration, clazz).filter(ExtensionConfiguration::getEnabled).isPresent();
    }

    default boolean isExtensionEnabled(ExtendableConfiguration extendableConfiguration, Class<? extends ExtensionConfiguration> clazz) {

        return getExtensionConfiguration(extendableConfiguration, clazz).filter(ExtensionConfiguration::getEnabled).isPresent();
    }
}
