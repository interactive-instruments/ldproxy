/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ldproxy.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CapabilityFeaturesHtml implements ApiBuildingBlock {

    @Inject
    CapabilityFeaturesHtml() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableFeaturesHtmlConfiguration.Builder()
            .enabled(true)
            .mapPosition(POSITION.AUTO)
            .style("DEFAULT")
            .build();
    }

}
