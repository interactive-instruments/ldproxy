/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.features.html.app;

import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.FeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CapabilityFeaturesHtml implements ApiBuildingBlock {

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableFeaturesHtmlConfiguration.Builder().enabled(true)
                                                       .schemaOrgEnabled(true)
                                                       .layout(FeaturesHtmlConfiguration.LAYOUT.CLASSIC)
                                                       .build();
    }

}
