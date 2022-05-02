/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class QueryParameterQ extends ApiExtensionCache implements OgcApiQueryParameter {

    private final Schema<?> baseSchema;
    private final SchemaValidator schemaValidator;

    @Inject
    public QueryParameterQ(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
        this.baseSchema = new ArraySchema().items(new StringSchema());
    }

    @Override
    public String getName() {
        return "q";
    }

    @Override
    public String getDescription() {
        return "General text search in multiple text properties of the data. Separate search terms by comma." +
               "If at least one of the search terms is included in an item, it is included in the result set. " +
               "Known limitation: The search should be case-insensitive, but currently is case-sensitive.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               definitionPath.equals("/collections/{collectionId}/items"));
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + collectionId + method.name(), () ->
            isEnabledForApi(apiData, collectionId) &&
                method== HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items"));
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return isExtensionEnabled(apiData.getCollections().get(collectionId), FeaturesCoreConfiguration.class, config -> !config.getQueryables()
            .orElse(FeaturesCollectionQueryables.of())
            .getQ()
            .isEmpty());
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
