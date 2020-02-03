/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;


import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.EpsgCrs;

import java.net.URI;
import java.util.Optional;

/**
 * This is the most commonly used tile matrix set. It is used by Google Maps and most other web mapping applications.
 * In WMTS it is called "Google Maps Compatible", in the Tile Matrix Set standard "WebMercatorQuad".
 *
 */
public class WebMercatorQuad extends AbstractTileMatrixSet implements TileMatrixSet {

    /**
     * Web Mercator is the coordinate reference system of the tile matrix set, EPSG code is 3857
     */
    private static final EpsgCrs CRS = EpsgCrs.of(3857);

    /**
     * The bounding box of the tile matrix set
     */
    private static final double BBOX_MIN_X = -20037508.3427892;
    private static final double BBOX_MAX_X = 20037508.3427892;
    private static final double BBOX_MIN_Y = -20037508.3427892;
    private static final double BBOX_MAX_Y = 20037508.3427892;
    private static final BoundingBox BBOX = new BoundingBox(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);

    private TileMatrixSetData data;

    @Override
    public String getId() {
        return "WebMercatorQuad";
    };

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    };

    @Override
    public Optional<String> getTitle() { return Optional.of("Google Maps Compatible for the World"); }

    @Override
    public Optional<URI> getWellKnownScaleSet() { return Optional.of(URI.create("http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible")); }

    @Override
    public int getMaxLevel() {
        return 24;
    };

    @Override
    public double getInitialScaleDenominator() {
        return 559082264.0287178;
    }

    @Override
    public int getInitialWidth() {
        return 1;
    }

    @Override
    public int getInitialHeight() {
        return 1;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return BBOX;
    }

}
