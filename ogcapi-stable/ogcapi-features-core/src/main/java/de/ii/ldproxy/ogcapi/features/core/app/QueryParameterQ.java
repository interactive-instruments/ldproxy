/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCollectionQueryables;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.Optional;

@Component
@Provides
@Instantiate
public class QueryParameterQ implements OgcApiQueryParameter {

    private final Schema baseSchema;

    public QueryParameterQ() {
        baseSchema = new ArraySchema().items(new StringSchema());
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
        return isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               definitionPath.equals("/collections/{collectionId}/items");
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        return isEnabledForApi(apiData, collectionId) &&
                method== HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items");
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        FeatureTypeConfigurationOgcApi collectionData = apiData.getCollections().get(collectionId);
        Optional<FeaturesCoreConfiguration> config = collectionData.getExtension(FeaturesCoreConfiguration.class);
        if (config.isPresent() && config.get().isEnabled()) {
            return !config.get()
                          .getQueryables()
                          .orElse(FeaturesCollectionQueryables.of())
                          .getQ()
                          .isEmpty();
        }
        return false;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
