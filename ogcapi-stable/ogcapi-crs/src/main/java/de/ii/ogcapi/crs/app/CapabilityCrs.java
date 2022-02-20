/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.crs.domain.ImmutableCrsConfiguration.Builder;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
@AutoBind
public class CapabilityCrs implements ApiBuildingBlock {

    private final CrsTransformerFactory crsTransformerFactory;
    private final FeaturesCoreProviders featuresCoreProviders;

    @Inject
    public CapabilityCrs(CrsTransformerFactory crsTransformerFactory, FeaturesCoreProviders featuresCoreProviders) {
        this.crsTransformerFactory = crsTransformerFactory;
        this.featuresCoreProviders = featuresCoreProviders;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(true)
                                                      .build();
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        Optional<CrsConfiguration> crsConfiguration = apiData.getExtension(CrsConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled);

        if (crsConfiguration.isEmpty()) {
            return ValidationResult.of();
        }

        EpsgCrs providerCrs = featuresCoreProviders.getFeatureProvider(apiData)
            .flatMap(featureProvider2 -> featureProvider2.getData().getNativeCrs())
            .orElse(OgcCrs.CRS84);

        EpsgCrs lastCrs = null;
        try {
            for (EpsgCrs crs : crsConfiguration.get().getAdditionalCrs()) {
                lastCrs = crs;
                crsTransformerFactory.getTransformer(providerCrs, crs);
            }
        } catch (Throwable e) {
            return ImmutableValidationResult.builder()
                .mode(apiValidation)
                .addErrors(String.format("Could not load CRS %s: %s", lastCrs.toSimpleString(), e.getMessage()))
                .build();
        }

        return ValidationResult.of();
    }

}
