/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.resources.Resource;
import de.ii.ldproxy.resources.Resources;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ResourcesView extends LdproxyView {
    private List<Resource> resourceList;
    public String none;

    public ResourcesView(OgcApiApiDataV2 apiData,
                         Resources resources,
                         List<NavigationDTO> breadCrumbs,
                         String staticUrlPrefix,
                         HtmlConfig htmlConfig,
                         boolean noIndex,
                         URICustomizer uriCustomizer,
                         I18n i18n,
                         Optional<Locale> language) {
        super("resources.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                resources.getLinks(),
                i18n.get("resourcesTitle", language),
                i18n.get("resourcesDescription", language));

        this.resourceList = resources.getResources();
        this.none = i18n.get ("none", language);
    }

    public List<Resource> getResources() {
        return resourceList;
    }
}
