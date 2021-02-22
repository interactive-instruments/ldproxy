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
import java.util.OptionalInt;

@Component
@Provides
@Instantiate
public class QueryParameterHeight implements OgcApiQueryParameter {

    private final Schema schema;
    final FeatureProcessInfo featureProcessInfo;

    public QueryParameterHeight(@Requires FeatureProcessInfo featureProcessInfo) {
        this.featureProcessInfo = featureProcessInfo;
        this.schema = new NumberSchema();
    }

    @Override
    public String getName() {
        return "height";
    }

    @Override
    public String getDescription() {
        return "The number of grid cells in the vertical direction.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                featureProcessInfo.matches(apiData, ObservationProcess.class, definitionPath,"grid");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) { return schema; }

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
        OptionalInt gridHeight = OptionalInt.empty();
        if (parameters.containsKey(getName()))
            gridHeight = OptionalInt.of(Integer.valueOf(parameters.get(getName())));

        context.put(getName(),gridHeight);
        return context;
    }

}
