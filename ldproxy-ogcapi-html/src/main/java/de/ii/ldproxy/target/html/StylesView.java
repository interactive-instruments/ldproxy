/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.StyleEntry;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.wfs3.styles.Styles;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StylesView extends LdproxyView {
    private List<StyleEntry> styleEntries;
    public String none;

    public StylesView(OgcApiDatasetData apiData,
                      Styles styles,
                      List<NavigationDTO> breadCrumbs,
                      String staticUrlPrefix,
                      HtmlConfig htmlConfig,
                      boolean noIndex,
                      URICustomizer uriCustomizer,
                      I18n i18n,
                      Optional<Locale> language) {
        super("styles.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                styles.getLinks(),
                i18n.get("stylesTitle", language),
                i18n.get("stylesDescription", language));

        // TODO this is quick and dirty - the view needs to be improved

        this.styleEntries = styles.getStyles();
        this.none = i18n.get ("none", language);
    }

    public List<StyleEntry> getStyles() {
        return styleEntries;
    }
}
