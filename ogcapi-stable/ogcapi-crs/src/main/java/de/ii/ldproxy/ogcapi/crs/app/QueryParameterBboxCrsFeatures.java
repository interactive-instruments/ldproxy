/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.crs.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.crs.domain.CrsConfiguration;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.foundation.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.github.azahnen.dagger.annotations.AutoBind;

@Singleton
@AutoBind
public class QueryParameterBboxCrsFeatures extends ApiExtensionCache implements OgcApiQueryParameter {

    public static final String BBOX = "bbox";
    public static final String BBOX_CRS = "bbox-crs";
    public static final String CRS84 = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    public static final String CRS84H = "http://www.opengis.net/def/crs/OGC/0/CRS84h";

    private final CrsSupport crsSupport;

    @Inject
    public QueryParameterBboxCrsFeatures(CrsSupport crsSupport) {
        this.crsSupport = crsSupport;
    }

    @Override
    public String getId(String collectionId) {
        return BBOX_CRS+"Features_"+collectionId;
    }

    @Override
    public String getName() {
        return BBOX_CRS;
    }

    @Override
    public String getDescription() {
        return "The coordinate reference system of the `bbox` parameter. Default is WGS84 longitude/latitude.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return computeIfAbsent(this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(), () ->
            isEnabledForApi(apiData) &&
                method== HttpMethods.GET &&
                definitionPath.equals("/collections/{collectionId}/items"));
    }

    private ConcurrentMap<Integer, ConcurrentMap<String,Schema>> schemaMap = new ConcurrentHashMap<>();

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        int apiHashCode = apiData.hashCode();
        if (!schemaMap.containsKey(apiHashCode))
            schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
        if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
            // TODO: include 2D (variants) of the CRSs
            // default is currently always CRS84
            String defaultCrs = CRS84
            /* TODO support 4 or 6 numbers
            apiData.getExtension(FeaturesCoreConfiguration.class, collectionId)
                .map(FeaturesCoreConfiguration::getDefaultEpsgCrs)
                .map(EpsgCrs::toUriString)
                .orElse(CRS84) */;
            List<String> crsList = crsSupport.getSupportedCrsList(apiData, apiData.getCollections().get(collectionId))
                .stream()
                .map(crs ->crs.equals(OgcCrs.CRS84h) ? OgcCrs.CRS84 : crs)
                .map(EpsgCrs::toUriString)
                .collect(ImmutableList.toImmutableList());
            schemaMap.get(apiHashCode)
                     .put(collectionId, new StringSchema()._enum(crsList)._default(defaultCrs));
        }
        return schemaMap.get(apiHashCode).get(collectionId);
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return CrsConfiguration.class;
    }

    @Override
    public Map<String, String> transformParameters(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                   Map<String, String> parameters, OgcApiDataV2 datasetData) {

        if (!isEnabledForApi(datasetData, featureTypeConfiguration.getId())) {
            return parameters;
        }

        if (parameters.containsKey(BBOX) && parameters.containsKey(BBOX_CRS)) {
            EpsgCrs bboxCrs;
            try {
                bboxCrs = EpsgCrs.fromString(parameters.get(BBOX_CRS));
            } catch (Throwable e) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: %s", BBOX_CRS, e.getMessage()), e);
            }
            // CRS84 is always supported
            if (!crsSupport.isSupported(datasetData, featureTypeConfiguration, bboxCrs) && !bboxCrs.equals(OgcCrs.CRS84)) {
                throw new IllegalArgumentException(String.format("The parameter '%s' is invalid: the crs '%s' is not supported", BBOX_CRS, bboxCrs.toUriString()));
            }

            Map<String, String> newParameters = new HashMap<>(parameters);
            newParameters.put(BBOX, String.format("%s,%s", parameters.get(BBOX), bboxCrs.toUriString()));
            return ImmutableMap.copyOf(newParameters);
        }

        return parameters;
    }
}
