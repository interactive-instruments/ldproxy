/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.filtertransformer;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiParameterExtension;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.ServerErrorException;
import java.util.Map;
import java.util.Optional;

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
    public boolean isEnabledForApi(OgcApiDatasetData apiData) {
        return isExtensionEnabled(apiData, OgcApiFeaturesCoreConfiguration.class);
    }

    @Override
    public ImmutableSet<String> getParameters(OgcApiDatasetData apiData, String subPath) {
        if (!isEnabledForApi(apiData))
            return ImmutableSet.of();

        if (subPath.matches("^/[\\w\\-]+/items/?$")) {
            // Features
            Optional<FeatureTypeConfigurationOgcApi> ft = apiData.getFeatureTypes()
                    .values()
                    .stream()
                    .filter(ftype -> apiData.isFeatureTypeEnabled(ftype.getId()))
                    .filter(ftype -> subPath.matches("^/" + ftype.getId() + "/items/?$"))
                    .findFirst();

            if (ft.isPresent()) {
                Map<String, String> filterableFields = apiData.getFilterableFieldsForFeatureType(ft.get().getId(), true);
                return new ImmutableSet.Builder<String>()
                        .addAll(filterableFields.keySet())
                        .build();
            }
            return ImmutableSet.of();
        } else if (subPath.matches("^/[\\w\\-]+/items/[^/\\s]+/?$")) {
            // Feature
            return ImmutableSet.of();
        }

        throw new ServerErrorException("Invalid sub path: "+subPath, 500);
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                   Map<String, String> parameters, OgcApiDatasetData apiData) {
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
