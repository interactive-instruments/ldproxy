/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * @author zahnen
 */
public class ServiceOverviewView extends DatasetView {
    public URI uri;
    public ServiceOverviewView(URI uri, Object data, String urlPrefix, HtmlConfig htmlConfig, I18n i18n, Optional<Locale> language) {
        super("services", uri, data, urlPrefix, htmlConfig, htmlConfig.isNoIndex());
        this.uri = uri;
        this.title = i18n.get("rootTitle", language);
        this.description = i18n.get("rootDescription", language);
        this.keywords = new ImmutableList.Builder<String>().add("ldproxy", "OGC API").build();
        this.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO(i18n.get("root", language), true))
                .build();
    }

    public String getCanonicalUrl() {
        return new URICustomizer(uri).clearParameters().ensureTrailingSlash().toString();
    }
}
