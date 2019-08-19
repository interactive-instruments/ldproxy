/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;

import java.util.List;
/**
 * This class is responsible for generating the links to the styles.
 *
 */
public class StylesLinkGenerator {

    /**
     * generates the links on the service landing page /{serviceId}?f=json
     *
     * @param uriBuilder the URI, split in host, path and query
     * @return a list with links
     */
    public List<Wfs3Link> generateLandingPageLinks(URICustomizer uriBuilder) {

        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>();
        uriBuilder.ensureParameter("f", "json");

        builder.add(ImmutableWfs3Link.builder()
                .href(uriBuilder.copy()
                        .ensureLastPathSegment("styles")
                        .setParameter("f", "json")
                        .toString())
                .rel("styles")
                .type("application/json")
                .description("the list of available styles")
                .build());

        // TODO: add text/html link, if enabled

        return builder.build();
    }

    /**
     * generates the links for a style on the page /{serviceId}/styles
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param styleId        the id of the style
     * @return a list with links to
     */
    public List<Wfs3Link> generateStyleLinks(URICustomizer uriBuilder, String styleId, List<String> mediaTypes) {

        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>();

        for (String mediaType: mediaTypes) {
            builder.add(ImmutableWfs3Link.builder()
                    .href(uriBuilder.copy()
                            .ensureLastPathSegment(styleId)
                            .setParameter("f", Wfs3MediaTypes.FORMATS.get(mediaType))
                            .toString()
                    )
                    .rel("stylesheet")
                    .type(mediaType)
                    .build());
        }

        builder.add(ImmutableWfs3Link.builder()
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

    public Wfs3Link generateStylesheetLink(URICustomizer uriBuilder, String styleId, String mediaType) {

        final ImmutableWfs3Link.Builder builder = ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment(styleId)
                                .setParameter("f", Wfs3MediaTypes.FORMATS.get(mediaType))
                                .toString()
                        )
                        .rel("stylesheet")
                        .type(mediaType);

        return builder.build();
    }
}
