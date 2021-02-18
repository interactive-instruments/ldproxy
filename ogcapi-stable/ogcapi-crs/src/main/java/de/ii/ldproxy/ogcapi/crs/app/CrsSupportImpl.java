/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Component
@Provides
@Instantiate
public class CrsSupportImpl implements CrsSupport {

    private final FeaturesCoreProviders providers;

    public CrsSupportImpl(@Requires FeaturesCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public List<EpsgCrs> getSupportedCrsList(OgcApiDataV2 apiData) {
        return getSupportedCrsList(apiData, null);
    }

    @Override
    public List<EpsgCrs> getSupportedCrsList(OgcApiDataV2 apiData,
                                             @Nullable FeatureTypeConfigurationOgcApi featureTypeConfiguration) {
        EpsgCrs nativeCrs = getStorageCrs(apiData, Optional.ofNullable(featureTypeConfiguration));
        EpsgCrs defaultCrs = getDefaultCrs(apiData, Optional.ofNullable(featureTypeConfiguration));
        Set<EpsgCrs> additionalCrs = getAdditionalCrs(apiData, Optional.ofNullable(featureTypeConfiguration));

        return Stream.concat(
                Stream.of(
                        defaultCrs,
                        nativeCrs
                ),
                additionalCrs.stream()
        )
                     .distinct()
                     .collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean isSupported(OgcApiDataV2 apiData, EpsgCrs crs) {
        return isSupported(apiData, null, crs);
    }

    @Override
    public boolean isSupported(OgcApiDataV2 apiData,
                               @Nullable FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                               EpsgCrs crs) {
        return getSupportedCrsList(apiData, featureTypeConfiguration).contains(crs);
    }

    @Override
    public EpsgCrs getStorageCrs(OgcApiDataV2 apiData,
                                 Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration) {
        FeatureProvider2 provider = featureTypeConfiguration.isPresent() ? providers.getFeatureProvider(apiData, featureTypeConfiguration.get()) : providers.getFeatureProvider(apiData);

        if (!provider.supportsCrs()) {
            throw new IllegalStateException("Provider has no CRS support.");
        }

        return provider.crs().getNativeCrs();
    }

    private EpsgCrs getDefaultCrs(OgcApiDataV2 apiData,
                                  Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration) {
        return apiData.getExtension(FeaturesCoreConfiguration.class)
                      .get()
                      .getDefaultEpsgCrs();
    }

    private Set<EpsgCrs> getAdditionalCrs(OgcApiDataV2 apiData,
                                          Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration) {
        return apiData.getExtension(CrsConfiguration.class)
                      .map(CrsConfiguration::getAdditionalCrs)
                      .orElse(ImmutableSet.of());
    }
}
