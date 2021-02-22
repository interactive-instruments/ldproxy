/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.tiles.TilesConfiguration;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.net.URI;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class WorldMercatorWGS84Quad extends AbstractTileMatrixSet implements TileMatrixSet {

    private static final EpsgCrs CRS = EpsgCrs.of(3395);

    /**
     * The bounding box of the tiling scheme
     */
    private static final double BBOX_MIN_X = -20037508.3427892;
    private static final double BBOX_MAX_X = 20037508.3427892;
    private static final double BBOX_MIN_Y = -20037508.3427892;
    private static final double BBOX_MAX_Y = 20037508.3427892;
    private static final BoundingBox BBOX = new BoundingBox(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public String getId() {
        return "WorldMercatorWGS84Quad";
    }

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    }

    @Override
    public Optional<String> getTitle() { return Optional.of("WGS84 for the World"); }

    @Override
    public Optional<URI> getWellKnownScaleSet() { return Optional.of(URI.create("http://www.opengis.net/def/wkss/OGC/1.0/WorldMercatorWGS84")); }

    @Override
    public int getMaxLevel() { return 24; }

    @Override
    public double getInitialScaleDenominator() { return 559082264.02871774; }

    @Override
    public int getInitialWidth() { return 1; }

    @Override
    public int getInitialHeight() { return 1; }

    @Override
    public BoundingBox getBoundingBox() { return BBOX; }
}
