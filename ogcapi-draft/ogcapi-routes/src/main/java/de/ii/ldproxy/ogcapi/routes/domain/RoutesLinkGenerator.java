/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is responsible for generating the links to route resources.
 */
public class RoutesLinkGenerator {

    /**
     * generates the links on the landing page /{apiId}
     *
     * @param uriBuilder the URI, split in host, path and query
     * @return a list with links
     */
    public List<Link> generateLandingPageLinks(URICustomizer uriBuilder,
                                               I18n i18n,
                                               Optional<Locale> language) {

        ImmutableList.Builder<Link> builder = ImmutableList.builder();

        builder.add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureNoTrailingSlash()
                                        .ensureLastPathSegment("routes")
                                        .clearParameters()
                                        .toString())
                        .rel("http://www.opengis.net/def/rel/ogc/1.0/routes")
                        .title(i18n.get("routesLink",language))
                        .build());

        return builder.build();
    }
}
