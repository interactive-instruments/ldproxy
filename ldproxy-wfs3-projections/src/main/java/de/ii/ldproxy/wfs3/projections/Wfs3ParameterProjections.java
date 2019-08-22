/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.projections;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.wfs3.api.Wfs3ParameterExtension;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;
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
public class Wfs3ParameterProjections implements Wfs3ParameterExtension {

    @Override
    public boolean isEnabledForDataset(OgcApiDatasetData dataset) {
        return isExtensionEnabled(dataset, ProjectionsConfiguration.class);
    }

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationOgcApi featureTypeConfiguration,
                                                        ImmutableFeatureQuery.Builder queryBuilder,
                                                        Map<String, String> parameters, OgcApiDatasetData datasetData) {

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
