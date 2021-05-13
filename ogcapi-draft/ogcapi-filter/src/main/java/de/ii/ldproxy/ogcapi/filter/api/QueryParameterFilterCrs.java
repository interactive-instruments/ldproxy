/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter.api;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.crs.domain.CrsSupport;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;

import java.util.HashMap;
import java.util.Map;

@Component
@Provides
@Instantiate
public class QueryParameterFilterCrs implements OgcApiQueryParameter {

    public static final String FILTER_CRS = "filter-crs";

    private final CrsSupport crsSupport;

    public QueryParameterFilterCrs(@Requires CrsSupport crsSupport) {
        this.crsSupport = crsSupport;
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
                method == HttpMethods.GET &&
                (definitionPath.equals("/collections/{collectionId}/items") ||
                        definitionPath.endsWith("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"));
    }

    @Override
    public String getName() {
        return "filter-crs";
    }

    @Override
    public String getDescription() {
        return "Specify which of the supported CRSs to use to encode geometric values in a filter expression";
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
}
