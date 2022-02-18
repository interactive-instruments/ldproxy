/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.core.domain.AbstractQueryParameterDatetime;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class QueryParameterDatetime extends AbstractQueryParameterDatetime {

    @Inject
    QueryParameterDatetime() {
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items"));
    }
}
