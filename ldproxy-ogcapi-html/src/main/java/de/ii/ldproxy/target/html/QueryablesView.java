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
import de.ii.ldproxy.ogcapi.collection.queryables.Queryable;
import de.ii.ldproxy.ogcapi.collection.queryables.Queryables;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class QueryablesView extends LdproxyView {
    public List<Queryable> queryables;
    public String typeTitle;
    public String none;

    public QueryablesView(OgcApiDatasetData apiData,
                          Queryables queryables,
                          List<NavigationDTO> breadCrumbs,
                          String staticUrlPrefix,
                          HtmlConfig htmlConfig,
                          URICustomizer uriCustomizer,
                          I18n i18n,
                          Optional<Locale> language) {
        super("queryables.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, staticUrlPrefix,
                queryables.getLinks(),
                i18n.get("queryablesTitle", language),
                i18n.get("queryablesDescription", language));

        this.queryables = queryables.getQueryables();
        this.typeTitle = i18n.get("typeTitle", language);
        this.none = i18n.get ("none", language);
    }
}
