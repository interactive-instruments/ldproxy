/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.api;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.features.core.api.OgcApiFeatureCoreProviders;
import de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import de.ii.ldproxy.ogcapi.observation_processing.application.Variable;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXy;
import de.ii.ldproxy.ogcapi.observation_processing.data.DataArrayXyt;
import de.ii.ldproxy.ogcapi.observation_processing.data.Geometry;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;

import java.io.IOException;
import java.io.OutputStream;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ObservationProcessingOutputFormat extends FormatExtension {

    final static String DAPA_PATH_ELEMENT = "dapa";

    default String getPathPattern() {
        return "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/position(?:\\:aggregate-time)?/?$)|" +
                "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/area(?:\\:aggregate-(space|time|space-time))?/?$)|" +
                "(?:^/collections/[\\w\\-]+/"+DAPA_PATH_ELEMENT+"/resample-to-grid(?:\\:aggregate-time)?/?$)";
    }

    default boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(featureType -> featureType.getEnabled())
                        .filter(featureType -> isEnabledForApi(apiData, featureType.getId()))
                        .findAny()
                        .isPresent();
    }

    default boolean isEnabledForApi(OgcApiApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData, apiData.getCollections().get(collectionId), ObservationProcessingConfiguration.class);
    }

    default boolean canTransformFeatures() {
        return true;
    }

    default Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContextObservationProcessing transformationContext,
                                                                OgcApiFeatureCoreProviders providers,
                                                                Http http) {
        OgcApiApiDataV2 serviceData = transformationContext.getApiData();
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
