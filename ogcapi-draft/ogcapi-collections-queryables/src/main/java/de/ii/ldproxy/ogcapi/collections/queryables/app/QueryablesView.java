/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables.app;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.collections.queryables.domain.Queryable;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class QueryablesView extends OgcApiView {
    public List<Queryable> queryables;
    public String typeTitle;
    public String none;
    // sum must be 12 for bootstrap
    public Integer idCols = 3;
    public Integer descCols = 9;

    public QueryablesView(OgcApiDataV2 apiData,
                          Queryables queryables,
                          List<NavigationDTO> breadCrumbs,
                          String staticUrlPrefix,
                          HtmlConfiguration htmlConfig,
                          boolean noIndex,
                          URICustomizer uriCustomizer,
                          I18n i18n,
                          Optional<Locale> language) {
        super("queryables.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, staticUrlPrefix,
                queryables.getLinks(),
                i18n.get("queryablesTitle", language),
                i18n.get("queryablesDescription", language));

        this.queryables = queryables.getQueryables();
        Integer maxIdLength = this.queryables
                .stream()
                .map(queryable -> queryable.getId())
                .filter(id -> Objects.nonNull(id))
                .mapToInt(id -> id.length())
                .max()
                .orElse(0);
        idCols = Math.min(Math.max(2, 1 + maxIdLength/10),6);
        descCols = 12 - idCols;
        this.typeTitle = i18n.get("typeTitle", language);
        this.none = i18n.get ("none", language);
    }
}
