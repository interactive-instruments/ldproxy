/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.Objects;
import java.util.function.Predicate;

@AutoMultiBind
public interface ApiExtension {

  default boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return isExtensionEnabled(apiData, getBuildingBlockConfigurationType());
  }

  default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return isExtensionEnabled(apiData.getCollections().get(collectionId),
        getBuildingBlockConfigurationType());
  }

  default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return FoundationConfiguration.class;
  }

  default <T extends ExtensionConfiguration> boolean isExtensionEnabled(
      ExtendableConfiguration extendableConfiguration, Class<T> clazz) {
    return Objects.nonNull(extendableConfiguration) && extendableConfiguration.getExtension(clazz)
        .filter(ExtensionConfiguration::isEnabled).isPresent();
  }

  default <T extends ExtensionConfiguration> boolean isExtensionEnabled(
      ExtendableConfiguration extendableConfiguration, Class<T> clazz, Predicate<T> predicate) {
    return Objects.nonNull(extendableConfiguration) && extendableConfiguration.getExtension(clazz)
        .filter(ExtensionConfiguration::isEnabled).filter(predicate).isPresent();
  }

  default ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
    // optional start actions
    return ValidationResult.of();
  }
}
