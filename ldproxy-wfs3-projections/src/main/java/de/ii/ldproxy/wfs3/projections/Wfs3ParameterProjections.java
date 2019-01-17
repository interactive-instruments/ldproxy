/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.projections;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.FeatureTypeConfigurationWfs3;
import de.ii.ldproxy.wfs3.api.Wfs3ParameterExtension;
import de.ii.ldproxy.wfs3.api.Wfs3ServiceData;
import de.ii.xtraplatform.feature.query.api.ImmutableFeatureQuery;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.util.List;
import java.util.Map;

import static de.ii.ldproxy.wfs3.projections.ProjectionsConfiguration.EXTENSION_KEY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class Wfs3ParameterProjections implements Wfs3ParameterExtension {

    @Override
    public ImmutableFeatureQuery.Builder transformQuery(FeatureTypeConfigurationWfs3 featureTypeConfigurationWfs3, ImmutableFeatureQuery.Builder queryBuilder, Map<String, String> parameters, Wfs3ServiceData serviceData) {

        if(!isExtensionEnabled(serviceData,EXTENSION_KEY)) {
            return queryBuilder;
        }
        List<String> propertiesList = getPropertiesList(parameters);

        return queryBuilder.fields(propertiesList);


    }

    private List<String> getPropertiesList(Map<String, String> parameters) {
        if (parameters.containsKey("properties")) {
            return Splitter.on(',').omitEmptyStrings().trimResults().splitToList(parameters.get("properties"));
        } else {
            return ImmutableList.of("*");
        }
    }
}
