/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import java.util.*;
import java.util.stream.Collectors;

public interface ExtendableConfiguration {

    @JsonAlias(value = "capabilities")
    List<ExtensionConfiguration> getExtensions();

    default <T extends ExtensionConfiguration> Optional<T> getExtension(Class<T> clazz) {
        return getMergedExtensions().stream()
                              .filter(extensionConfiguration -> Objects.equals(extensionConfiguration.getBuildingBlock(), ExtensionConfiguration.getBuildingBlockIdentifier(clazz)))
                              .filter(Objects::nonNull)
                              .findFirst()
                              .map(extensionConfiguration -> (T) extensionConfiguration);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<ExtensionConfiguration> getMergedExtensions() {
        return getMergedExtensions(getExtensions());
    }

    default List<ExtensionConfiguration> getMergedExtensions(List<ExtensionConfiguration> extensions) {
        Map<String, ExtensionConfiguration> mergedExtensions = new LinkedHashMap<>();

        extensions.forEach(extensionConfiguration -> {
            String buildingBlock = extensionConfiguration.getBuildingBlock();
            if (mergedExtensions.containsKey(buildingBlock)) {
                mergedExtensions.put(buildingBlock, extensionConfiguration.mergeInto(mergedExtensions.get(buildingBlock)));
            } else {
                mergedExtensions.put(buildingBlock, extensionConfiguration);
            }
        });

        return mergedExtensions.values()
                               .stream()
                               .map(extensionConfiguration -> extensionConfiguration.getDefaultValues()
                                                                                    .isPresent()
                                       ? extensionConfiguration.mergeInto(extensionConfiguration.getDefaultValues()
                                                                                                .get())
                                       : extensionConfiguration)
                               .collect(Collectors.toList());
    }
}
