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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class LandingPageLinksGenerator {

    public List<OgcApiLink> generateLandingPageLinks(URICustomizer uriBuilder, Optional<String> describeFeatureTypeUrl,
                                                     OgcApiMediaType mediaType, List<OgcApiMediaType> alternateMediaTypes) {
        uriBuilder
                .ensureParameter("f", mediaType.parameter());

        final boolean isCollections = uriBuilder.isLastPathSegment("collections");
        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.toString())
                        .rel("self")
                        .type(mediaType.type()
                                       .toString())
                        .description("this document")
                        .build())
                .addAll(alternateMediaTypes.stream()
                                             .map(generateAlternateLink(uriBuilder.copy(), true))
                                             .collect(Collectors.toList()))
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegment("collections")
                                        .ensureLastPathSegment("api")
                                        .setParameter("f", "json")
                                        .toString())
                        .rel("service-desc")
                        .type("application/vnd.oai.openapi+json;version=3.0") // TODO: configurable
                        .description("the OpenAPI definition")
                        .typeLabel("JSON")
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegment("collections")
                                        .ensureLastPathSegment("api")
                                        .setParameter("f", "html")
                                        .toString())
                        .rel("service-doc")
                        .type("text/html")
                        .description("the OpenAPI definition")
                        .typeLabel("HTML")
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegment("collections")
                                        .ensureLastPathSegment("conformance")
                                        .setParameter("f", "json")
                                        .toString())
                        .rel("conformance")
                        .type("application/json")
                        .description("Conformance classes implemented by this server")
                        .build());
        if (!isCollections) {
            builder
                    .add(new ImmutableOgcApiLink.Builder()
                            .href(uriBuilder.copy()
                                            .ensureLastPathSegment("collections")
                                            .setParameter("f", "json")
                                            .toString())
                            .rel("data")
                            .type("application/json")
                            .description("Metadata about the feature collections")
                            .build());
        }

        if (describeFeatureTypeUrl.isPresent()) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(describeFeatureTypeUrl.get())
                    .rel("describedBy")
                    .type("application/xml")
                    .description("XML schema for all feature types")
                    .build());
        }

        return builder.build();
    }

    private Function<OgcApiMediaType, OgcApiLink> generateAlternateLink(final URIBuilder uriBuilder, boolean isMetadata) {
        return mediaType -> new ImmutableOgcApiLink.Builder()
                .href(uriBuilder
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("alternate")
                .type(isMetadata ? mediaType.type() // TODO: check
                                            .toString() : mediaType.type()
                                                                   .toString())
                .description("this document")
                .typeLabel(isMetadata ? mediaType.label() : mediaType.label()) // TODO: check
                .build();
    }
}
