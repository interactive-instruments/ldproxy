/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.domain;

import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static de.ii.ldproxy.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

public interface TileFormatWithQuerySupportExtension extends TileFormatExtension {

    @Override
    default boolean canMultiLayer() { return true; }

    @Override
    default boolean canTransformFeatures() { return true; }

    Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContextTiles transformationContext, Optional<Locale> language);

    byte[] getEmptyTile(Tile tile);

    FeatureQuery getQuery(Tile tile,
                          List<OgcApiQueryParameter> allowedParameters,
                          Map<String, String> queryParameters,
                          TilesConfiguration tilesConfiguration,
                          URICustomizer uriCustomizer);

    class MultiLayerTileContent {
        public byte[] byteArray;
        public boolean isComplete;
    }

    MultiLayerTileContent combineSingleLayerTilesToMultiLayerTile(TileMatrixSet tileMatrixSet, Map<String, Tile> singleLayerTileMap, Map<String, ByteArrayOutputStream> singleLayerByteArrayMap) throws IOException;

    double getMaxAllowableOffsetNative(Tile tile);
    double getMaxAllowableOffsetCrs84(Tile tile);
}
