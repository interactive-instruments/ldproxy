/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;


import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@AutoBind
public class PathParameterCollectionIdTiles extends AbstractPathParameterCollectionId {

    @Override
    public List<String> getValues(OgcApiDataV2 apiData) {
        if (!apiCollectionMap.containsKey(apiData.hashCode())) {
            apiCollectionMap.put(apiData.hashCode(), apiData.getCollections().values()
                                                            .stream()
                                                            .filter(collection -> apiData.isCollectionEnabled(collection.getId()))
                                                            .filter(collection -> collection.getExtension(TilesConfiguration.class).filter(ExtensionConfiguration::isEnabled).isPresent())
                                                            .map(FeatureTypeConfiguration::getId)
                                                            .collect(Collectors.toUnmodifiableList()));
        }

        return apiCollectionMap.get(apiData.hashCode());
    }

    @Override
    public boolean getExplodeInOpenApi(OgcApiDataV2 apiData) { return false; }

    @Override
    public String getId() {
        return "collectionIdTiles";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
               definitionPath.startsWith("/collections/{collectionId}/tiles");
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId) {
        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        final TilesConfiguration tilesConfiguration = collectionData.getExtension(TilesConfiguration.class)
                .orElseThrow(() -> new RuntimeException(MessageFormat.format("Could not access tiles configuration for API ''{0}'' and collection ''{1}''.", apiData.getId(), collectionId)));

        return tilesConfiguration.isEnabled() &&
               tilesConfiguration.isSingleCollectionEnabled() &&
               definitionPath.startsWith("/collections/{collectionId}/tiles");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, TilesConfiguration.class);
    }
}
