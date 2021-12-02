/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.filter.domain.FilterConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Provides
@Instantiate
public class QueryParameterFilterCrs extends ApiExtensionCache implements OgcApiQueryParameter {

    public static final String FILTER_CRS = "filter-crs";
    public static final String CRS84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

    private final CrsSupport crsSupport;

    public QueryParameterFilterCrs(@Requires CrsSupport crsSupport) {
        this.crsSupport = crsSupport;
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
                isEnabledForApi(apiData) &&
                method == HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                 definitionPath.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}") ||
                 definitionPath.equals("/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
    }

    @Override
    public String getName() {
        return "filter-crs";
    }

    @Override
    public String getDescription() {
        return "Specify which of the supported CRSs to use to encode geometric values in a filter expression (parameter 'filter'). Default is WGS84 longitude/latitude (with or without height).";
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            List<String> crsList = crsSupport.getSupportedCrsList(apiData)
                                             .stream()
                                             .map(EpsgCrs::toUriString)
                                             .collect(ImmutableList.toImmutableList());
            schemaMap.get(apiHashCode).put("*", new StringSchema()._enum(crsList)._default(CRS84));
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            List<String> crsList = crsSupport.getSupportedCrsList(apiData, apiData.getCollections().get(collectionId))
                                             .stream()
                                             .map(EpsgCrs::toUriString)
                                             .collect(ImmutableList.toImmutableList());
            String defaultCrs = apiData.getExtension(FeaturesCoreConfiguration.class, collectionId)
                .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                .map(ImmutableEpsgCrs::toUriString)
                .orElse(CRS84);
            schemaMap.get(apiHashCode).put(collectionId, new StringSchema()._enum(crsList)._default(defaultCrs));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                   Map<String, String> parameters, OgcApiDataV2 datasetData) {
        if (!isEnabledForApi(datasetData, featureTypeConfiguration.getId())) {
            return parameters;
        }
        if (parameters.containsKey(FILTER_CRS)) {
            EpsgCrs filterCrs;
            try {
                filterCrs = EpsgCrs.fromString(parameters.get(FILTER_CRS));
            } catch (Throwable e) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: %s", FILTER_CRS, e.getMessage()), e);
            }
            if (!crsSupport.isSupported(datasetData, featureTypeConfiguration, filterCrs)) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: the crs '%s' is not supported", FILTER_CRS, filterCrs.toUriString()));
            }

            Map<String, String> newParameters = new HashMap<>(parameters);
            newParameters.put(FILTER_CRS, filterCrs.toUriString());
            return ImmutableMap.copyOf(newParameters);
        }
        return parameters;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FilterConfiguration.class;
    }
}
