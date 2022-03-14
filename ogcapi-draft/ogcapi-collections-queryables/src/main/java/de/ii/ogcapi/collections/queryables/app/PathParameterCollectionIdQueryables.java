/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.queryables.app;


import de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
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
    public boolean matchesPath(String definitionPath) {
        return definitionPath.equals("/collections/{collectionId}/queryables");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return QueryablesConfiguration.class;
    }
}
