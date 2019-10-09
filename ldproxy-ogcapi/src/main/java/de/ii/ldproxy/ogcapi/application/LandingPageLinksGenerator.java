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

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class LandingPageLinksGenerator extends DefaultLinksGenerator {

    public List<OgcApiLink> generateLinks(URICustomizer uriBuilder,
                                                     Optional<String> describeFeatureTypeUrl,
                                                     OgcApiMediaType mediaType,
                                                     List<OgcApiMediaType> alternateMediaTypes,
                                                     I18n i18n,
                                                     Optional<Locale> language) {
        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>()
                .addAll(super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

        uriBuilder
                .ensureNoTrailingSlash()
                .removeParameters("lang")
                .ensureParameter("f", mediaType.parameter());

        builder.add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment("collections")
                                .removeParameters("f")
                                .toString())
                        .rel("data")
                        .description(i18n.get("dataLink",language))
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("api")
                                        .setParameter("f", "json")
                                        .toString())
                        .rel("service-desc")
                        .type("application/vnd.oai.openapi+json;version=3.0")
                        .description(i18n.get("serviceDescLink",language))
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("api")
                                        .setParameter("f", "html")
                                        .toString())
                        .rel("service-doc")
                        .type("text/html")
                        .description(i18n.get("serviceDocLink",language))
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("conformance")
                                        .removeParameters("f")
                                        .toString())
                        .rel("conformance")
                        .description(i18n.get("conformanceLink",language))
                        .build());

        return builder.build();
    }
}
