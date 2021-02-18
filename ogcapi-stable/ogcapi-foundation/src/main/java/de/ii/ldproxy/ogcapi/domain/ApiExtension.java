/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import org.immutables.value.Value;

import static de.ii.xtraplatform.features.domain.TypeInfoValidator.ValidationResult;

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

    default StartupResult onStartup(OgcApiDataV2 apiData, FeatureProviderDataV2.VALIDATION apiValidation) {
        // optional start actions
        return StartupResult.of();
    }

    @Value.Immutable
    interface StartupResult extends ValidationResult {

        static StartupResult of() {
            return new ImmutableStartupResult.Builder()
                    .mode(FeatureProviderDataV2.VALIDATION.NONE)
                    .build();
        }

        default StartupResult mergeWith(StartupResult other) {
            return new ImmutableStartupResult.Builder()
                    .from(this)
                    .mode(other.getMode())
                    .addAllErrors(other.getErrors())
                    .addAllStrictErrors(other.getStrictErrors())
                    .addAllWarnings(other.getWarnings())
                    .build();
        }
    }
}
