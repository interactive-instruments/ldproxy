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
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApi;
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

/**
 * # Coordinate Reference Systems (CRS)
 * @langEn The module *Coordinate Reference Systems* may be enabled for every API with a feature provider.
 * It provides support for additional coordinate reference systems apart from the default
 * [CRS84](http://www.opengis.net/def/crs/OGC/1.3/CRS84) (WGS 84).
 *
 * All transformations between two coordinate reference systems are handled by *Geotools*.
 * If multiple transformations are available, *Geotools* decides which one to use. Transformations are
 * currently not configurable.
 *
 * *Coordinate Reference Systems* implements all requirements of conformance class *Coordinate Reference
 * System by Reference* of [OGC API - Features - Part 2: Coordinate Reference System by Reference
 * 1.0.0-draft.1](http://docs.opengeospatial.org/DRAFTS/18-058.html).
 * @langDe Das Modul "CRS" kann für jede über ldproxy bereitgestellte API mit einem Feature-Provider
 * aktiviert werden. Es ergänzt die Unterstützung für weitere Koordinatenreferenzsysteme neben dem
 * Standard-Koordinatenreferenzsystem [CRS84](http://www.opengis.net/def/crs/OGC/1.3/CRS84) (WGS 84).
 *
 * Alle Koordinatentransformationen zwischen zwei Koordinatenreferenzsystemen erfolgen mit Geotools.
 * Geotools entscheidet, welche Transformation verwendet wird, sofern mehrere verfügbar sind.
 * Eine Konfigurationsmöglichkeit in ldproxy besteht nicht.
 *
 * Das Modul implementiert alle Vorgaben der Konformitätsklasse "Coordinate Reference System by
 * Reference" von [OGC API - Features - Part 2: Coordinate Reference System by Reference 1.0]
 * (http://www.opengis.net/doc/IS/ogcapi-features-2/1.0).
 * @see CrsConfiguration
 */
@Singleton
@AutoBind
public class CrsBuildingBlock implements ApiBuildingBlock {

    private final CrsTransformerFactory crsTransformerFactory;
    private final FeaturesCoreProviders featuresCoreProviders;

    @Inject
    public CrsBuildingBlock(CrsTransformerFactory crsTransformerFactory, FeaturesCoreProviders featuresCoreProviders) {
        this.crsTransformerFactory = crsTransformerFactory;
        this.featuresCoreProviders = featuresCoreProviders;
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new Builder().enabled(true)
                                                      .build();
    }

    @Override
    public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
        Optional<CrsConfiguration> crsConfiguration = api.getData().getExtension(CrsConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled);

        if (crsConfiguration.isEmpty()) {
            return ValidationResult.of();
        }


        EpsgCrs defaultCrs = api.getData().getExtension(
            FeaturesCoreConfiguration.class).map(FeaturesCoreConfiguration::getDefaultEpsgCrs).orElse(OgcCrs.CRS84);
        EpsgCrs providerCrs = featuresCoreProviders.getFeatureProvider(api.getData())
            .flatMap(featureProvider2 -> featureProvider2.getData().getNativeCrs())
            .orElse(OgcCrs.CRS84);

        EpsgCrs lastCrs = null;
        try {
            lastCrs = defaultCrs;
            crsTransformerFactory.getTransformer(providerCrs, defaultCrs);

            for (EpsgCrs crs : crsConfiguration.get().getAdditionalCrs()) {
                lastCrs = crs;
                crsTransformerFactory.getTransformer(providerCrs, crs);
            }
        } catch (Throwable e) {
            return ImmutableValidationResult.builder()
                .mode(apiValidation)
                .addErrors(String.format("Could not find transformation for %s -> %s: %s", providerCrs.toHumanReadableString(), lastCrs.toHumanReadableString(), e.getMessage()))
                .build();
        }
        try {
            lastCrs = defaultCrs;
            crsTransformerFactory.getTransformer(defaultCrs, providerCrs);

            for (EpsgCrs crs : crsConfiguration.get().getAdditionalCrs()) {
                lastCrs = crs;
                crsTransformerFactory.getTransformer(crs, providerCrs);
            }
        } catch (Throwable e) {
            return ImmutableValidationResult.builder()
                .mode(apiValidation)
                .addErrors(String.format("Could not find transformation for %s -> %s: %s", lastCrs.toHumanReadableString(), providerCrs.toHumanReadableString(), e.getMessage()))
                .build();
        }

        return ValidationResult.of();
    }

}
