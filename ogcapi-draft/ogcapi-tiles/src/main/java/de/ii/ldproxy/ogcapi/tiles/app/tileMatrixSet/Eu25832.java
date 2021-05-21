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
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.net.URI;
import java.util.Optional;

@Component
@Provides
@Instantiate
public class Eu25832 extends AbstractTileMatrixSet implements TileMatrixSet {

    private static final EpsgCrs CRS = EpsgCrs.of(25832);

    /**
     * The bounding box of the tiling scheme
     */
    private static final double DIFF = 1252344.27142433;
    private static final double BBOX_MIN_X = -3803165.98427;
    private static final double BBOX_MAX_Y = 8805908.08285;
    private static final double BBOX_MAX_X = BBOX_MIN_X + DIFF * 6;
    private static final double BBOX_MIN_Y = BBOX_MAX_Y - DIFF * 5;
    private static final BoundingBox BBOX = BoundingBox.of(BBOX_MIN_X, BBOX_MIN_Y, BBOX_MAX_X, BBOX_MAX_Y, CRS);

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return TilesConfiguration.class;
    }

    @Override
    public String getId() {
        return "EU_25832";
    }

    @Override
    public EpsgCrs getCrs() {
        return CRS;
    }

    @Override
    public Optional<String> getTitle() { return Optional.of("Tiling scheme für ETRS89/UTM32N covering Europe"); }

    @Override
    public Optional<URI> getWellKnownScaleSet() { return Optional.empty(); }

    @Override
    public int getMaxLevel() { return 14; }

    @Override
    public double getInitialScaleDenominator() { return  17471320.7508974; }

    @Override
    public int getInitialWidth() { return 6; }

    @Override
    public int getInitialHeight() { return 5; }

    @Override
    public BoundingBox getBoundingBox() { return BBOX; }
}
