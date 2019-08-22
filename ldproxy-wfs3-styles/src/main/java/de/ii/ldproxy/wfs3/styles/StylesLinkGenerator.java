/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableWfs3Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;

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
    public List<Wfs3Link> generateLandingPageLinks(URICustomizer uriBuilder) {

        return ImmutableList.<Wfs3Link>builder()
                .add(new ImmutableWfs3Link.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("styles")
                                        .ensureParameter("f", "json")
                                        .toString())
                        .rel("styles")
                        .type("application/json")
                        .description("the list of available styles")
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
    public List<Wfs3Link> generateStyleLinks(URICustomizer uriBuilder, String styleId, List<OgcApiMediaType> mediaTypes) {

        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>();

        for (OgcApiMediaType mediaType: mediaTypes) {
            builder.add(new ImmutableWfs3Link.Builder()
                    .href(uriBuilder.copy()
                            .ensureLastPathSegment(styleId)
                            .setParameter("f", mediaType.parameter())
                            .toString()
                    )
                    .rel("stylesheet")
                    .type(mediaType.main().toString())
                    .build());
        }

        builder.add(new ImmutableWfs3Link.Builder()
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

    public Wfs3Link generateStylesheetLink(URICustomizer uriBuilder, String styleId, OgcApiMediaType mediaType) {

        final ImmutableWfs3Link.Builder builder = new ImmutableWfs3Link.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment(styleId)
                                .setParameter("f", mediaType.parameter())
                                .toString()
                        )
                        .rel("stylesheet")
                        .type(mediaType.main().toString());

        return builder.build();
    }
}
