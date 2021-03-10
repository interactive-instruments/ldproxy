/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ConformanceClass;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
@Provides
@Instantiate
public class FeaturesCore implements ConformanceClass {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesCore.class);

    @Override
    public List<String> getConformanceClassUris() {
        return ImmutableList.of("http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core", "http://www.opengis.net/spec/ogcapi-records-1/0.0/conf/core");
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
