/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@Provides
@Instantiate
public class CrsSupportImpl implements CrsSupport {

    private final OgcApiFeatureCoreProviders providers;

    public CrsSupportImpl(@Requires OgcApiFeatureCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public List<EpsgCrs> getSupportedCrsList(OgcApiApiDataV2 apiData) {
        return getSupportedCrsList(apiData, null);
    }

    @Override
    public List<EpsgCrs> getSupportedCrsList(OgcApiApiDataV2 apiData,
                                             @Nullable FeatureTypeConfigurationOgcApi featureTypeConfiguration) {
        EpsgCrs nativeCrs = getStorageCrs(apiData, Optional.ofNullable(featureTypeConfiguration));
        EpsgCrs defaultCrs = getDefaultCrs(apiData, Optional.ofNullable(featureTypeConfiguration));
        List<EpsgCrs> additionalCrs = getAdditionalCrs(apiData, Optional.ofNullable(featureTypeConfiguration));

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
    public boolean isSupported(OgcApiApiDataV2 apiData, EpsgCrs crs) {
        return isSupported(apiData, null, crs);
    }

    @Override
    public boolean isSupported(OgcApiApiDataV2 apiData,
                               @Nullable FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                               EpsgCrs crs) {
        return getSupportedCrsList(apiData, featureTypeConfiguration).contains(crs);
    }

    @Override
    public EpsgCrs getStorageCrs(OgcApiApiDataV2 apiData,
                                 Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration) {
        FeatureProvider2 provider = featureTypeConfiguration.isPresent() ? providers.getFeatureProvider(apiData, featureTypeConfiguration.get()) : providers.getFeatureProvider(apiData);

        if (!provider.supportsCrs()) {
            throw new IllegalStateException("Provider has no CRS support.");
        }

        return provider.crs().getNativeCrs();
    }

    private EpsgCrs getDefaultCrs(OgcApiApiDataV2 apiData,
                                  Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration) {
        return apiData.getExtension(OgcApiFeaturesCoreConfiguration.class)
                      .get()
                      .getDefaultEpsgCrs();
    }

    private List<EpsgCrs> getAdditionalCrs(OgcApiApiDataV2 apiData,
                                           Optional<FeatureTypeConfigurationOgcApi> featureTypeConfiguration) {
        return apiData.getExtension(CrsConfiguration.class)
                      .map(CrsConfiguration::getAdditionalCrs)
                      .orElse(ImmutableList.of());
    }
}
