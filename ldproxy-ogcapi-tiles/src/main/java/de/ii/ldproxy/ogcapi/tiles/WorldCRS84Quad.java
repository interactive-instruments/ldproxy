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

public class WorldCRS84Quad extends AbstractTileMatrixSet implements TileMatrixSet {

    private static final EpsgCrs CRS = new EpsgCrs(4326, true);

    /**
     * The bounding box of the tiling scheme
     */
    private static final double BBOX_MIN_X = -180.0;
    private static final double BBOX_MAX_X = 180.0;
    private static final double BBOX_MIN_Y = -90.0;
    private static final double BBOX_MAX_Y = 90.0;
    private static final BoundingBox BBOX = new BoundingBox(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);
    private static final double[] TOP_LEFT_CORNER = {BBOX_MIN_X, BBOX_MAX_Y};

    @Override
    public String getId() {
        return "WorldCRS84Quad";
    }

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    }

    @Override
    public Optional<String> getTitle() { return Optional.of("CRS84 for the World"); }

    @Override
    public Optional<URI> getWellKnownScaleSet() { return Optional.of(URI.create("http://www.opengis.net/def/wkss/OGC/1.0/GoogleCRS84Quad")); }

    @Override
    public int getMaxLevel() {
        return 23;
    }

    @Override
    public double getInitialScaleDenominator() {
        return 279541132.0143589;
    }

    @Override
    public int getInitialWidth() {
        return 2;
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
