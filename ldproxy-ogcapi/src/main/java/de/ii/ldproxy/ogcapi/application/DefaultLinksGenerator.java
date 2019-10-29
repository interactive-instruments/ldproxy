/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.application;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import org.apache.http.client.utils.URIBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultLinksGenerator {

    // TODO add option to not include self/alternate links for more compact responses (note: voids compliance with OGC API)

    public List<OgcApiLink> generateLinks(URICustomizer uriBuilder,
                                          OgcApiMediaType mediaType,
                                          List<OgcApiMediaType> alternateMediaTypes,
                                          I18n i18n,
                                          Optional<Locale> language) {
        uriBuilder
                .removeParameters("lang")
                .ensureNoTrailingSlash();

        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder
                                .setParameter("f", mediaType.parameter())
                                .toString())
                        .rel("self")
                        .type(mediaType.type()
                                       .toString())
                        .title(i18n.get("selfLink",language))
                        .build())
                .addAll(alternateMediaTypes.stream()
                                             .map(generateAlternateLink(uriBuilder.copy(), i18n, language))
                                             .collect(Collectors.toList()));

        return builder.build();
    }

    private Function<OgcApiMediaType, OgcApiLink> generateAlternateLink(final URIBuilder uriBuilder, I18n i18n, Optional<Locale> language) {
        return mediaType -> new ImmutableOgcApiLink.Builder()
                .href(uriBuilder
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("alternate")
                .type(mediaType
                        .type()
                        .toString())
                .title(i18n.get("alternateLink",language)+" "+mediaType.label())
                .build();
    }
}
