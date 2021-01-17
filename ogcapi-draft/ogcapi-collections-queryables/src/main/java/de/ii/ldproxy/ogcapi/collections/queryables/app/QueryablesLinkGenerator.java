/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.queryables.app;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is responsible for generating the links in the json Files.
 */
public class QueryablesLinkGenerator {

    /**
     * generates the links on the page /{apiId}/collections/{collectionId}
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<Link> generateCollectionLinks(URICustomizer uriBuilder, I18n i18n, Optional<Locale> language) {

        return ImmutableList.<Link>builder()
                .add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment("queryables")
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("http://www.opengis.net/def/rel/ogc/1.0/queryables")
                        .title(i18n.get("queryablesLink", language))
                        .build())
                .build();
    }
}
