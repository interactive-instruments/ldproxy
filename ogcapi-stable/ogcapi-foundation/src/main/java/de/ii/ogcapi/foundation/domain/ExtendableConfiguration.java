/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

public interface ExtendableConfiguration {

  @JsonAlias("capabilities")
  List<ExtensionConfiguration> getExtensions();

  default <T extends ExtensionConfiguration> Optional<T> getExtension(Class<T> clazz) {
    //noinspection unchecked
    return getMergedExtensions().stream()
        .filter(
            extensionConfiguration ->
                Objects.equals(
                    extensionConfiguration.getBuildingBlock(),
                    ExtensionConfiguration.getBuildingBlockIdentifier(clazz)))
        .findFirst()
        .map(extensionConfiguration -> (T) extensionConfiguration);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default List<ExtensionConfiguration> getMergedExtensions() {
    return getMergedExtensions(getExtensions());
  }

  default List<ExtensionConfiguration> getMergedExtensions(
      List<ExtensionConfiguration> extensions) {
    Map<String, ExtensionConfiguration> mergedExtensions = new LinkedHashMap<>();

    extensions.forEach(
        extensionConfiguration -> {
          String buildingBlock = extensionConfiguration.getBuildingBlock();
          if (mergedExtensions.containsKey(buildingBlock)) {
            mergedExtensions.put(
                buildingBlock,
                extensionConfiguration.mergeInto(mergedExtensions.get(buildingBlock)));
          } else {
            mergedExtensions.put(buildingBlock, extensionConfiguration);
          }
        });

    return mergedExtensions.values().stream()
        .map(
            extensionConfiguration ->
                extensionConfiguration.getDefaultValues().isPresent()
                    ? extensionConfiguration.mergeInto(
                        extensionConfiguration.getDefaultValues().get())
                    : extensionConfiguration)
        .collect(Collectors.toList());
  }
}
