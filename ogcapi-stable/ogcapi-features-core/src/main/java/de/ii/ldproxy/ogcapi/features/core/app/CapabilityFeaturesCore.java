/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.core.app;

import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ldproxy.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.DEFAULT_PAGE_SIZE;
import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.MAX_PAGE_SIZE;
import static de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration.MINIMUM_PAGE_SIZE;

@Component
@Provides
@Instantiate
public class CapabilityFeaturesCore implements ApiBuildingBlock {

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableFeaturesCoreConfiguration.Builder().enabled(true)
                                                               .itemType(FeaturesCoreConfiguration.ItemType.feature)
                                                               .defaultCrs(FeaturesCoreConfiguration.DefaultCrs.CRS84)
                                                               .minimumPageSize(MINIMUM_PAGE_SIZE)
                                                               .defaultPageSize(DEFAULT_PAGE_SIZE)
                                                               .maximumPageSize(MAX_PAGE_SIZE)
                                                               .supportPostOnItems(false)
                                                               .showsFeatureSelfLink(false)
                                                               .build();
    }

}
