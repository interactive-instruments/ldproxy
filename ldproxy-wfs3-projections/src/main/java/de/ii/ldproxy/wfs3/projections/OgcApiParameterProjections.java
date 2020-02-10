/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.projections;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiParameterExtension;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class OgcApiParameterProjections implements OgcApiParameterExtension {

    @Override
    public boolean isEnabledForApi(OgcApiApiDataV2 apiData) {
        return isExtensionEnabled(apiData, ProjectionsConfiguration.class);
    }

    @Override
    public ImmutableSet<String> getParameters(OgcApiApiDataV2 apiData, String subPath) {
        if (!isEnabledForApi(apiData))
            return ImmutableSet.of();

        if (subPath.matches("^/[\\w\\-]+/items/?$")) {
            // Features
            return ImmutableSet.of("properties");
        } else if (subPath.matches("^/[\\w\\-]+/items/[^/\\s]+/?$")) {
            // Feature
            return ImmutableSet.of("properties");
        }

        return ImmutableSet.of();
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiApiDataV2 datasetData) {

        if (!isExtensionEnabled(datasetData, ProjectionsConfiguration.class)) {
            return queryBuilder;
        }
        List<String> propertiesList = getPropertiesList(parameters);

        return queryBuilder.fields(propertiesList);


    }

    private List<String> getPropertiesList(Map<String, String> parameters) {
        if (parameters.containsKey("properties")) {
            return Splitter.on(',')
                           .omitEmptyStrings()
                           .trimResults()
                           .splitToList(parameters.get("properties"));
        } else {
            return ImmutableList.of("*");
        }
    }
}
