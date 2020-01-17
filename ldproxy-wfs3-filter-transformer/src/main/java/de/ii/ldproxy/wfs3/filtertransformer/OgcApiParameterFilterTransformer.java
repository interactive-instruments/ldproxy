/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiParameterExtension;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiParameterFilterTransformer implements OgcApiParameterExtension {

    private final HttpClient httpClient;

    public OgcApiParameterFilterTransformer(@Requires Http http) {
        this.httpClient = http.getDefaultClient();
    }

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath) {
        if (!isEnabledForApi(apiData))
            return ImmutableSet.of();

        if (subPath.matches("^/[\\w\\-]+/items/?$")) {
            // Features

            ImmutableSet.Builder<String> parameters = new ImmutableSet.Builder<>();
            Set<String> parametersFromConfiguration = apiData.getFeatureTypes()
                    .values()
                    .stream()
                    .flatMap(featureTypeConfigurationOgcApi -> featureTypeConfigurationOgcApi.getCapabilities().stream())
                    .filter(extensionConfiguration -> extensionConfiguration instanceof FilterTransformersConfiguration)
                    .flatMap(extensionConfiguration -> ((FilterTransformersConfiguration) extensionConfiguration).getTransformers().stream())
                    .filter(filterTransformerConfiguration -> filterTransformerConfiguration instanceof RequestGeoJsonBboxConfiguration)
                    .flatMap(filterTransformerConfiguration -> ((RequestGeoJsonBboxConfiguration) filterTransformerConfiguration).getParameters().stream())
                    .collect(Collectors.toSet());
            parameters.addAll(parametersFromConfiguration);

            Optional<FeatureTypeConfigurationOgcApi> ft = apiData.getFeatureTypes()
                    .values()
                    .stream()
                    .filter(ftype -> apiData.isCollectionEnabled(ftype.getId()))
                    .filter(ftype -> subPath.matches("^/" + ftype.getId() + "/items/?$"))
                    .findFirst();
            Optional<List<String>> otherQueryables = ft.flatMap(featureTypeConfigurationOgcApi -> featureTypeConfigurationOgcApi.getExtension(OgcApiFeaturesCoreConfiguration.class))
                                               .flatMap(OgcApiFeaturesCoreConfiguration::getQueryables)
                                               .map(OgcApiFeaturesCollectionQueryables::getOther);
            if (otherQueryables.isPresent()) {
                parameters.addAll(otherQueryables.get());
            }

            return parameters.build();
        } else if (subPath.matches("^/[\\w\\-]+/items/[^/\\s]+/?$")) {
            // Feature
            return ImmutableSet.of();
        }

        return ImmutableSet.of();
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                   Map<String, String> parameters, OgcApiApiDataV2 apiData) {
        final Optional<FilterTransformersConfiguration> filterTransformersConfiguration = featureTypeConfiguration.getExtension(FilterTransformersConfiguration.class);

        if (filterTransformersConfiguration.isPresent()) {

            Map<String, String> nextParameters = parameters;

            for (FilterTransformerConfiguration filterTransformerConfiguration : filterTransformersConfiguration.get()
                                                                                                                .getTransformers()) {
                //TODO
                if (filterTransformerConfiguration instanceof RequestGeoJsonBboxConfiguration) {
                    RequestGeoJsonBboxTransformer requestGeoJsonBboxTransformer = new RequestGeoJsonBboxTransformer((RequestGeoJsonBboxConfiguration) filterTransformerConfiguration, httpClient);
                    nextParameters = requestGeoJsonBboxTransformer.resolveParameters(nextParameters);

                }
            }

            return nextParameters;
        }
        return parameters;
    }
}
