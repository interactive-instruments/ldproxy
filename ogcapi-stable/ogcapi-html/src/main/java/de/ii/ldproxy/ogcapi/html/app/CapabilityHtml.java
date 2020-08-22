/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.app;

import de.ii.ldproxy.ogcapi.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class CapabilityHtml implements ApiBuildingBlock {

    @Override
    public ExtensionConfiguration.Builder getConfigurationBuilder() {
        return new ImmutableHtmlConfiguration.Builder();
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableHtmlConfiguration.Builder().enabled(true)
                                                       .noIndexEnabled(true)
                                                       .schemaOrgEnabled(true)
                                                       .collectionDescriptionsInOverview(false)
                                                       .legalName("Legal notice")
                                                       .legalUrl("")
                                                       .privacyName("Privacy notice")
                                                       .privacyUrl("")
                                                       .leafletUrl("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
                                                       .leafletAttribution("&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors")
                                                       .openLayersUrl("https://{a-c}.tile.openstreetmap.org/{z}/{x}/{y}.png")
                                                       .openLayersAttribution("&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors")
                                                       .footerText("")
                                                       .build();
    }

}
