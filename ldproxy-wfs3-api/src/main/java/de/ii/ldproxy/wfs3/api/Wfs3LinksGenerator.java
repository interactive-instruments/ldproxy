/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

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
import java.util.stream.Stream;

/**
 * @author zahnen
 */
public class Wfs3LinksGenerator {

    public List<OgcApiLink> generateDatasetCollectionLinks(URICustomizer uriBuilder, String collectionId,
                                                           String collectionName, Optional<String> describeFeatureTypeUrl,
                                                           OgcApiMediaType mediaType,
                                                           List<OgcApiMediaType> alternateMediaTypes,
                                                           List<OgcApiMediaType> featureMediaTypes) {
        boolean isCollection = false;

        if (uriBuilder.getLastPathSegment()
                      .equals("collections"))
            isCollection = true;

        uriBuilder
                .ensureParameter("f", mediaType.parameter())
                .ensureLastPathSegments("collections", collectionId, "items");

        ImmutableList.Builder<OgcApiLink> links = new ImmutableList.Builder<OgcApiLink>()
                .addAll(featureMediaTypes.stream()
                              .map(generateItemsLink(uriBuilder.copy(), collectionName))
                              .collect(Collectors.toList()))
                .addAll(Stream.concat(Stream.of(mediaType), alternateMediaTypes.stream())
                              .map(generateCollectionsLink(uriBuilder.copy(), collectionName, collectionId, isCollection))
                              .collect(Collectors.toList()));

        describeFeatureTypeUrl.ifPresent(url -> links.add(new ImmutableOgcApiLink.Builder()
                .href(describeFeatureTypeUrl.get())
                .rel("describedBy")
                .type("application/xml")
                .description("XML schema for feature type " + collectionName)
                .build()));

        return links.build();
    }


    public List<OgcApiLink> generateCollectionOrFeatureLinks(URICustomizer uriBuilder, boolean isFeatureCollection,
                                                             int page, int count, OgcApiMediaType mediaType,
                                                             List<OgcApiMediaType> alternateMediaTypes,
                                                             List<OgcApiMediaType> collectionMediaTypes) {
        uriBuilder
                .ensureParameter("f", mediaType.parameter());

        final ImmutableList.Builder<OgcApiLink> links = new ImmutableList.Builder<OgcApiLink>()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.toString())
                        .rel("self")
                        .type(mediaType.type()
                                       .toString())
                        .description("this document")
                        .build())
                .addAll(alternateMediaTypes.stream()
                                           .map(generateAlternateLink(uriBuilder.copy()))
                                           .collect(Collectors.toList()));

        if (isFeatureCollection) {
            // TODO: no next link, if there is no next page
            links.add(new ImmutableOgcApiLink.Builder()
                    .href(getUrlWithPageAndCount(uriBuilder.copy(), page + 1, count))
                    .rel("next")
                    .type(mediaType.type()
                                   .toString())
                    .description("next page")
                    .build());
            if (page > 1) {
                links.add(new ImmutableOgcApiLink.Builder()
                        .href(getUrlWithPageAndCount(uriBuilder.copy(), page - 1, count))
                        .rel("prev")
                        .type(mediaType.type()
                                       .toString())
                        .description("previous page")
                        .build());
            }
        } else {
            links.addAll(collectionMediaTypes.stream()
                    .map(generateCollectionLink(uriBuilder.copy()))
                    .collect(Collectors.toList()));
        }

        return links.build();
    }

    public List<OgcApiLink> generateAlternateLinks(final URICustomizer uriBuilder,
                                                   List<OgcApiMediaType> alternateMediaTypes) {
        return alternateMediaTypes.stream()
                                  .map(generateAlternateLink(uriBuilder.copy()))
                                  .collect(Collectors.toList());
    }

    private Function<OgcApiMediaType, OgcApiLink> generateAlternateLink(final URIBuilder uriBuilder) {
        return mediaType -> new ImmutableOgcApiLink.Builder()
                .href(uriBuilder
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("alternate")
                .type(mediaType.type()
                               .toString())
                .description("this document")
                .typeLabel(mediaType.label())
                .build();
    }

    private Function<OgcApiMediaType, OgcApiLink> generateCollectionLink(final URICustomizer uriBuilder) {
        return mediaType -> new ImmutableOgcApiLink.Builder()
                .href(uriBuilder
                        .copy()
                        .removeLastPathSegments(2)
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("collection")
                .type(mediaType.type()
                        .toString())
                .description("the collection document")
                .typeLabel(mediaType.label())
                .build();
    }

    private Function<OgcApiMediaType, OgcApiLink> generateItemsLink(final URIBuilder uriBuilder,
                                                                    final String collectionName) {
        return mediaType -> new ImmutableOgcApiLink.Builder()
                .href(uriBuilder
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("items")
                .type(mediaType.type()
                               .toString())
                .description(collectionName)
                .typeLabel(mediaType.label())
                .build();
    }

    private Function<OgcApiMediaType, OgcApiLink> generateCollectionsLink(final URICustomizer uriBuilder,
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
            return new ImmutableOgcApiLink.Builder()
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
