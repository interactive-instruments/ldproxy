/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.geometry.simplification.app;

import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.math.BigDecimal;
import java.util.Map;

@Singleton
@AutoBind
public class QueryParameterMaxAllowableOffsetFeatures extends ApiExtensionCache implements
    OgcApiQueryParameter {

    private final SchemaValidator schemaValidator;

    @Inject
    QueryParameterMaxAllowableOffsetFeatures(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    @Override
    public String getName() {
        return "maxAllowableOffset";
    }

    @Override
    public String getDescription() {
        return "This option can be used to specify the maxAllowableOffset to be used for simplifying the geometries in the response. " +
                "The maxAllowableOffset is in the units of the response coordinate reference system.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/collections/{collectionId}/items/{featureId}")));
    }

    private final Schema<?> schema = new NumberSchema()._default(BigDecimal.valueOf(0)).example(0.05);

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData) {
        return schema;
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        return schema;
    }

    @Override
    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return GeometrySimplificationConfiguration.class;
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(
        FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDataV2 datasetData) {
        if (!isExtensionEnabled(datasetData.getCollections()
                                           .get(featureTypeConfiguration.getId()), GeometrySimplificationConfiguration.class)) {
            return queryBuilder;
        }
        if (parameters.containsKey("maxAllowableOffset")) {
            try {
                queryBuilder.maxAllowableOffset(Double.valueOf(parameters.get("maxAllowableOffset")));
            } catch (NumberFormatException e) {
                //ignore
            }
        }

        return queryBuilder;
    }
}
