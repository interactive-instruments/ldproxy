/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;


import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Provides
@Instantiate
public class PathParameterCollectionIdStyles extends AbstractPathParameterCollectionId {

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.hashCode())) {
            apiCollectionMap.put(apiData.hashCode(), apiData.getCollections().values()
                                                            .stream()
                                                            .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                                                            .filter(collection -> collection.getExtension(StylesConfiguration.class).filter(ExtensionConfiguration::isEnabled).isPresent())
                                                            .map(FeatureTypeConfiguration::getId)
                                                            .collect(Collectors.toUnmodifiableList()));
        }

        return apiCollectionMap.get(apiData.hashCode());
    }

    @Override
    public boolean getExplodeInOpenApi(OgcApiDataV2 apiData) { return false;
    }

    @Override
    public String getId() {
        return "collectionIdStyles";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               definitionPath.startsWith("/collections/{collectionId}/styles");
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        final StylesConfiguration configuration = collectionData.getExtension(StylesConfiguration.class)
                .orElseThrow(() -> new RuntimeException(MessageFormat.format("Could not access styles configuration for API ''{0}'' and collection ''{1}''.", apiData.getId(), collectionId)));

        return configuration.isEnabled() &&
               definitionPath.startsWith("/collections/{collectionId}/styles");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, StylesConfiguration.class);
    }
}
