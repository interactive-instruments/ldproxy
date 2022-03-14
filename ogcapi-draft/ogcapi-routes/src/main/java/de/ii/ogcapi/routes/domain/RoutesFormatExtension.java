/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;

@AutoMultiBind
public interface RoutesFormatExtension extends FormatExtension {

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return false;
    }

    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    default String getPathPattern() {
        return "^/routes/?$";
    }

    Object getRoutesEntity(Routes routes, OgcApi api, ApiRequestContext requestContext);
}
