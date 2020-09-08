/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiCatalog;
import de.ii.ldproxy.ogcapi.domain.ApiCatalogEntry;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.html.domain.DatasetView;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class ServiceOverviewView extends DatasetView {
    public URI uri;
    public boolean isApiCatalog = true;
    public String canonicalUrl;

    public ServiceOverviewView(URI uri, ApiCatalog apiCatalog, HtmlConfiguration htmlConfig, I18n i18n, Optional<Locale> language) {
        super("services", uri, apiCatalog.getApis(), apiCatalog.getUrlPrefix(), htmlConfig, Objects.equals(htmlConfig.getNoIndexEnabled(), true));
        this.uri = uri;
        this.title = apiCatalog.getTitle().orElse(i18n.get("rootTitle", language));
        this.description = apiCatalog.getDescription().orElse(i18n.get("rootDescription", language));
        this.canonicalUrl = apiCatalog.getCatalogUri().toString();
        this.keywords = new ImmutableList.Builder<String>().add("ldproxy", "OGC API").build();
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(i18n.get("root", language), true))
                .build();
    }

    public String getDatasetsAsString() {
        return ((List<ApiCatalogEntry>)getData())
                .stream()
                .map(api -> "{ \"@type\": \"Dataset\", \"name\": \"" +
                        api.getTitle().orElse(api.getId()).replace("\"", "\\\"") +
                        "\", \"description\": \"" +
                        api.getDescription().orElse("").replace("\"", "\\\"") +
                        "\", \"url\": \"" +
                        api.getLandingPageUri() +
                        "\", \"sameAs\": \"" +
                        api.getLandingPageUri() +
                        "\" }\n")
                .collect(Collectors.joining(", "));
    }
}
