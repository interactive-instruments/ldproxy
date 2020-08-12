/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.ExtendableConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Optional;

@Component
@Provides
@Instantiate
public class OgcApiFeaturesCoreProvidersImpl implements OgcApiFeatureCoreProviders {

    private final EntityRegistry entityRegistry;

    public OgcApiFeaturesCoreProvidersImpl(@Requires EntityRegistry entityRegistry) {
        this.entityRegistry = entityRegistry;
    }

    @Override
    public FeatureProvider2 getFeatureProvider(OgcApiApiDataV2 apiData) {
        Optional<FeatureProvider2> optionalFeatureProvider = getOptionalFeatureProvider(apiData);

        if (!optionalFeatureProvider.isPresent()) {
            optionalFeatureProvider = entityRegistry.getEntity(FeatureProvider2.class, apiData.getId());
        }
        return optionalFeatureProvider
                .orElseThrow(() -> new IllegalStateException("no FeatureProvider found"));
    }

    @Override
    public FeatureProvider2 getFeatureProvider(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureType) {
        return getOptionalFeatureProvider(featureType)
                .orElse(getFeatureProvider(apiData));
    }

    private Optional<FeatureProvider2> getOptionalFeatureProvider(ExtendableConfiguration extendableConfiguration) {
        return extendableConfiguration.getExtension(OgcApiFeaturesCoreConfiguration.class)
                                      .flatMap(OgcApiFeaturesCoreConfiguration::getFeatureProvider)
                                      .flatMap(id -> entityRegistry.getEntity(FeatureProvider2.class, id));
    }
}
