/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.domain;


import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiPathParameter;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
@AutoBind
public class PathParameterCollectionIdMaps extends AbstractPathParameterCollectionId {

    @Inject
    PathParameterCollectionIdMaps() {
    }

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
        return "collectionIdMapTiles";
    }

    @Override
    public boolean matchesPath(String definitionPath) {
        return definitionPath.startsWith("/collections/{collectionId}/map/tiles");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return MapTilesConfiguration.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        final FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        return super.isEnabledForApi(apiData, collectionId) &&
            Objects.nonNull(collectionData) &&
            isExtensionEnabled(collectionData, TilesConfiguration.class) &&
            collectionData.getEnabled();
    }
}
