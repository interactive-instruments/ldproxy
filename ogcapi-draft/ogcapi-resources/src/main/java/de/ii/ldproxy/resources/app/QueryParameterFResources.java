/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.common.domain.QueryParameterF;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ldproxy.ogcapi.foundation.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.styles.domain.StylesConfiguration;
import de.ii.ldproxy.resources.domain.ResourcesConfiguration;
import de.ii.ldproxy.resources.domain.ResourcesFormatExtension;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterFResources extends QueryParameterF {

    @Inject
    public QueryParameterFResources(ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fResources";
    }

    @Override
    protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return definitionPath.equals("/resources");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return ResourcesFormatExtension.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<ResourcesConfiguration> resourcesExtension = apiData.getExtension(ResourcesConfiguration.class);
        Optional<StylesConfiguration> stylesExtension = apiData.getExtension(StylesConfiguration.class);

        if ((resourcesExtension.isPresent() && resourcesExtension.get()
                                                                 .isEnabled()) ||
                (stylesExtension.isPresent() && stylesExtension.get()
                                                               .getResourcesEnabled())) {
            return true;
        }
        return false;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ResourcesConfiguration.class;
    }

}
