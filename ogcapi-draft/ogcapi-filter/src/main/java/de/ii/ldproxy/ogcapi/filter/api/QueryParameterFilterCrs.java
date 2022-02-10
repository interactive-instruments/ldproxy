/**
 * Copyright 2022 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ldproxy.ogcapi.filter.domain.FilterConfiguration;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
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
    private final FeaturesCoreProviders providers;

    public QueryParameterFilterCrs(@Requires CrsSupport crsSupport,
                                   @Requires FeaturesCoreProviders providers) {
        this.crsSupport = crsSupport;
        this.providers = providers;
    }

    @Override
    public boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return super.isEnabledForApi(apiData) && providers.getFeatureProvider(apiData).map(FeatureProvider2::supportsQueries).orElse(false);
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
        return "Specify which of the supported CRSs to use to encode geometric values in a filter expression (parameter 'filter'). Default is WGS84 longitude/latitude.";
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema<?>>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey("*")) {
            // TODO: only include 2D (variants) of the CRSs
            String defaultCrs = CRS84 /* TODO support 4 or 6 numbers
            apiData.getExtension(FeaturesCoreConfiguration.class, collectionId)
                .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                .map(ImmutableEpsgCrs::toUriString)
                .orElse(CRS84) */;
            List<String> crsList = crsSupport.getSupportedCrsList(apiData)
                .stream()
                .map(crs ->crs.equals(OgcCrs.CRS84h) ? OgcCrs.CRS84 : crs)
                .map(EpsgCrs::toUriString)
                .collect(ImmutableList.toImmutableList());
            schemaMap.get(apiHashCode).put("*", new StringSchema()._enum(crsList)._default(defaultCrs));
        }
        return schemaMap.get(apiHashCode).get("*");
    }

    @Override
    public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            // always support both default CRSs
            String defaultCrs = apiData.getExtension(FeaturesCoreConfiguration.class, collectionId)
                .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                .map(EpsgCrs::toUriString)
                .orElse(CRS84);
            ImmutableList.Builder<String> crsListBuilder = new ImmutableList.Builder<>();
            List<String> crsList = crsSupport.getSupportedCrsList(apiData, apiData.getCollections().get(collectionId))
                .stream()
                .map(EpsgCrs::toUriString)
                .collect(ImmutableList.toImmutableList());
            crsListBuilder.addAll(crsList);
            if (!crsList.contains(CRS84))
                crsListBuilder.add(CRS84);
            schemaMap.get(apiHashCode).put(collectionId, new StringSchema()._enum(crsListBuilder.build())._default(defaultCrs));
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
            // CRS84 is always supported
            if (!crsSupport.isSupported(datasetData, featureTypeConfiguration, filterCrs) && !filterCrs.equals(OgcCrs.CRS84)) {
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
