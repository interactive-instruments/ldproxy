/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.google.common.base.Charsets;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.styles.domain.StyleEntry;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;
import de.ii.ogcapi.styles.domain.Styles;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class StylesView extends OgcApiView {
    private final List<StyleEntry> styleEntries;
    public String none;

    public StylesView(OgcApiDataV2 apiData,
                      Styles styles,
                      List<NavigationDTO> breadCrumbs,
                      String staticUrlPrefix,
                      HtmlConfiguration htmlConfig,
                      boolean noIndex,
                      URICustomizer uriCustomizer,
                      I18n i18n,
                      Optional<Locale> language) {
        super("styles.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                styles.getLinks(),
                i18n.get("stylesTitle", language),
                i18n.get("stylesDescription", language));

        this.styleEntries = styles.getStyles();
        this.none = i18n.get ("none", language);
    }

    public List<StyleEntry> getStyles() {
        return styleEntries;
    }
}
