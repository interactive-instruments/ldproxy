/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ApiMediaType;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableLink;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    public List<Link> generateLandingPageLinks(URICustomizer uriBuilder,
                                               Optional<String> defaultStyle,
                                               I18n i18n,
                                               Optional<Locale> language) {

        ImmutableList.Builder<Link> builder = ImmutableList.<Link>builder();

        builder.add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureNoTrailingSlash()
                                        .ensureLastPathSegment("styles")
                                        .removeParameters("f")
                                        .toString())
                        .rel("styles")
                        .title(i18n.get("stylesLink",language))
                        .build());

        if (defaultStyle.isPresent())
            builder.add(new ImmutableLink.Builder()
                                .href(uriBuilder.copy()
                                             .ensureNoTrailingSlash()
                                             .ensureLastPathSegments("styles", defaultStyle.get())
                                             .setParameter("f", "html")
                                             .toString())
                                .rel("ldp-map")
                                .title(i18n.get("webmapLink",language))
                                .build());

        return builder.build();
    }

    /**
     * generates the links for a style on the page /{serviceId}/styles
     *
     * @param uriBuilder the URI, split in host, path and query
     * @param styleId    the ids of the styles
     * @return a list with links
     */
    public List<Link> generateStyleLinks(URICustomizer uriBuilder,
                                         String styleId,
                                         List<ApiMediaType> mediaTypes,
                                         I18n i18n,
                                         Optional<Locale> language) {

        final ImmutableList.Builder<Link> builder = new ImmutableList.Builder<Link>();

        for (ApiMediaType mediaType: mediaTypes) {
            builder.add(new ImmutableLink.Builder()
                    .href(uriBuilder.copy()
                            .ensureNoTrailingSlash()
                            .ensureLastPathSegment(styleId)
                            .setParameter("f", mediaType.parameter())
                            .toString()
                    )
                    .rel("stylesheet")
                    .type(mediaType.type().toString())
                    .title(mediaType.label().equals("HTML") ?
                                   i18n.get("stylesheetLinkMap",language) :
                                   i18n.get("stylesheetLink",language).replace("{{format}}", mediaType.label()))
                    .build());
        }

        builder.add(new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegments("styles", styleId, "metadata")
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("describedby")
                        .title(i18n.get("styleMetadataLink",language))
                        .build());

        return builder.build();
    }

    public Link generateStylesheetLink(URICustomizer uriBuilder,
                                       String styleId,
                                       ApiMediaType mediaType,
                                       I18n i18n,
                                       Optional<Locale> language) {

        final ImmutableLink.Builder builder = new ImmutableLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment(styleId)
                                .setParameter("f", mediaType.parameter())
                                .toString()
                        )
                        .rel("stylesheet")
                        .title(i18n.get("stylesheetLink",language).replace("{{format}}", mediaType.label()))
                        .type(mediaType.type().toString());

        return builder.build();
    }
}
