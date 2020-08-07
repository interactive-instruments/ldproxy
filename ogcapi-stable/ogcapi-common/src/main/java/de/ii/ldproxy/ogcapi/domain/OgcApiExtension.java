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

    //TODO: use this to centralize isEnabledForApi implementations
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return null;
    }

    default <T extends ExtensionConfiguration> boolean isExtensionEnabled(ExtendableConfiguration extendableConfiguration, Class<T> clazz) {

        return extendableConfiguration.getExtension(clazz).filter(ExtensionConfiguration::isEnabled).isPresent();
    }
}
