/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.DEFAULT_PAGE_SIZE;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.MAX_PAGE_SIZE;
import static de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration.MINIMUM_PAGE_SIZE;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.ImmutableFeaturesCoreConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CapabilityFeaturesCore implements ApiBuildingBlock {

    @Inject
    CapabilityFeaturesCore() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableFeaturesCoreConfiguration.Builder().enabled(true)
                                                               .itemType(FeaturesCoreConfiguration.ItemType.feature)
                                                               .defaultCrs(FeaturesCoreConfiguration.DefaultCrs.CRS84)
                                                               .minimumPageSize(MINIMUM_PAGE_SIZE)
                                                               .defaultPageSize(DEFAULT_PAGE_SIZE)
                                                               .maximumPageSize(MAX_PAGE_SIZE)
                                                               .showsFeatureSelfLink(false)
                                                               .build();
    }

}
