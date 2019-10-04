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
import java.util.Optional;

public class LandingPageLinksGenerator extends DefaultLinksGenerator {

    public List<OgcApiLink> generateLandingPageLinks(URICustomizer uriBuilder, Optional<String> describeFeatureTypeUrl,
                                                     OgcApiMediaType mediaType, List<OgcApiMediaType> alternateMediaTypes) {
        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>()
                .addAll(super.generateLinks(uriBuilder, mediaType, alternateMediaTypes));

        uriBuilder
                .ensureNoTrailingSlash()
                .ensureParameter("f", mediaType.parameter());

        builder.add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment("collections")
                                .removeParameters("f")
                                .toString())
                        .rel("data")
                        .description("Access the data")
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("api")
                                        .setParameter("f", "json")
                                        .toString())
                        .rel("service-desc")
                        .type("application/vnd.oai.openapi+json;version=3.0") // TODO make configurable
                        .description("Formal definition of the API in OpenAPI 3.0")
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("api")
                                        .setParameter("f", "html")
                                        .toString())
                        .rel("service-doc")
                        .type("text/html")
                        .description("Documentation of the API")
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("conformance")
                                        .removeParameters("f")
                                        .toString())
                        .rel("conformance")
                        .description("OGC API conformance classes implemented by this server")
                        .build());

        return builder.build();
    }
}
