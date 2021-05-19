/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.AbstractTileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.net.URI;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class WorldCRS84Quad extends AbstractTileMatrixSet implements TileMatrixSet {

    private static final EpsgCrs CRS = EpsgCrs.of(4326, Force.LON_LAT);

    /**
     * The bounding box of the tiling scheme
     */
    private static final double BBOX_MIN_X = -180.0;
    private static final double BBOX_MAX_X = 180.0;
    private static final double BBOX_MIN_Y = -90.0;
    private static final double BBOX_MAX_Y = 90.0;
    private static final BoundingBox BBOX = BoundingBox.of(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);
    private static final double[] TOP_LEFT_CORNER = {BBOX_MIN_X, BBOX_MAX_Y};

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

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
