/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.*;
import de.ii.ldproxy.ogcapi.features.core.api.FeatureTransformationContext;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import org.apache.felix.ipojo.annotations.Requires;

import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.util.*;

public interface TileFormatExtension extends FormatExtension {

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
                          Set<OgcApiQueryParameter> allowedParameters,
                          Map<String, String> queryParameters,
                          TilesConfiguration tilesConfiguration,
                          URICustomizer uriCustomizer);

    class MultiLayerTileContent {
        byte[] byteArray;
        boolean isComplete;
    }

    MultiLayerTileContent combineSingleLayerTilesToMultiLayerTile(TileMatrixSet tileMatrixSet, Map<String, Tile> singleLayerTileMap, Map<String, ByteArrayOutputStream> singleLayerByteArrayMap);

    double getMaxAllowableOffsetNative(Tile tile);
    double getMaxAllowableOffsetCrs84(Tile tile);
}
