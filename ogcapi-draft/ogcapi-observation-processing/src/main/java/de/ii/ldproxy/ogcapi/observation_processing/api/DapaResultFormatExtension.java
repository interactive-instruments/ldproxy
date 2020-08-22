/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.domain.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variable;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXy;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXyt;
import de.ii.ldproxy.ogcapi.observation_processing.data.Geometry;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.streams.domain.Http;

import java.io.IOException;
import java.io.OutputStream;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DapaResultFormatExtension extends FormatExtension {

    final static String DAPA_PATH_ELEMENT = "dapa";

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(ObservationProcessingConfiguration.class)
                      .filter(config -> config.getResultEncodings().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(ObservationProcessingConfiguration.class)
                      .filter(config -> config.getResultEncodings().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    default String getPathPattern() {
        return "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/position(?:\\:aggregate-time)?/?$)|" +
                "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/area(?:\\:aggregate-(space|time|space-time))?/?$)|" +
                "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/resample-to-grid(?:\\:aggregate-time)?/?$)";
    }

    default boolean canTransformFeatures() {
        return true;
    }

    default Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContextObservationProcessing transformationContext,
                                                                FeaturesCoreProviders providers,
                                                                Http http) {
        OgcApiDataV2 serviceData = transformationContext.getApiData();
        String collectionName = transformationContext.getCollectionId();
        String staticUrlPrefix = transformationContext.getOgcApiRequest()
                .getStaticUrlPrefix();
        URICustomizer uriCustomizer = transformationContext.getOgcApiRequest()
                                                           .getUriCustomizer();

        if (transformationContext.isFeatureCollection()) {
            FeatureTypeConfigurationOgcApi collectionData = serviceData.getCollections()
                                                                       .get(collectionName);
            Optional<OgcApiFeaturesCoreConfiguration> featuresCoreConfiguration = collectionData.getExtension(OgcApiFeaturesCoreConfiguration.class);
            Optional<ObservationProcessingConfiguration> obsProcConfiguration = collectionData.getExtension(ObservationProcessingConfiguration.class);
            FeatureProviderDataV2 providerData = providers.getFeatureProvider(serviceData, collectionData)
                    .getData();

            Map<String, String> filterableFields = featuresCoreConfiguration
                    .map(OgcApiFeaturesCoreConfiguration::getOtherFilterParameters)
                    .orElse(ImmutableMap.of());

        } else {
            // TODO throw error
        }

        FeatureTransformer2 transformer = new FeatureTransformerObservationProcessing(transformationContext, http.getDefaultClient());

        return Optional.of(transformer);
    }

    Object initializeResult(FeatureProcessChain processes, Map<String, Object> processingParameters, List<Variable> variables, OutputStream outputStream) throws IOException;
    default boolean addDataArray(Object result, DataArrayXyt array) throws IOException { return false; }
    default boolean addDataArray(Object result, DataArrayXy array) throws IOException { return false; }
    void addFeature(Object result, Optional<String> locationCode, Optional<String> locationName, Geometry geometry, Temporal timeBegin, Temporal timeEnd, Map<String, Number> values) throws IOException;
    void finalizeResult(Object result) throws IOException;
}
