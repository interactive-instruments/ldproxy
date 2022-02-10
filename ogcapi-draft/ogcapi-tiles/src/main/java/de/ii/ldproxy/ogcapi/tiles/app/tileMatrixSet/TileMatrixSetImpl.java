/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrix;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetData;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableBoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TileMatrixSetImpl implements TileMatrixSet {

    private final TileMatrixSetData data;

    public TileMatrixSetImpl(TileMatrixSetData data) {
        this.data = data;
    }

    @Override
    public String getId() {
        return data.getId();
    }

    @Override
    public Optional<URI> getURI() {
        return data.getUri();
    }

    @Override
    public Optional<String> getTitle() {
        return data.getTitle();
    }

    @Override
    public Optional<String> getDescription() {
        return data.getDescription();
    }

    @Override
    public List<String> getKeywords() {
        return data.getKeywords();
    }

    @Override
    public EpsgCrs getCrs() {
        EpsgCrs crs;
        try {
            crs = EpsgCrs.fromString(data.getCrs());
        } catch (Throwable e) {
            throw new IllegalStateException(String.format("The CRS URI '%s' is invalid: %s", data.getCrs(), e.getMessage()), e);
        }
        return crs;
    }

    @Override
    public List<String> getOrderedAxes() {
        return data.getOrderedAxes();
    }

    @Override
    public Optional<URI> getWellKnownScaleSet() {
        return data.getWellKnownScaleSet();
    }

    @Override
    public int getCols(int level) {
        return data.getTileMatrices()
                   .stream()
                   .filter(tm -> tm.getTileLevel()==level)
                   .findAny()
                   .map(TileMatrix::getMatrixWidth)
                   .orElse(0L).intValue();
    }

    @Override
    public int getRows(int level) {
        return data.getTileMatrices()
                   .stream()
                   .filter(tm -> tm.getTileLevel()==level)
                   .findAny()
                   .map(TileMatrix::getMatrixHeight)
                   .orElse(0L).intValue();
    }

    @Override
    public int getMaxLevel() {
        return data.getTileMatrices()
                   .stream()
                   .mapToInt(TileMatrix::getTileLevel)
                   .max()
                   .orElse(0);
    }

    @Override
    public int getMinLevel() {
        return data.getTileMatrices()
                   .stream()
                   .mapToInt(TileMatrix::getTileLevel)
                   .min()
                   .orElse(0);
    }

    @Override
    public TileMatrixSetData getTileMatrixSetData() {
        return data;
    }

    @Override
    public double getInitialScaleDenominator() {
        int minLevel = getMinLevel();
        return data.getTileMatrices()
                   .stream()
                   .filter(tm -> tm.getTileLevel()==minLevel)
                   .findAny()
                   .map(tm -> tm.getScaleDenominator().doubleValue())
                   .orElse(Double.NaN);
    }

    @Override
    public int getInitialWidth() {
        return getCols(getMinLevel());
    }

    @Override
    public int getInitialHeight() {
        return getRows(getMinLevel());
    }

    @Override
    public BoundingBox getBoundingBox() {
        return data.getBoundingBox().map(bbox -> new ImmutableBoundingBox.Builder().epsgCrs(bbox.getCrs().map(EpsgCrs::fromString).orElse(getCrs()))
                                                                                   .xmin(bbox.getLowerLeft()[0].doubleValue())
                                                                                   .ymin(bbox.getLowerLeft()[1].doubleValue())
                                                                                   .xmax(bbox.getUpperRight()[0].doubleValue())
                                                                                   .ymax(bbox.getUpperRight()[1].doubleValue())
                                                                                   .build())
                   .orElse(new ImmutableBoundingBox.Builder().epsgCrs(getCrs())
                                                             .xmin(data.getTileMatrices()
                                                                       .stream()
                                                                       .filter(tm -> tm.getCornerOfOrigin().equals("topLeft"))
                                                                       .mapToDouble(tm -> tm.getPointOfOrigin()[0].doubleValue())
                                                                       .min().orElse(Double.NaN))
                                                             .ymin(data.getTileMatrices()
                                                                       .stream()
                                                                       .filter(tm -> tm.getCornerOfOrigin().equals("topLeft"))
                                                                       .mapToDouble(tm -> tm.getPointOfOrigin()[1].doubleValue() - tm.getCellSize().doubleValue() * tm.getTileHeight() * tm.getMatrixHeight())
                                                                       .min().orElse(Double.NaN))
                                                             .xmax(data.getTileMatrices()
                                                                       .stream()
                                                                       .filter(tm -> tm.getCornerOfOrigin().equals("topLeft"))
                                                                       .mapToDouble(tm -> tm.getPointOfOrigin()[0].doubleValue() + tm.getCellSize().doubleValue() * tm.getTileWidth() * tm.getMatrixWidth())
                                                                       .max().orElse(Double.NaN))
                                                             .ymax(data.getTileMatrices()
                                                                       .stream()
                                                                       .filter(tm -> tm.getCornerOfOrigin().equals("topLeft"))
                                                                       .mapToDouble(tm -> tm.getPointOfOrigin()[1].doubleValue())
                                                                       .max().orElse(Double.NaN))
                                                             .build());
    }

    @Override
    public BoundingBox getBoundingBoxCrs84(CrsTransformerFactory crsTransformerFactory) throws CrsTransformationException {
        if (getCrs().equals(OgcCrs.CRS84))
            return getBoundingBox();
        CrsTransformer crsTransformer = crsTransformerFactory.getTransformer(getCrs(), OgcCrs.CRS84, true)
                                                             .orElseThrow(() -> new IllegalStateException(String.format("Could not transform the bounding box of tile matrix set '%s' to CRS84.", getId())));
        return crsTransformer.transformBoundingBox(getBoundingBox());
    }

    @Override
    public List<TileMatrix> getTileMatrices(int minLevel, int maxLevel) {
        return data.getTileMatrices()
                   .stream()
                   .filter(tm -> tm.getTileLevel()>=minLevel && tm.getTileLevel()<=maxLevel)
                   .collect(ImmutableList.toImmutableList());
    }

    @Override
    public TileMatrix getTileMatrix(int level) {
        return data.getTileMatrices()
                   .stream()
                   .filter(tm -> tm.getTileLevel()==level)
                   .findAny()
                   .orElse(null);
    }
}
