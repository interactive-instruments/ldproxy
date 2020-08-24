/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.html.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.DatasetView;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.services.domain.ServiceData;

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

    public ServiceOverviewView(URI uri, List<ServiceData> data, String urlPrefix, HtmlConfiguration htmlConfig, I18n i18n, Optional<Locale> language) {
        super("services", uri, data, urlPrefix, htmlConfig, Objects.equals(htmlConfig.getNoIndexEnabled(), true));
        this.uri = uri;
        this.title = Objects.requireNonNullElse(htmlConfig.getApiCatalogLabel(), i18n.get("rootTitle", language));
        this.description = Objects.requireNonNullElse(htmlConfig.getApiCatalogDescription(), i18n.get("rootDescription", language));
        this.keywords = new ImmutableList.Builder<String>().add("ldproxy", "OGC API").build();
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(i18n.get("root", language), true))
                .build();
    }

    public String getCanonicalUrl() {
        return new URICustomizer(uri).clearParameters().ensureTrailingSlash().toString();
    }

    public String getDatasetsAsString() {
        return ((List<ServiceData>)getData())
                .stream()
                .map(api -> "{ \"@type\": \"Dataset\", \"name\": \"" +
                        api.getLabel() +
                        "\", \"description\": \"" +
                        api.getDescription() +
                        "\", \"url\": \"" +
                        getCanonicalUrl() + api.getId() +
                        "\", \"sameAs\": \"" +
                        getCanonicalUrl() + api.getId() +
                        "\" }")
                .collect(Collectors.joining(", "));
    }
}
