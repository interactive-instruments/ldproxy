/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessInfo;
import de.ii.ldproxy.ogcapi.observation_processing.api.ObservationProcess;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

@Component
@Provides
@Instantiate
public class QueryParameterWidth extends ApiExtensionCache implements OgcApiQueryParameter {

    private final Schema baseSchema;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterWidth(@Requires FeatureProcessInfo featureProcessInfo) {
        this.featureProcessInfo = featureProcessInfo;
        baseSchema = new NumberSchema();
    }

    @Override
    public String getName() {
        return "width";
    }

    @Override
    public String getDescription() {
        return "The number of grid cells in the horizontal direction.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"grid"));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        OptionalInt defValue = getDefault(apiData, Optional.empty());
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.getAsInt());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        OptionalInt defValue = getDefault(apiData, Optional.of(collectionId));
        if (defValue.isPresent()) {
            Schema schema = baseSchema;
            schema.setDefault(defValue.getAsInt());
            return schema;
        }
        return baseSchema;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ObservationProcessingConfiguration.class) ||
                apiData.getCollections()
                        .values()
                        .stream()
                        .filter(FeatureTypeConfigurationOgcApi::getEnabled)
                        .anyMatch(featureType -> isEnabledForApi(apiData, featureType.getId()));
}

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ObservationProcessingConfiguration.class;
    }

    @Override
    public Map<String, Object> transformContext(FeatureTypeConfigurationOgcApi featureType,
                                                Map<String, Object> context,
                                                Map<String, String> parameters,
                                                OgcApiDataV2 apiData) {
        OptionalInt gridWidth;
        if (parameters.containsKey(getName()))
            gridWidth = OptionalInt.of(Integer.valueOf(parameters.get(getName())));
        else
            gridWidth = getDefault(apiData, Optional.of(featureType.getId()));

        context.put(getName(),gridWidth);
        return context;
    }

    private OptionalInt getDefault(OgcApiDataV2 apiData, Optional<String> collectionId) {
        FeatureTypeConfigurationOgcApi featureType = collectionId.isPresent() ? apiData.getCollections().get(collectionId.get()) : null;
        Optional<ObservationProcessingConfiguration> config = featureType!=null ?
                featureType.getExtension(ObservationProcessingConfiguration.class) :
                apiData.getExtension(ObservationProcessingConfiguration.class);
        if (config.isPresent()) {
            return config.get().getDefaultWidth();
        }
        return OptionalInt.empty();
    }
}
