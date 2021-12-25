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
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclarationExtension;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableConformanceDeclaration;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.routes.sql.domain.Preference;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.AbstractMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.CORE;
import static de.ii.ldproxy.ogcapi.routes.app.CapabilityRouting.MODE;

/**
 * add routes information to the conformance declaration
 */
@Component
@Provides
@Instantiate
public class RoutesOnConformanceDeclaration implements ConformanceDeclarationExtension {

    private final FeaturesCoreProviders providers;

    public RoutesOnConformanceDeclaration(@Requires FeaturesCoreProviders providers) {
        this.providers = providers;
    }

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

        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(apiData);

        // TODO check on startup that there is at least one preference/mode and that the default preference/mode is one of them
        List<String> preferences = List.copyOf(featureProvider.getData().getExtension(RoutesConfiguration.class)
            .map(RoutesConfiguration::getPreferences)
            .map(Map::keySet)
            .orElse(ImmutableSet.of()));
        List<String> modes = List.copyOf(featureProvider.getData().getExtension(RoutesConfiguration.class)
            .map(RoutesConfiguration::getModes)
            .map(Map::keySet)
            .orElse(ImmutableSet.of()));
        builder.putExtensions("properties",
                              ImmutableMap.of(CORE,
                                              ImmutableMap.of("preferences",
                                                              preferences.stream().collect(ImmutableList.toImmutableList())),
                                              MODE,
                                              ImmutableMap.of("modes",
                                                              modes.stream().collect(ImmutableList.toImmutableList()))));
        return builder;
    }
}
