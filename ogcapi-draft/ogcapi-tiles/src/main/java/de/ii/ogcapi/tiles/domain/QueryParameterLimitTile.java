/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@AutoBind
public class QueryParameterLimitTile extends ApiExtensionCache implements OgcApiQueryParameter {

    @Inject
    QueryParameterLimitTile() {
    }

    @Override
    public String getId() {
        return "limitTile";
    }

    @Override
    public String getName() {
        return "limit";
    }

    @Override
    public String getDescription() {
        return "The optional limit parameter limits the number of features that are included in the tile.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                (definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                    definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, String collectionId, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + collectionId + method.name(), () ->
            isEnabledForApi(apiData, collectionId) &&
                method== HttpMethods.GET &&
                definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    private final ConcurrentMap<Integer, Map<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            Schema schema = new IntegerSchema().minimum(BigDecimal.valueOf(0));

            Optional<Integer> limit = apiData.getExtension(TilesConfiguration.class)
                    .map(TilesConfiguration::getLimitDerived);
            limit.ifPresent(integer -> schema.setDefault(BigDecimal.valueOf(integer)));

            schemaMap.get(apiHashCode)
                     .put("*", schema);
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            Schema schema = new IntegerSchema().minimum(BigDecimal.valueOf(0));

            FeatureTypeConfigurationOgcApi featureType = apiData.getCollections().get(collectionId);
            Optional<Integer> limit = featureType.getExtension(TilesConfiguration.class)
                    .map(TilesConfiguration::getLimitDerived);
            limit.ifPresent(integer -> schema.setDefault(BigDecimal.valueOf(integer)));

            schemaMap.get(apiHashCode)
                     .put(collectionId, schema);
        }
        return schemaMap.get(apiHashCode)
                        .get(collectionId);
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters,
                                                        OgcApiDataV2 apiData) {
        if (parameters.containsKey(getName())) {
            queryBuilder.limit(Integer.parseInt(parameters.get(getName())));
        }

        return queryBuilder;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
        return config.isPresent() && config.get().getTileProvider().requiresQuerySupport();
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        Optional<TilesConfiguration> config = apiData.getCollections()
                                                     .get(collectionId)
                                                     .getExtension(TilesConfiguration.class);
        return config.isPresent() && config.get().getTileProvider().requiresQuerySupport();
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }
}
