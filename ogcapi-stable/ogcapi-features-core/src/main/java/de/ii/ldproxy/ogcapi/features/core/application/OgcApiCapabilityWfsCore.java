/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.application;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiBuildingBlock;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import static de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration.DEFAULT_PAGE_SIZE;
import static de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration.MAX_PAGE_SIZE;
import static de.ii.ldproxy.ogcapi.features.core.application.OgcApiFeaturesCoreConfiguration.MINIMUM_PAGE_SIZE;

@Component
@Provides
@Instantiate
public class OgcApiCapabilityWfsCore implements OgcApiBuildingBlock {

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableOgcApiFeaturesCoreConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableOgcApiFeaturesCoreConfiguration.Builder().enabled(true)
                                                                     .defaultCrs(OgcApiFeaturesCoreConfiguration.DefaultCrs.CRS84)
                                                                     .minimumPageSize(MINIMUM_PAGE_SIZE)
                                                                     .defaultPageSize(DEFAULT_PAGE_SIZE)
                                                                     .maximumPageSize(MAX_PAGE_SIZE)
                                                                     .showsFeatureSelfLink(false)
                                                                     .build();
    }

}
