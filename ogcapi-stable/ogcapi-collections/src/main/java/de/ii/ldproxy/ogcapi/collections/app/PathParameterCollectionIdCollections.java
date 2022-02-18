/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class PathParameterCollectionIdCollections extends AbstractPathParameterCollectionId {

    @Inject
    public PathParameterCollectionIdCollections() {
    }

    @Override
    public String getId() {
        return "collectionIdCollections";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return isEnabledForApi(apiData) &&
                definitionPath.equals("/collections/{collectionId}");
    }
}
