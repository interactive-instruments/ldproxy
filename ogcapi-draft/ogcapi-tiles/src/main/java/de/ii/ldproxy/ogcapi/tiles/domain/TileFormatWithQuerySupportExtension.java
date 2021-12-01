/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.features.domain.FeatureQuery;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class TileFormatWithQuerySupportExtension extends TileFormatExtension {

    @Override
    public boolean canMultiLayer() { return true; }

    @Override
    public boolean canTransformFeatures() { return true; }

    public abstract FeatureQuery getQuery(Tile tile,
                                          List<OgcApiQueryParameter> allowedParameters,
                                          Map<String, String> queryParameters,
                                          TilesConfiguration tilesConfiguration,
                                          URICustomizer uriCustomizer);

    public class MultiLayerTileContent {
        public byte[] byteArray;
        public boolean isComplete;
    }

    public abstract MultiLayerTileContent combineSingleLayerTilesToMultiLayerTile(TileMatrixSet tileMatrixSet, Map<String, Tile> singleLayerTileMap, Map<String, ByteArrayOutputStream> singleLayerByteArrayMap) throws IOException;

    public abstract double getMaxAllowableOffsetNative(Tile tile);
    public abstract double getMaxAllowableOffsetCrs84(Tile tile);
}
