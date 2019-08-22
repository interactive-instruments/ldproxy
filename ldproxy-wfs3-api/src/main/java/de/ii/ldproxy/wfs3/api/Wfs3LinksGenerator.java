/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.ImmutableWfs3Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.domain.Wfs3Link;
import org.apache.http.client.utils.URIBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
public class Wfs3LinksGenerator {

    public List<Wfs3Link> generateDatasetLinks(URICustomizer uriBuilder, Optional<String> describeFeatureTypeUrl,
                                               OgcApiMediaType mediaType, OgcApiMediaType... alternativeMediaTypes) {
        uriBuilder
                .ensureParameter("f", mediaType.parameter());

        final boolean isCollections = uriBuilder.isLastPathSegment("collections");
        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(new ImmutableWfs3Link.Builder()
                        .href(uriBuilder.toString())
                        .rel("self")
                        .type(mediaType.metadata()
                                       .toString())
                        .description("this document")
                        .build())
                .addAll(Arrays.stream(alternativeMediaTypes)
                              .map(generateAlternateLink(uriBuilder.copy(), true))
                              .collect(Collectors.toList()))
                .add(new ImmutableWfs3Link.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegment("collections")
                                        .ensureLastPathSegment("api")
                                        .setParameter("f", "json")
                                        .toString())
                        .rel("service-desc")
                        .type("application/vnd.oai.openapi+json;version=3.0")
                        .description("the OpenAPI definition")
                        .typeLabel("JSON")
                        .build())
                .add(new ImmutableWfs3Link.Builder()
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
                .add(new ImmutableWfs3Link.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegment("collections")
                                        .ensureLastPathSegment("conformance")
                                        .setParameter("f", "json")
                                        .toString())
                        .rel("conformance")
                        .type("application/json")
                        .description("WFS 3.0 conformance classes implemented by this server")
                        .build());
        if (!isCollections) {
            builder
                    .add(new ImmutableWfs3Link.Builder()
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
            builder.add(new ImmutableWfs3Link.Builder()
                    .href(describeFeatureTypeUrl.get())
                    .rel("describedBy")
                    .type("application/xml")
                    .description("XML schema for all feature types")
                    .build());
        }

        return builder.build();
    }

    public List<Wfs3Link> generateDatasetCollectionLinks(URICustomizer uriBuilder, String collectionId,
                                                         String collectionName, Optional<String> describeFeatureTypeUrl,
                                                         OgcApiMediaType mediaType,
                                                         List<OgcApiMediaType> alternativeMediaTypes) {
        boolean isCollection = false;

        if (uriBuilder.getLastPathSegment()
                      .equals("collections"))
            isCollection = true;

        uriBuilder
                .ensureParameter("f", mediaType.parameter())
                .ensureLastPathSegments("collections", collectionId, "items");

        ImmutableList.Builder<Wfs3Link> links = new ImmutableList.Builder<Wfs3Link>()
                .addAll(Stream.concat(Stream.of(mediaType), alternativeMediaTypes.stream())
                              .map(generateItemsLink(uriBuilder.copy(), collectionName))
                              .collect(Collectors.toList()))
                .addAll(Stream.concat(Stream.of(mediaType), alternativeMediaTypes.stream())
                              .map(generateCollectionsLink(uriBuilder.copy(), collectionName, collectionId, isCollection))
                              .collect(Collectors.toList()));

        describeFeatureTypeUrl.ifPresent(url -> links.add(new ImmutableWfs3Link.Builder()
                .href(describeFeatureTypeUrl.get())
                .rel("describedBy")
                .type("application/xml")
                .description("XML schema for feature type " + collectionName)
                .build()));

        return links.build();
    }


    public List<Wfs3Link> generateCollectionOrFeatureLinks(URICustomizer uriBuilder, boolean isFeatureCollection,
                                                           int page, int count, OgcApiMediaType mediaType,
                                                           List<OgcApiMediaType> alternativeMediaTypes) {
        uriBuilder
                .ensureParameter("f", mediaType.parameter());

        final ImmutableList.Builder<Wfs3Link> links = new ImmutableList.Builder<Wfs3Link>()
                .add(new ImmutableWfs3Link.Builder()
                        .href(uriBuilder.toString())
                        .rel("self")
                        .type(mediaType.main()
                                       .toString())
                        .description("this document")
                        .build())
                .addAll(alternativeMediaTypes.stream()
                                             .map(generateAlternateLink(uriBuilder.copy(), false))
                                             .collect(Collectors.toList()));

        if (isFeatureCollection) {
            links.add(new ImmutableWfs3Link.Builder()
                    .href(getUrlWithPageAndCount(uriBuilder.copy(), page + 1, count))
                    .rel("next")
                    .type(mediaType.main()
                                   .toString())
                    .description("next page")
                    .build());
            if (page > 1) {
                links.add(new ImmutableWfs3Link.Builder()
                        .href(getUrlWithPageAndCount(uriBuilder.copy(), page - 1, count))
                        .rel("prev")
                        .type(mediaType.main()
                                       .toString())
                        .description("previous page")
                        .build());
            }
        } else {
            links.add(new ImmutableWfs3Link.Builder()
                    .href(uriBuilder.copy()
                                    .removeLastPathSegments(2)
                                    .clearParameters()
                                    .setParameter("f", mediaType.parameter())
                                    .toString())
                    .rel("collection")
                    .type(mediaType.metadata()
                                   .toString())
                    .description("the collection document")
                    .build());
        }

        return links.build();
    }

    public List<Wfs3Link> generateAlternateLinks(final URICustomizer uriBuilder, boolean isMetadata,
                                                 List<OgcApiMediaType> alternativeMediaTypes) {
        return alternativeMediaTypes.stream()
                                    .map(generateAlternateLink(uriBuilder.copy(), isMetadata))
                                    .collect(Collectors.toList());
    }

    private Function<OgcApiMediaType, Wfs3Link> generateAlternateLink(final URIBuilder uriBuilder, boolean isMetadata) {
        return mediaType -> new ImmutableWfs3Link.Builder()
                .href(uriBuilder
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("alternate")
                .type(isMetadata ? mediaType.metadata()
                                            .toString() : mediaType.main()
                                                                   .toString())
                .description("this document")
                .typeLabel(isMetadata ? mediaType.metadataLabel() : mediaType.label())
                .build();
    }

    private Function<OgcApiMediaType, Wfs3Link> generateItemsLink(final URIBuilder uriBuilder,
                                                                  final String collectionName) {
        return mediaType -> new ImmutableWfs3Link.Builder()
                .href(uriBuilder
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("item")
                .type(mediaType.main()
                               .toString())
                .description(collectionName)
                .typeLabel(mediaType.label())
                .build();
    }

    private Function<OgcApiMediaType, Wfs3Link> generateCollectionsLink(final URICustomizer uriBuilder,
                                                                        final String collectionName,
                                                                        final String collectionId,
                                                                        boolean isCollection) {


        return mediaType -> {
            String rel = "";
            String type = "";
            String media = mediaType.parameter();
            switch (media) {
                case "json":
                    rel = "self";
                    type = "application/json";
                    break;
                case "geo+json":
                    rel = "self";
                    type = "application/geo+json";
                    break;
                case "html":
                    rel = "alternate";
                    type = "text/html";
                    break;
                case "xml":
                    rel = "alternate";
                    type = "application/gml+xml;profile=\\\"http://www.opengis.net/def/profile/ogc/2.0/gml-sf2\\\";version=3.2";
            }
            if (isCollection)
                rel = "data";
            return new ImmutableWfs3Link.Builder()
                    .href(uriBuilder
                            .copy()
                            .removeLastPathSegment("items")
                            .ensureLastPathSegment(collectionId)
                            .setParameter("f", mediaType.parameter())
                            .toString())
                    .rel(rel)
                    .type(type)
                    .description("Information about the " + collectionName + " data")
                    .build();
        };
    }

    private String getUrlWithPageAndCount(final URICustomizer uriBuilder, final int page, final int count) {
        return uriBuilder
                .removeParameters("page", "startIndex", "offset", "count", "limit")
                .ensureParameter("offset", String.valueOf((page - 1) * count))
                .ensureParameter("limit", String.valueOf(count))
                .toString();
    }
}
