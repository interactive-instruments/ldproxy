/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;

import java.util.List;

/**
 * This class is responsible for generating the links to the styles.
 */
public class StylesLinkGenerator {

    /**
     * generates the links on the service landing page /{serviceId}?f=json
     *
     * @param uriBuilder the URI, split in host, path and query
     * @return a list with links
     */
    public List<OgcApiLink> generateLandingPageLinks(URICustomizer uriBuilder) {

        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("styles")
                                        .ensureParameter("f", "json")
                                        .toString())
                        .rel("styles")
                        .type("application/json")
                        .description("Styles to render data")
                        .build())
                .build();
    }

    /**
     * generates the links for a style on the page /{serviceId}/styles
     *
     * @param uriBuilder the URI, split in host, path and query
     * @param styleId    the ids of the styles
     * @return a list with links
     */
    public List<OgcApiLink> generateStyleLinks(URICustomizer uriBuilder, String styleId, List<OgcApiMediaType> mediaTypes) {

        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>();

        for (OgcApiMediaType mediaType: mediaTypes) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder.copy()
                            .ensureLastPathSegment(styleId)
                            .setParameter("f", mediaType.parameter())
                            .toString()
                    )
                    .rel("stylesheet")
                    .type(mediaType.type().toString())
                    .build());
        }

        builder.add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment(styleId)
                                .ensureLastPathSegment("metadata")
                                .setParameter("f", "json")
                                .toString()
                        )
                        .rel("describedBy")
                        .type("application/json")
                        .build());

        return builder.build();
    }

    public OgcApiLink generateStylesheetLink(URICustomizer uriBuilder, String styleId, OgcApiMediaType mediaType) {

        final ImmutableOgcApiLink.Builder builder = new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment(styleId)
                                .setParameter("f", mediaType.parameter())
                                .toString()
                        )
                        .rel("stylesheet")
                        .type(mediaType.type().toString());

        return builder.build();
    }
}
