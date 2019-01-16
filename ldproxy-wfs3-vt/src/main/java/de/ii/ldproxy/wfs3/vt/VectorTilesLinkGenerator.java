/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.vt;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.wfs3.api.ImmutableWfs3Link;
import de.ii.ldproxy.wfs3.api.URICustomizer;
import de.ii.ldproxy.wfs3.api.Wfs3Link;
import de.ii.ldproxy.wfs3.api.Wfs3MediaType;

import java.util.List;

/**
 * This class is responsible for generating the links in the json Files.
 *
 */
public class VectorTilesLinkGenerator {

    /**
     * generates the Links on the page /serviceId?f=json and /serviceId/collections?f=json
     *
     * @param uriBuilder the URI, split in host, path and query
     * @return a List with links
     */
    public List<Wfs3Link> generateDatasetLinks(URICustomizer uriBuilder) {

        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>();
        uriBuilder.ensureParameter("f", "json");



            builder.add(ImmutableWfs3Link.builder()
                    .href(uriBuilder.copy()
                            .removeLastPathSegment("collections")
                            .ensureLastPathSegment("tilingSchemes")
                            .setParameter("f", "json")
                            .toString())
                    .rel("tilingSchemes")
                    .type("application/json")
                    .description("the list of available tiling schemes")
                    .build())
                    .add(ImmutableWfs3Link.builder()
                            .href(uriBuilder.copy()
                                    .removeLastPathSegment("collections")
                                    .ensureLastPathSegment("tiles")
                                    .setParameter("f", "json")
                                    .toString())
                            .rel("tiles")
                            .type("application/json")
                            .description("the list of available tiling schemes")
                            .build());

        return builder.build();
    }

    /**
     * generates the links on the page /serviceId/tilingSchemes
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param tilingSchemeId the id of the tiling Scheme
     * @return a list with links
     */
    public List<Wfs3Link> generateTilingSchemesLinks(URICustomizer uriBuilder, String tilingSchemeId) {


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment("tilingSchemes")
                                .ensureLastPathSegment(tilingSchemeId)
                                .setParameter("f", "json")
                                .toString()
                        )
                        .rel("tilingScheme")
                        .type("application/json")
                        .description("Google Maps Tiling Scheme")//TODO dynamic naming
                        .build())
                ;


        return builder.build();
    }

    /**
     * generates the links on the page /serviceId/tiles
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param tilingSchemeId the id of the tiling Scheme
     * @return a list with links
     */
    public List<Wfs3Link> generateTilesLinks(URICustomizer uriBuilder, String tilingSchemeId) {


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>()
                .add(ImmutableWfs3Link.builder()
                        .href(uriBuilder.copy()
                                .ensureLastPathSegment("tiles")
                                .ensureLastPathSegment(tilingSchemeId)
                                .setParameter("f", "json")
                                .toString()
                        )
                        .rel("tilingScheme")
                        .type("application/json")
                        .description("Google Maps Tiling Scheme") //TODO dynamic naming
                        .build())
                ;


        return builder.build();
    }

    /**
     * generates the Links in the GeoJSON Tiles
     *
     * @param uriBuilder            the URI, split in host, path and query
     * @param mediaType             the media type
     * @param alternativeMediaType  the alternative media type
     * @param tilingSchemeId        the id of the tiling scheme of the tile
     * @param zoomLevel             the zoom level of the tile
     * @param row                   the row of the tile
     * @param col                   the col of the tile
     * @param mvt                   mvt enabled or disabled
     * @param json                  json enabled or disabled
     * @return
     */
    public List<Wfs3Link> generateGeoJSONTileLinks(URICustomizer uriBuilder, Wfs3MediaType mediaType, Wfs3MediaType alternativeMediaType, String tilingSchemeId, String zoomLevel, String row, String col, boolean mvt, boolean json) {

        uriBuilder.removeLastPathSegments(3);
        uriBuilder
                .ensureParameter("f", mediaType.parameter())
                .ensureLastPathSegments("tiles","default",zoomLevel,row,col);


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>();

        if(json){
            builder.add(ImmutableWfs3Link.builder()
                    .href(uriBuilder
                            .ensureLastPathSegments("tiles",tilingSchemeId,zoomLevel,row,col)
                            .toString())
                    .rel("self")
                    .type(mediaType.main()
                            .toString())
                    .description("this document")
                    .build());
        }
        if(mvt){
            builder.add(ImmutableWfs3Link.builder()
                    .href(uriBuilder.copy().ensureLastPathSegments("tiles",tilingSchemeId,zoomLevel,row,col)
                            .removeLastPathSegment("")
                            .setParameter("f", "mvt")
                            .toString())
                    .rel("alternate")
                    .type(alternativeMediaType.metadata()
                            .toString())
                    .description("this document as MVT")
                    .build());
        }
        return builder.build();
    }


    /**
     * generates the URI templates on the page /serviceId/tilingSchemes/{tilingSchemeId} and /serviceId/tiles/{tilingSchemeId}
     *
     * @param uriBuilder     the URI, split in host, path and query
     * @param tilingSchemeId the id of the tiling Scheme
     * @param mvt            mvt enabled or disabled
     * @param json           json enabled or disabled
     * @return a list with links
     */
    public List<Wfs3Link> generateTilingSchemeLinks(URICustomizer uriBuilder, String tilingSchemeId, boolean mvt, boolean json) {


        final ImmutableList.Builder<Wfs3Link> builder = new ImmutableList.Builder<Wfs3Link>();

        if(json) {
            builder.add(ImmutableWfs3Link.builder()
                    .href(uriBuilder.copy().ensureLastPathSegment(tilingSchemeId).clearParameters().toString() + "/{level}/{row}/{col}?f=json")
                    .rel("tiles")
                    .type("application/geo+json")
                    .description("Tile in GeoJSON. The link is a URI template where {level}/{row}/{col} is the tile based on the tiling scheme.")
                    .templated("true")
                    .build());
        }
        if (mvt) {
            builder.add(ImmutableWfs3Link.builder()
                    .href(uriBuilder.copy().ensureLastPathSegment(tilingSchemeId).clearParameters().toString() + "/{level}/{row}/{col}?f=mvt")
                    .rel("tiles")
                    .type("application/vnd.mapbox-vector-tile")
                    .description("Mapbox vector tile. The link is a URI template where {level}/{row}/{col} is the tile based on the tiling scheme.")
                    .templated("true")
                    .build());
        }

        return builder.build();
    }

}
