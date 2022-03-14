/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.ogcapi.common.domain.GenericFormatExtension;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ogcapi.resources.app.Resources;

import java.util.Optional;

@AutoMultiBind
public interface ResourcesFormatExtension extends GenericFormatExtension {

    @Override
    default String getPathPattern() {
        return "^/resources/?$";
    }

    Object getResourcesEntity(Resources resources,
                              OgcApiDataV2 apiData,
                              ApiRequestContext requestContext);

    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ResourcesConfiguration.class;
    }

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return GenericFormatExtension.super.isEnabledForApi(apiData) ||
            apiData.getExtension(StylesConfiguration.class).map(StylesConfiguration::isResourcesEnabled).orElse(false);
    }
}
