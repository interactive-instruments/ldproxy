/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;
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

    public List<Wfs3Link> generateDatasetLinks(URICustomizer uriBuilder, Optional<String> describeFeatureTypeUrl, Wfs3MediaType mediaType, Wfs3MediaType... alternativeMediaTypes) {
        uriBuilder
                .ensureParameter("f", mediaType.parameter());

        final boolean isCollections = uriBuilder.isLastPathSegment("collections");

        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                                      .href(uriBuilder.toString())
                                      .rel("self")
                                      .type(mediaType.metadata()
                                                     .toString())
                                      .description("this document")
                                      .build())
                .addAll(Arrays.stream(alternativeMediaTypes)
                              .map(generateAlternateLink(uriBuilder.copy(), true))
                              .collect(Collectors.toList()))
                .add(ImmutableWfs3Link.builder()
                                      .href(uriBuilder.copy()
                                                      .removeLastPathSegment("collections")
                                                      .ensureLastPathSegment("api")
                                                      .setParameter("f", "json")
                                                      .toString())
                                      .rel("service")
                                      .type("application/openapi+json;version=3.0")
                                      .description("the OpenAPI definition")
                                      .typeLabel("JSON")
                                      .build())
                .add(ImmutableWfs3Link.builder()
                                      .href(uriBuilder.copy()
                                                      .removeLastPathSegment("collections")
                                                      .ensureLastPathSegment("api")
                                                      .setParameter("f", "html")
                                                      .toString())
                                      .rel("service")
                                      .type("text/html")
                                      .description("the OpenAPI definition")
                                      .typeLabel("HTML")
                                      .build())
                .add(ImmutableWfs3Link.builder()
                                      .href(uriBuilder.copy()
                                                      .removeLastPathSegment("collections")
                                                      .ensureLastPathSegment("conformance")
                                                      .setParameter("f", "json")
                                                      .toString())
                                      .rel("conformance")
                                      .type("application/json")
                                      .description("WFS 3.0 conformance classes implemented by this server")
                                      .build())
                .add(ImmutableWfs3Link.builder()
                                      .href(uriBuilder.copy()
                                                      .removeLastPathSegment("collections")
                                                      .ensureLastPathSegment("tilingSchemes")
                                                      .setParameter("f", "json")
                                                      .toString())
                                                      .rel("tilingSchemes")
                                      .type("application/json")
                                      .description("the list of available tiling schemes")
                                      .build());

        if (!isCollections) {
            builder
                    .add(ImmutableWfs3Link.builder()
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
            builder.add(ImmutableWfs3Link.builder()
                                         .href(describeFeatureTypeUrl.get())
                                         .rel("describedBy")
                                         .type("application/xml")
                                         .description("XML schema for all feature types")
                                         .build());
        }

        return builder.build();
    }

    public List<Wfs3Link> generateDatasetCollectionLinks(URICustomizer uriBuilder, String collectionId, String collectionName, Optional<String> describeFeatureTypeUrl, Wfs3MediaType mediaType, Wfs3MediaType... alternativeMediaTypes) {
        uriBuilder
                .ensureParameter("f", mediaType.parameter())
                .ensureLastPathSegments("collections", collectionId, "items");

        ImmutableList.Builder<Wfs3Link> links = new ImmutableList.Builder<Wfs3Link>()
                .addAll(Stream.concat(Stream.of(mediaType), Arrays.stream(alternativeMediaTypes))
                              .map(generateItemLink(uriBuilder.copy(), collectionName))
                              .collect(Collectors.toList()))
                .addAll(Stream.concat(Stream.of(mediaType), Arrays.stream(alternativeMediaTypes))
                        .map(generateCollectionsLink(uriBuilder.copy(), collectionName,collectionId))
                        .collect(Collectors.toList()));

        describeFeatureTypeUrl.ifPresent(url -> links.add(ImmutableWfs3Link.builder()
                                                                           .href(describeFeatureTypeUrl.get())
                                                                           .rel("describedBy")
                                                                           .type("application/xml")
                                                                           .description("XML schema for feature type " + collectionName)
                                                                           .build()));

        return links.build();
    }


    public List<Wfs3Link> generateCollectionOrFeatureLinks(URICustomizer uriBuilder, boolean isFeatureCollection, int page, int count, Wfs3MediaType mediaType, Wfs3MediaType... alternativeMediaTypes) {
        uriBuilder
                .ensureParameter("f", mediaType.parameter());

        final ImmutableList.Builder<Wfs3Link> links = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                                      .href(uriBuilder.toString())
                                      .rel("self")
                                      .type(mediaType.main()
                                                     .toString())
                                      .description("this document")
                                      .build())
                .addAll(Arrays.stream(alternativeMediaTypes)
                              .map(generateAlternateLink(uriBuilder.copy(), false))
                              .collect(Collectors.toList()));

        if (isFeatureCollection) {
            links.add(ImmutableWfs3Link.builder()
                                       .href(getUrlWithPageAndCount(uriBuilder.copy(), page + 1, count))
                                       .rel("next")
                                       .type(mediaType.main()
                                                      .toString())
                                       .description("next page")
                                       .build());
            if (page > 1) {
                links.add(ImmutableWfs3Link.builder()
                                           .href(getUrlWithPageAndCount(uriBuilder.copy(), page - 1, count))
                                           .rel("prev")
                                           .type(mediaType.main()
                                                          .toString())
                                           .description("previous page")
                                           .build());
            }
        } else {
            links.add(ImmutableWfs3Link.builder()
                                       .href(uriBuilder.copy()
                                                       .removeLastPathSegments(1)
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

    public List<Wfs3Link> generateAlternateLinks(final URICustomizer uriBuilder, boolean isMetadata, Wfs3MediaType... alternativeMediaTypes)  {
        return Arrays.stream(alternativeMediaTypes)
                     .map(generateAlternateLink(uriBuilder.copy(), isMetadata))
                     .collect(Collectors.toList());
    }

    private Function<Wfs3MediaType, Wfs3Link> generateAlternateLink(final URIBuilder uriBuilder, boolean isMetadata) {
        return mediaType -> ImmutableWfs3Link.builder()
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

    private Function<Wfs3MediaType, Wfs3Link> generateItemLink(final URIBuilder uriBuilder, final String collectionName) {
        return mediaType -> ImmutableWfs3Link.builder()
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

    private Function<Wfs3MediaType, Wfs3Link> generateCollectionsLink(final URICustomizer uriBuilder, final String collectionName, final String collectionId) {
        return mediaType -> ImmutableWfs3Link.builder()
                .href(uriBuilder
                        .copy()
                        .removeLastPathSegment("items")
                        .ensureLastPathSegment(collectionId)
                        .setParameter("f", mediaType.parameter())
                        .toString())
                .rel("data")
                .type(mediaType.main()
                        .toString())
                .description(collectionName)
                .typeLabel(mediaType.label())
                .build();
    }

    private String getUrlWithPageAndCount(final URICustomizer uriBuilder, final int page, final int count) {
        return uriBuilder
                .removeParameters("page", "startIndex", "offset", "count", "limit")
                .ensureParameter("page", String.valueOf(page))
                .ensureParameter("limit", String.valueOf(count))
                .toString();
    }


    public List<Wfs3Link> generateGeoJSONTileLinks(URICustomizer uriBuilder, Wfs3MediaType mediaType,Wfs3MediaType alternativeMediaType,String tilingSchemeId, String zoomLevel, String row, String col) {

        uriBuilder.removeLastPathSegments(3);
        uriBuilder
                .ensureParameter("f", mediaType.parameter())
                .ensureLastPathSegments("tiles","default",zoomLevel,row,col);


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder
                                .ensureLastPathSegments("tiles",tilingSchemeId,zoomLevel,row,col)
                                .toString())
                        .rel("self")
                        .type(mediaType.metadata()
                                .toString())
                        .description("this document")
                        .build())
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy().ensureLastPathSegments("tiles",tilingSchemeId,zoomLevel,row,col)
                                .removeLastPathSegment("")
                                .setParameter("f", "mvt")
                                .toString())
                        .rel("alternate")
                        .type(alternativeMediaType.metadata()
                                .toString())
                        .description("this document as MVT")
                        .build());


        return builder.build();
    }

    public List<Wfs3Link> generateTilingSchemesLinks(URICustomizer uriBuilder, String tilingSchemeId) {


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment("tilingSchemes")
                                .ensureLastPathSegment(tilingSchemeId)
                                .setParameter("f", "json")
                                .toString()
                        )
                        .rel("about")
                        .type("application/json")
                        .description(tilingSchemeId + " tiling Scheme")
                        .build())
                ;


        return builder.build();
    }
    public List<Wfs3Link> generateTilingSchemeLinks(URICustomizer uriBuilder, String tilingSchemeId) {


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy().ensureLastPathSegment("tiles/").clearParameters().toString() + "{tilingSchemeId}/{level}/{row}/{column}?f=json")
                        .rel("tile-template")
                        .type("application/geo+json")
                        .description("Tiles in GeoJSON. The link is a URI template where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' property, {level}/{row}/{col} the tile based on the tiling scheme.")
                        .templated("true")
                        .build())

                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy().ensureLastPathSegment("tiles/").clearParameters().toString() + "{tilingSchemeId}/{level}/{row}/{column}?f=mvt")
                        .rel("tile-template")
                        .type("application/vnd.mapbox-vector-tile")
                        .description("Mapbox vector tiles. The link is a URI template where {tilingSchemeId} is one of the schemes listed in the 'tilingSchemes' property, {level}/{row}/{col} the tile based on the tiling scheme.")
                        .templated("true")
                        .build());


        return builder.build();
    }
}
