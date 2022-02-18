/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.schema.domain;


import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;


@Singleton
@AutoBind
public class PathParameterCollectionIdSchema extends AbstractPathParameterCollectionId {

    @Override
    public String getId() {
        return "collectionIdSchema";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) && definitionPath.equals("/collections/{collectionId}/schemas/{type}");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, FeaturesCoreConfiguration.class);
    }
}
