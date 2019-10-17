/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiMediaType;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * This class is responsible for generating the links in the json Files.
 */
public class VectorTilesLinkGenerator {

    /**
     * generates the Links on the page /serviceId?f=json and /serviceId/collections?f=json
     *
     * @param uriBuilder the URI, split in host, path and query
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a List with links
     */
    public List<OgcApiLink> generateDatasetLinks(URICustomizer uriBuilder, I18n i18n, Optional<Locale> language) {

        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegment("collections")
                                        .ensureLastPathSegment("tileMatrixSets")
                                        .removeParameters("f")
                                        .toString())
                        .rel("tileMatrixSets")
                        .description(i18n.get("tileMatrixSetsLink", language))
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .removeLastPathSegment("collections")
                                .ensureLastPathSegment("tiles")
                                .removeParameters("f")
                                .toString())
                        .rel("tiles")
                        .description(i18n.get("tilesLink", language))
                        .build())
                .build();
    }

    /**
     * generates the links on the page /serviceId/tileMatrixSets
     *
     * @param uriBuilder        the URI, split in host, path and query
     * @param tileMatrixSetId   the id of the tiling Scheme
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<OgcApiLink> generateTileMatrixSetsLinks(URICustomizer uriBuilder, String tileMatrixSetId, I18n i18n, Optional<Locale> language) {


        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment(tileMatrixSetId)
                                        .removeParameters("f")
                                        .toString())
                        .rel("tileMatrixSet")
                        .type("application/json")
                        .description(i18n.get("tileMatrixSetLink", language).replace("{{tileMatrixSetId}}", tileMatrixSetId))
                        .build())
                .build();
    }

    /**
     * generates the Links in the GeoJSON Tiles
     *
     * @param uriBuilder           the URI, split in host, path and query
     * @param mediaType            the media type
     * @param alternateMediaType the alternative media type
     * @param tileMatrixSetId       the id of the tiling scheme of the tile
     * @param zoomLevel            the zoom level of the tile
     * @param row                  the row of the tile
     * @param col                  the col of the tile
     * @param mvt                  mvt enabled or disabled
     * @param json                 json enabled or disabled
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return
     */
    public List<OgcApiLink> generateGeoJSONTileLinks(URICustomizer uriBuilder, OgcApiMediaType mediaType,
                                                     OgcApiMediaType alternateMediaType, String tileMatrixSetId,
                                                     String zoomLevel, String row, String col, boolean mvt,
                                                     boolean json, I18n i18n, Optional<Locale> language) {

        uriBuilder.removeLastPathSegments(3);
        uriBuilder
                .ensureParameter("f", mediaType.parameter())
                .ensureLastPathSegments("tiles", "WebMercatorQuad", zoomLevel, row, col);


        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>();

        if (json) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder
                            .ensureLastPathSegments("tiles", tileMatrixSetId, zoomLevel, row, col)
                            .toString())
                    .rel("self")
                    .type(mediaType.type()
                                   .toString())
                    .description(i18n.get("selfLink", language))
                    .build());
        }
        if (mvt) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder.copy()
                                    .ensureLastPathSegments("tiles", tileMatrixSetId, zoomLevel, row, col)
                                    .removeLastPathSegment("")
                                    .setParameter("f", "mvt")
                                    .toString())
                    .rel("alternate")
                    .type("application/vnd.mapbox-vector-tile")
                    .description(i18n.get("alternateLink", language))
                    .typeLabel("MVT")
                    .build());
        }
        return builder.build();
    }


    /**
     * generates the URI templates on the page /serviceId/tileMatrixSets/{tileMatrixSetId} and /serviceId/tiles/{tileMatrixSetId}
     *
     * @param uriBuilder        the URI, split in host, path and query
     * @param tileMatrixSetId   the id of the tiling Scheme
     * @param mvt               mvt enabled or disabled
     * @param json              json enabled or disabled
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<OgcApiLink> generateTileMatrixSetLinks(URICustomizer uriBuilder, String tileMatrixSetId, boolean mvt,
                                                       boolean json, I18n i18n, Optional<Locale> language) {


        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>();

        if (json) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder.copy()
                                    .clearParameters()
                                    .ensureNoTrailingSlash()
                                    .toString() + (tileMatrixSetId==null ? "" : "/"+tileMatrixSetId) + "/{tileMatrix}/{tileRow}/{tileCol}?f=json")
                    .rel("tiles")
                    .type("application/geo+json")
                    .description(i18n.get("tilesLinkTemplateGeoJSON", language))
                    .templated("true")
                    .build());
        }
        if (mvt) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder.copy()
                                    .clearParameters()
                                    .ensureNoTrailingSlash()
                                    .toString() + (tileMatrixSetId==null ? "" : "/"+tileMatrixSetId) + "/{tileMatrix}/{tileRow}/{tileCol}?f=mvt")
                    .rel("tiles")
                    .type("application/vnd.mapbox-vector-tile")
                    .description(i18n.get("tilesLinkTemplateMVT", language))
                    .templated("true")
                    .build());
        }
        builder.add(new ImmutableOgcApiLink.Builder()
                .href(uriBuilder.copy()
                        .clearParameters()
                        .ensureNoTrailingSlash()
                        .toString() + (tileMatrixSetId==null ? "" : "/"+tileMatrixSetId))
                .rel("multitiles")
                .type("application/geo+json")
                .description("description")
                .build());

        return builder.build();
    }

    /**
     * generates the links on the page /serviceId/tiles
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param i18n module to get linguistic text
     * @param language the requested language (optional)
     * @return a list with links
     */
    public List<OgcApiLink> generateTilesLinks(URICustomizer uriBuilder, I18n i18n, Optional<Locale> language) {


        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment("tiles")
                                .removeParameters("f")
                                .toString()
                        )
                        .rel("tiles")
                        .description(i18n.get("tilesLink", language))
                        .build())
                .build();
    }
}
