/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.OptBoolean;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface ExtendableConfiguration {

    @JsonMerge(value = OptBoolean.FALSE)
    List<ExtensionConfiguration> getCapabilities();

    default <T extends ExtensionConfiguration> Optional<T> getExtension(Class<T> clazz) {
        return getCapabilities().stream()
                                .filter(extensionConfiguration -> Objects.equals(extensionConfiguration.getExtensionType(), ExtensionConfiguration.getExtensionType(clazz)))
                                .findFirst()
                                .map(extensionConfiguration -> (T) extensionConfiguration);
    }
}
