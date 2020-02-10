/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.filter;

import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiParameterExtension;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.ws.rs.BadRequestException;
import java.util.Map;
import java.util.Set;

@Component
@Provides
@Instantiate
public class OgcApiParameterFilter implements OgcApiParameterExtension {

    private static final String FILTER_LANG_CQL = "cql-text";

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, FilterConfiguration.class);
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

        if (parameters.containsKey("filter-lang") && !FILTER_LANG_CQL.equals(parameters.get("filter-lang"))) {
            throw new BadRequestException(
                    String.format("The following value for query parameter filter-lang is rejected: %s. Valid parameter value is: %s",
                            parameters.get("filter-lang"), FILTER_LANG_CQL));
        }

        return queryBuilder;
    }


}
