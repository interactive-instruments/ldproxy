/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclarationExtension;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableConformanceDeclaration;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.routes.domain.Preference;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingFlag;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.CORE;

/**
 * add routes information to the conformance declaration
 */
@Component
@Provides
@Instantiate
public class RoutesOnConformanceDeclaration implements ConformanceDeclarationExtension {

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public ImmutableConformanceDeclaration.Builder process(ImmutableConformanceDeclaration.Builder builder,
                                                           OgcApiDataV2 apiData,
                                                           URICustomizer uriCustomizer,
                                                           ApiMediaType mediaType,
                                                           List<ApiMediaType> alternateMediaTypes,
                                                           Optional<Locale> language) {

        if (!isEnabledForApi(apiData)) {
            return builder;
        }

        // TODO check on startup that there is at least one preference and that the default preference is one of them
        Map<String, Preference> preferences = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getPreferences)
            .orElse(ImmutableMap.of());
        builder.putExtensions(CORE,ImmutableMap.of("preferences", preferences.keySet().stream().collect(ImmutableList.toImmutableList())));

        return builder;
    }
}
