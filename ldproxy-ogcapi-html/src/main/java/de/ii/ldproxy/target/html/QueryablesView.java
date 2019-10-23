/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.collection.queryables.Queryable;
import de.ii.ldproxy.ogcapi.collection.queryables.Queryables;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import io.dropwizard.views.View;

import java.util.*;
import java.util.stream.Collectors;

public class QueryablesView extends View {
    private final OgcApiDatasetData apiData;
    private final List<NavigationDTO> breadCrumbs;
    public final HtmlConfig htmlConfig;
    public List<Queryable> queryables;
    public List<OgcApiLink> links;
    public String urlPrefix;
    public String title;
    public String description;
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
        super("queryables.mustache", Charsets.UTF_8);

        // TODO this is quick and dirty - the view needs to be improved

        this.queryables = queryables.getQueryables();
        this.links = queryables.getLinks();
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = staticUrlPrefix;
        this.htmlConfig = htmlConfig;

        this.title = i18n.get("queryablesTitle", language);
        this.description = i18n.get("queryablesDescription", language);
        this.typeTitle = i18n.get("typeTitle", language);
        this.none = i18n.get ("none", language);

        this.apiData = apiData;
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        links.stream()
            .filter(link -> Objects.equals(link.getRel(), "alternate"))
            .sorted(Comparator.comparing(link -> link.getTypeLabel()
                    .toUpperCase()))
            .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
            .collect(Collectors.toList());

        return ImmutableList.of();
    }
}
