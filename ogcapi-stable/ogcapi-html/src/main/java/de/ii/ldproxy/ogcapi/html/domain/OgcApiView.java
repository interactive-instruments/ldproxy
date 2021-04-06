/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.Link;
import io.dropwizard.views.View;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class OgcApiView extends View {

    protected final OgcApiDataV2 apiData;
    protected final List<NavigationDTO> breadCrumbs;
    protected final List<Link> links;
    protected final HtmlConfiguration htmlConfig;
    protected final String urlPrefix;
    protected final String title;
    protected final String description;
    protected final boolean noIndex;

    protected OgcApiView(String templateName, @Nullable Charset charset, @Nullable OgcApiDataV2 apiData,
                         List<NavigationDTO> breadCrumbs, HtmlConfiguration htmlConfig, boolean noIndex, String urlPrefix,
                         @Nullable List<Link> links, @Nullable String title, @Nullable String description) {
        super(String.format("/templates/%s", templateName), charset);
        this.apiData = apiData;
        this.breadCrumbs = Objects.requireNonNullElse(breadCrumbs, ImmutableList.of());
        this.links = Objects.requireNonNullElse(links, ImmutableList.of());
        this.htmlConfig = htmlConfig;
        this.noIndex = noIndex;
        this.urlPrefix = urlPrefix;
        this.title = title;
        this.description = description;
    }

    public List<NavigationDTO> getFormats() {
        return links.stream()
                .filter(link -> Objects.equals(link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                .collect(Collectors.toList());
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public boolean hasBreadCrumbs() { return breadCrumbs.size() > 1; }

    public String getBreadCrumbsList() {
        String result = "";
        for (int i=0; i<breadCrumbs.size(); i++) {
            NavigationDTO item = breadCrumbs.get(i);
            result += "{ \"@type\": \"ListItem\", \"position\": "+ (i+1) +", \"name\": \""+ item.label +"\"";
            if (Objects.nonNull(item.url))
                result += ", \"item\": \""+ item.url +"\"";
            result += " }";
            if (i<breadCrumbs.size()-1)
                result += ",\n    ";
        }
        return result;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
