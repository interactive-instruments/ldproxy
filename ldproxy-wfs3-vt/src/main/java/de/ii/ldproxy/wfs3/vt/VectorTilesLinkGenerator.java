/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiLink;
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;

import java.util.List;

/**
 * This class is responsible for generating the links in the json Files.
 */
public class VectorTilesLinkGenerator {

    /**
     * generates the Links on the page /serviceId?f=json and /serviceId/collections?f=json
     *
     * @param uriBuilder the URI, split in host, path and query
     * @return a List with links
     */
    public List<OgcApiLink> generateDatasetLinks(URICustomizer uriBuilder) {

        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .removeLastPathSegment("collections")
                                        .ensureLastPathSegment("tileMatrixSets")
                                        .ensureParameter("f", "json")
                                        .toString())
                        .rel("tileMatrixSets")
                        .type("application/json")
                        .description("List of tileMatrixSets implemented by this API in JSON")
                        .build())
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .removeLastPathSegment("collections")
                                .ensureLastPathSegment("tiles")
                                .ensureParameter("f", "json")
                                .toString())
                        .rel("tiles")
                        .type("application/json")
                        .description("Link to information on map tiles combining more than one collection")
                        .build())
                .build();
    }

    /**
     * generates the links on the page /serviceId/tileMatrixSets
     *
     * @param uriBuilder        the URI, split in host, path and query
     * @param tileMatrixSetId   the id of the tiling Scheme
     * @return a list with links
     */
    public List<OgcApiLink> generateTileMatrixSetsLinks(URICustomizer uriBuilder, String tileMatrixSetId) {


        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment(tileMatrixSetId)
                                        .setParameter("f", "json")
                                        .toString()
                        )
                        .rel("tileMatrixSet")
                        .type("application/json")
                        .build())
                .build();
    }

    /**
     * generates the links on the page /serviceId/tiles
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @return a list with links
     */
    public List<OgcApiLink> generateTilesLinks(URICustomizer uriBuilder, String tilingSchemeId) {

        //TODO: check if this method should be removed
        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                        .ensureLastPathSegment("tiles")
                                        .ensureLastPathSegment(tilingSchemeId)
                                        .setParameter("f", "json")
                                        .toString()
                        )
                        .rel("tiles")
                        .type("application/json")
                        .description("Vector Tiles Description") //TODO dynamic naming
                        .build())
                .build();
    }


    /**
     * generates the links on the page /serviceId/tiles
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @return a list with links
     */
    public List<OgcApiLink> generateTilesLinks(URICustomizer uriBuilder) {


        return ImmutableList.<OgcApiLink>builder()
                .add(new ImmutableOgcApiLink.Builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment("tiles")
                                .setParameter("f", "json")
                                .toString()
                        )
                        .rel("tiles")
                        .type("application/json")
                        .description("Vector Tiles Description") //TODO dynamic naming
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
     * @return
     */
    public List<OgcApiLink> generateGeoJSONTileLinks(URICustomizer uriBuilder, OgcApiMediaType mediaType,
                                                     OgcApiMediaType alternateMediaType, String tileMatrixSetId,
                                                     String zoomLevel, String row, String col, boolean mvt,
                                                     boolean json) {

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
                    .description("this document")
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
                    .type(alternateMediaType.type() // TODO: check
                                              .toString())
                    .description("this document as MVT")
                    .build());
        }
        return builder.build();
    }


    /**
     * generates the URI templates on the page /serviceId/tilingSchemes/{tilingSchemeId} and /serviceId/tiles/{tilingSchemeId}
     *
     * @param uriBuilder        the URI, split in host, path and query
     * @param tileMatrixSetId   the id of the tiling Scheme
     * @param mvt               mvt enabled or disabled
     * @param json              json enabled or disabled
     * @return a list with links
     */
    public List<OgcApiLink> generateTileMatrixSetLinks(URICustomizer uriBuilder, String tileMatrixSetId, boolean mvt,
                                                       boolean json) {


        final ImmutableList.Builder<OgcApiLink> builder = new ImmutableList.Builder<OgcApiLink>();

        if (json) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder.copy()
                                    .clearParameters()
                                    .toString() + "/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=json")
                    .rel("tiles")
                    .type("application/geo+json")
                    .description("Tile in GeoJSON. The link is a URI template where {tileMatrix}/{tileRow}/{tileCol} is the tile based on the tiling scheme {tileMatrixSetId}.")
                    .templated("true")
                    .build());
        }
        if (mvt) {
            builder.add(new ImmutableOgcApiLink.Builder()
                    .href(uriBuilder.copy()
                                    .clearParameters()
                                    .toString() + "/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=mvt")
                    .rel("tiles")
                    .type("application/vnd.mapbox-vector-tile")
                    .description("Mapbox vector tile. The link is a URI template where {tileMatrix}/{tileRow}/{tileCol} is the tile based on the tiling scheme {tileMatrixSetId}.")
                    .templated("true")
                    .build());
        }

        return builder.build();
    }

}
