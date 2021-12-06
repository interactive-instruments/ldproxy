/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.projections;

import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Map;
import java.util.Objects;

@Component
@Provides
@Instantiate
public class QueryParameterSkipGeometry extends ApiExtensionCache implements OgcApiQueryParameter {

    private static final Schema<?> SCHEMA = new BooleanSchema()._default(false);

    @Override
    public String getId(String collectionId) {
        return String.format("%s_%s", getName(), collectionId);
    }

    @Override
    public String getName() {
        return "skipGeometry";
    }

    @Override
    public String getDescription() {
        return "Use this option to exclude geometries from the response for each feature.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method == HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                        definitionPath.equals("/collections/{collectionId}/items/{featureId}")));
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return SCHEMA;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return SCHEMA;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return ProjectionsConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 datasetData) {

        if (!isExtensionEnabled(datasetData.getCollections()
                                           .get(featureTypeConfiguration.getId()), ProjectionsConfiguration.class)) {
            return queryBuilder;
        }

        boolean skipGeometry = getSkipGeometry(parameters);

        return queryBuilder.skipGeometry(skipGeometry);
    }

    private boolean getSkipGeometry(Map<String, String> parameters) {
        if (parameters.containsKey(getName())) {
            return Objects.equals(parameters.get(getName()), "true");
        }
        return false;
    }
}
