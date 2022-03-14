/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;


import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@AutoBind
public class PathParameterCollectionIdStyles extends AbstractPathParameterCollectionId {

    @Inject
    PathParameterCollectionIdStyles() {
    }

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
    public boolean matchesPath(String definitionPath) {
        return definitionPath.startsWith("/collections/{collectionId}/styles");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return StylesConfiguration.class;
    }
}
