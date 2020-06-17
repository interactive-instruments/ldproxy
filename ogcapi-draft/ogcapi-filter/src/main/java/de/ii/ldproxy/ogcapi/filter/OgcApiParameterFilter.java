/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiParameterExtension;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Provides
@Instantiate
public class OgcApiParameterFilter implements OgcApiParameterExtension, ConformanceClass {

    private static final String FILTER_LANG_CQL = "cql-text";
    private static final String FILTER_LANG_JSON = "cql-json";

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, FilterConfiguration.class);
    }

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-3/1.0/conf/filter",
                "http://www.opengis.net/spec/ogcapi-features-3/1.0/conf/features-filter",
                "http://www.opengis.net/spec/ogcapi-features-3/1.0/conf/simple-cql",
                "http://www.opengis.net/spec/ogcapi-features-3/1.0/conf/cql-text",
                "http://www.opengis.net/spec/ogcapi-features-3/1.0/conf/cql-json");
    }

    @Override
    public ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath) {
        if (!isEnabledForApi(apiData))
            return ImmutableSet.of();

        if (subPath.matches("^/[\\w\\-]+/items/?$")) {
            // Features
            return ImmutableSet.of("filter", "filter-lang");
        }
        if (subPath.matches("^/[\\w\\-]+/tiles/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+$")) {
            // Collection Tiles
            return ImmutableSet.of("filter", "filter-lang");
        }
        if (subPath.matches("^/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+$")) {
            // Tiles from more than one collection
            return ImmutableSet.of("filter", "filter-lang");
        }
        return ImmutableSet.of();
    }

    @Override
    public Set<String> getFilterParameters(Set<String> filterParameters, OgcApiApiDataV2 apiData) {
        if (!isEnabledForApi(apiData))
            return filterParameters;

        return new ImmutableSet.Builder<String>()
                .addAll(filterParameters)
                .add("filter")
                .build();
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiApiDataV2 datasetData) {

        if (parameters.containsKey("filter-lang") && !(FILTER_LANG_CQL.equals(parameters.get("filter-lang"))
                || FILTER_LANG_JSON.equals(parameters.get("filter-lang")))) {
            throw new BadRequestException(
                    String.format("The following value for query parameter filter-lang is rejected: %s. Valid parameter values are: %s",
                            parameters.get("filter-lang"), String.join(",", FILTER_LANG_CQL, FILTER_LANG_JSON)));
        }

        return queryBuilder;
    }


}
