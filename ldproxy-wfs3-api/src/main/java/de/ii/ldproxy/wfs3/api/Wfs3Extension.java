/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import java.util.Optional;

/**
 * @author zahnen
 */
public interface Wfs3Extension {

    default Optional<ExtensionConfiguration> getExtensionConfiguration(ExtendableConfiguration extendableConfiguration, String extensionKey) {

        return Optional.ofNullable(extendableConfiguration.getExtensions()
                .get(extensionKey));
    }

    default Optional<ExtensionConfiguration> getExtensionConfiguration(ExtendableConfiguration defaultExtendableConfiguration, ExtendableConfiguration extendableConfiguration, String extensionKey) {

        Optional<ExtensionConfiguration> defaultExtensionConfiguration = Optional.ofNullable(defaultExtendableConfiguration.getExtensions()
                .get(extensionKey));

        Optional<ExtensionConfiguration> extensionConfiguration = Optional.ofNullable(extendableConfiguration.getExtensions()
                .get(extensionKey));
        if (defaultExtensionConfiguration.isPresent() && !extensionConfiguration.isPresent()) {
            return defaultExtensionConfiguration;
        } else if (!defaultExtensionConfiguration.isPresent() && extensionConfiguration.isPresent()) {
            return extensionConfiguration;
        } else if (defaultExtensionConfiguration.isPresent() && extensionConfiguration.isPresent()) {
            return Optional.of(extensionConfiguration.get().mergeDefaults(defaultExtensionConfiguration.get()));
        }

        return Optional.empty();
    }

    default boolean isExtensionEnabled(ExtendableConfiguration defaultExtendableConfiguration, ExtendableConfiguration extendableConfiguration, String extensionKey) {

        return getExtensionConfiguration(defaultExtendableConfiguration, extendableConfiguration, extensionKey).filter(ExtensionConfiguration::getEnabled).isPresent();
    }

    default boolean isExtensionEnabled(ExtendableConfiguration extendableConfiguration, String extensionKey) {

        return getExtensionConfiguration(extendableConfiguration, extensionKey).filter(ExtensionConfiguration::getEnabled).isPresent();
    }

}
