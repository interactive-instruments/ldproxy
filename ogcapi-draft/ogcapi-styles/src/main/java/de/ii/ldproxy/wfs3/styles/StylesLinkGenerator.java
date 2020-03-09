/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import javax.ws.rs.core.MediaType;
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
    public List<OgcApiLink> generateLandingPageLinks(URICustomizer uriBuilder,
                                          I18n i18n,
                                          Optional<Locale> language) {

        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("styles")
                                        .removeParameters("f")
                                        .toString())
                        .rel("styles")
                        .title(i18n.get("stylesLink",language))
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
    public List<OgcApiLink> generateStyleLinks(URICustomizer uriBuilder,
                                               String styleId,
                                               List<OgcApiMediaType> mediaTypes,
                                               boolean maps,
                                               I18n i18n,
                                               Optional<Locale> language) {

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
                    .title(i18n.get("stylesheetLink",language).replace("{{format}}", mediaType.label()))
                    .build());
        }

        builder.add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegments("styles", styleId, "metadata")
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("describedBy")
                        .title(i18n.get("styleMetadataLink",language))
                        .build());

        if (maps && mediaTypes.stream()
                              .filter(mediaType -> mediaType.matches(new MediaType("application","vnd.mapbox.style+json")))
                              .count() > 0) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder.copy()
                            .ensureLastPathSegments("styles", styleId, "map")
                            .removeParameters("f")
                            .toString()
                    )
                    .rel("map")
                    .title(i18n.get("styleMapLink",language))
                    .build());
        }

        return builder.build();
    }

    public OgcApiLink generateStylesheetLink(URICustomizer uriBuilder,
                                             String styleId,
                                             OgcApiMediaType mediaType,
                                             I18n i18n,
                                             Optional<Locale> language) {

        final ImmutableOgcApiLink.Builder builder = new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
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
