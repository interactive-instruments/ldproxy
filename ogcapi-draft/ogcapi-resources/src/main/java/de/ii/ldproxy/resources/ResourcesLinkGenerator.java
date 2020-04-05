/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is responsible for generating the links to the resources.
 */
public class ResourcesLinkGenerator {

    /**
     * generates the links for a resource on the page /{serviceId}/resources
     *
     * @param uriBuilder the URI, split in host, path and query
     * @param resourceId    the ids of the styles
     * @return the link
     */
    public OgcApiLink generateResourceLink(URICustomizer uriBuilder, String resourceId) {

        final ImmutableOgcApiLink.Builder builder = new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .removeParameters("f")
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment(resourceId)
                                .toString()
                        )
                        .title(resourceId)
                        .rel("item");

        return builder.build();
    }

    /**
     * generates the links on the service landing page /{serviceId}?f=json
     *
     * @param uriBuilder the URI, split in host, path and query
     * @return a list with links
     */
    public List<OgcApiLink> generateLandingPageLinks(URICustomizer uriBuilder,
                                                     I18n i18n,
                                                     Optional<Locale> language) {

        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment("resources")
                                .removeParameters("f")
                                .toString())
                        .rel("resources")
                        .title(i18n.get("resourcesLink",language))
                        .build())
                .build();
    }


}
