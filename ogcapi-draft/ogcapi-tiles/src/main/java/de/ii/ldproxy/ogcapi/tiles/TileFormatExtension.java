/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.FormatExtension;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public interface TileFormatExtension extends FormatExtension {

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData) {
        return apiData.getExtension(TilesConfiguration.class)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::getMultiCollectionEnabled)
                      .filter(config -> config.getTileEncodings().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
        return apiData.getCollections()
                      .get(collectionId)
                      .getExtension(TilesConfiguration.class)
                      .filter(TilesConfiguration::getEnabled)
                      .filter(TilesConfiguration::getSingleCollectionEnabled)
                      .filter(config -> config.getTileEncodings().contains(this.getMediaType().label()))
                      .isPresent();
    }

    @Override
    default String getPathPattern() {
        return "^(?:/collections/[\\w\\-]+)?/tiles/\\w+/\\w+/\\w+/\\w+/?$";
    }

    default boolean canMultiLayer() { return false; }

    default boolean canTransformFeatures() { return false; }

    Optional<FeatureTransformer2> getFeatureTransformer(FeatureTransformationContextTiles transformationContext, Optional<Locale> language);

    String getExtension();

    Object getEmptyTile(Tile tile);

    FeatureQuery getQuery(Tile tile,
                          List<OgcApiQueryParameter> allowedParameters,
                          Map<String, String> queryParameters,
                          TilesConfiguration tilesConfiguration,
                          URICustomizer uriCustomizer);

    class MultiLayerTileContent {
        byte[] byteArray;
        boolean isComplete;
    }

    MultiLayerTileContent combineSingleLayerTilesToMultiLayerTile(TileMatrixSet tileMatrixSet, Map<String, Tile> singleLayerTileMap, Map<String, ByteArrayOutputStream> singleLayerByteArrayMap) throws IOException;

    double getMaxAllowableOffsetNative(Tile tile);
    double getMaxAllowableOffsetCrs84(Tile tile);
}
