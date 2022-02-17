/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ldproxy.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ldproxy.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableHtmlConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CapabilityHtml implements ApiBuildingBlock {

    @Inject
    CapabilityHtml() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {
        return new ImmutableHtmlConfiguration.Builder().enabled(true)
                                                       .noIndexEnabled(true)
                                                       .schemaOrgEnabled(true)
                                                       .collectionDescriptionsInOverview(false)
                                                       .sendEtags(false)
                                                       .legalName("Legal notice")
                                                       .legalUrl("")
                                                       .privacyName("Privacy notice")
                                                       .privacyUrl("")
                                                       .basemapUrl("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png")
                                                       .basemapAttribution("&copy; <a href=\"http://osm.org/copyright\">OpenStreetMap</a> contributors")
                                                       .defaultStyle("NONE")
                                                       .footerText("")
                                                       .build();
    }

}
