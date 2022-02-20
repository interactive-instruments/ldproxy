/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.common.domain.ConformanceDeclarationExtension;
import de.ii.ldproxy.ogcapi.common.domain.ImmutableConformanceDeclaration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.routes.sql.domain.RoutesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * add routes information to the conformance declaration
 */
@Singleton
@AutoBind
public class RoutesOnConformanceDeclaration implements ConformanceDeclarationExtension {

    private final FeaturesCoreProviders providers;

    @Inject
    public RoutesOnConformanceDeclaration(FeaturesCoreProviders providers) {
        this.providers = providers;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return RoutingConfiguration.class;
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, ValidationResult.MODE apiValidation) {
        ValidationResult result = ConformanceDeclarationExtension.super.onStartup(apiData, apiValidation);

        if (apiValidation== ValidationResult.MODE.NONE)
            return result;

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
            .from(result)
            .mode(apiValidation);

        // check that there is at least one preference/mode and that the default preference/mode is one of them
        FeatureProvider2 featureProvider = providers.getFeatureProviderOrThrow(apiData);

        List<String> preferences = List.copyOf(featureProvider.getData().getExtension(RoutesConfiguration.class)
                                                   .map(RoutesConfiguration::getPreferences)
                                                   .map(Map::keySet)
                                                   .orElse(ImmutableSet.of()));
        if (preferences.isEmpty())
            builder.addErrors("Routing: There must be at least one value for the routing preference.");
        apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getDefaultPreference)
            .ifPresentOrElse(defaultPreference -> {
                if (!preferences.contains(defaultPreference)) {
                    builder.addErrors("Routing: The default preference '{}' is not one of the known preference values: {}.", defaultPreference, preferences.toString());
                }
            }, () -> builder.addErrors("Routing: No default preference has been configured."));

        List<String> modes = List.copyOf(featureProvider.getData().getExtension(RoutesConfiguration.class)
                                             .map(RoutesConfiguration::getModes)
                                             .map(Map::keySet)
                                             .orElse(ImmutableSet.of()));
        if (modes.isEmpty())
            builder.addErrors("Routing: There must be at least one value for the routing mode.");
        apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getDefaultMode)
            .ifPresentOrElse(defaultMode -> {
                if (!modes.contains(defaultMode)) {
                    builder.addErrors("Routing: The default mode '{}' is not one of the known modes: {}.", defaultMode, modes.toString());
                }
            }, () -> builder.addErrors("Routing: No default mode has been configured."));
        modes.forEach(mode -> {
            if (!featureProvider.getData().getExtension(RoutesConfiguration.class)
                .map(RoutesConfiguration::getFromToQuery)
                .map(q -> q.containsKey(mode))
                .orElse(false))
                builder.addErrors("Routing: No 'fromToQuery' is specified for mode '{}'.", mode);
            if (!featureProvider.getData().getExtension(RoutesConfiguration.class)
                .map(RoutesConfiguration::getEdgesQuery)
                .map(q -> q.containsKey(mode))
                .orElse(false))
                builder.addErrors("Routing: No 'edgesQuery' is specified for mode '{}'.", mode);
        });

        return builder.build();
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

        List<String> preferences = List.copyOf(featureProvider.getData().getExtension(RoutesConfiguration.class)
            .map(RoutesConfiguration::getPreferences)
            .map(Map::keySet)
            .orElse(ImmutableSet.of()));
        List<String> modes = List.copyOf(featureProvider.getData().getExtension(RoutesConfiguration.class)
            .map(RoutesConfiguration::getModes)
            .map(Map::keySet)
            .orElse(ImmutableSet.of()));
        builder.putExtensions("properties",
                              ImmutableMap.of(CapabilityRouting.CORE,
                                              ImmutableMap.of("preferences",
                                                              preferences.stream().collect(ImmutableList.toImmutableList())),
                                              CapabilityRouting.MODE,
                                              ImmutableMap.of("modes",
                                                              modes.stream().collect(ImmutableList.toImmutableList()))));
        return builder;
    }
}
