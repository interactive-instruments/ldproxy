/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.common.domain.QueryParameterF;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterFRoute extends QueryParameterF {

    @Inject
    public QueryParameterFRoute(ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fRoute";
    }

    @Override
    protected boolean matchesPath(String definitionPath) {
        return definitionPath.equals("/routes/{routeId}");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return RouteFormatExtension.class;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

}
