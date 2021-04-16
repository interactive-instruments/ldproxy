/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.parameters;

import de.ii.ldproxy.ogcapi.common.domain.QueryParameterF;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.observation_processing.api.DapaVariablesFormatExtension;
import de.ii.ldproxy.ogcapi.observation_processing.application.ObservationProcessingConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

@Component
@Provides
@Instantiate
public class QueryParameterFVariables extends QueryParameterF {

    protected QueryParameterFVariables(@Requires ExtensionRegistry extensionRegistry) {
        super(extensionRegistry);
    }

    @Override
    public String getId() {
        return "fVariables";
    }

    @Override
    protected boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
        return definitionPath.equals("/collections/{collectionId}/variables");
    }

    @Override
    protected Class<? extends FormatExtension> getFormatClass() {
        return DapaVariablesFormatExtension.class;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) ||
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
}
