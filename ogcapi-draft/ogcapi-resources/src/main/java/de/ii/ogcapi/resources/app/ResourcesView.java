/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.foundation.domain.I18n;
import de.ii.ldproxy.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.foundation.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ResourcesView extends OgcApiView {
    private List<Resource> resourceList;
    public String none;

    public ResourcesView(OgcApiDataV2 apiData,
                         Resources resources,
                         List<NavigationDTO> breadCrumbs,
                         String staticUrlPrefix,
                         HtmlConfiguration htmlConfig,
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
