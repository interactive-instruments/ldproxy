/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;


import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;


@Singleton
@AutoBind
public class PathParameterCollectionIdQueryables extends AbstractPathParameterCollectionId {

    @Inject
    PathParameterCollectionIdQueryables() {
    }

    @Override
    public String getId() {
        return "collectionIdQueryables";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) && definitionPath.matches("/collections/\\{collectionId\\}/queryables");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, FeaturesCoreConfiguration.class);
    }
}