/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ExtendableConfiguration {

    //@JsonMerge(value = OptBoolean.FALSE)
    @JsonAlias(value = "capabilities")
    List<ExtensionConfiguration> getExtensions();

    default <T extends ExtensionConfiguration> Optional<T> getExtension(Class<T> clazz) {
        return getExtensions().stream()
                              .filter(extensionConfiguration -> Objects.equals(extensionConfiguration.getBuildingBlock(), ExtensionConfiguration.getBuildingBlockIdentifier(clazz)))
                              .filter(extensionConfiguration -> extensionConfiguration!=null)
                              .findFirst()
                              .map(extensionConfiguration -> (T) extensionConfiguration);
    }

    default boolean hasRedundantExtensions() {
        return getExtensions().stream()
                              .map(ExtensionConfiguration::getBuildingBlock)
                              .distinct()
                              .count() < getExtensions().size();
    }

    default List<ExtensionConfiguration> getMergedExtensions() {
        Map<String, ExtensionConfiguration> mergedExtensions = new LinkedHashMap<>();

        getExtensions().forEach(extensionConfiguration -> {
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
