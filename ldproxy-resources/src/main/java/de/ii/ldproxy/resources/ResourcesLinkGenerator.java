/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.resources;

import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;

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
                                .ensureLastPathSegment(resourceId)
                                .toString()
                        )
                        .rel("item");

        return builder.build();
    }
}
