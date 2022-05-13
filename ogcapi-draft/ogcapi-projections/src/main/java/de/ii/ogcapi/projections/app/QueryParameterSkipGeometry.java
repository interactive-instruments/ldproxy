/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.projections.app;

import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.util.Map;
import java.util.Objects;

/**
 * @langEn Use this option to exclude geometries from the response for each feature.
 * @langDe Todo
 * @name skipGeometry
 * @endpoints skipGeometry
 */

@Singleton
@AutoBind
public class QueryParameterSkipGeometry extends ApiExtensionCache implements OgcApiQueryParameter {

    private static final Schema<?> SCHEMA = new BooleanSchema()._default(false);

    @Inject
    QueryParameterSkipGeometry() {
    }

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
