/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.OgcApiQueriesHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryHandler;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryInput;
import de.ii.ldproxy.ogcapi.features.processing.FeatureProcessChain;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;

public interface TilesQueriesHandler extends OgcApiQueriesHandler<TilesQueriesHandler.Query> {

    @Override
    Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers();

    enum Query implements OgcApiQueryIdentifier {TILE_SETS, TILE_SET, SINGLE_LAYER_TILE, MULTI_LAYER_TILE, TILE_FILE, EMPTY_TILE}

    @Value.Immutable
    interface OgcApiQueryInputTileEmpty extends OgcApiQueryInput {

        Tile getTile();
    }

    @Value.Immutable
    interface OgcApiQueryInputTileFile extends OgcApiQueryInput {

        Tile getTile();
        File getTileFile();
    }

    @Value.Immutable
    interface OgcApiQueryInputTileMultiLayer extends OgcApiQueryInput {

        Tile getTile();
        Map<String, Tile> getSingleLayerTileMap();
        Map<String, FeatureQuery> getQueryMap();
        EpsgCrs getDefaultCrs();

        // the processing
        Optional<OutputStream> getOutputStream();
        Optional<FeatureProcessChain> getProcesses();
        Map<String, Object> getProcessingParameters();
    }

    @Value.Immutable
    interface OgcApiQueryInputTileSingleLayer extends OgcApiQueryInput {

        Tile getTile();
        FeatureQuery getQuery();
        EpsgCrs getDefaultCrs();

        // the processing
        Optional<OutputStream> getOutputStream();
        Optional<FeatureProcessChain> getProcesses();
        Map<String, Object> getProcessingParameters();
    }

    @Value.Immutable
    interface OgcApiQueryInputTileSets extends OgcApiQueryInput {

        Optional<String> getCollectionId();
        double[] getCenter();
        Map<String, MinMax> getTileMatrixSetZoomLevels();
    }

    @Value.Immutable
    interface OgcApiQueryInputTileSet extends OgcApiQueryInput {

        Optional<String> getCollectionId();
        String getTileMatrixSetId();
        double[] getCenter();
        MinMax getZoomLevels();
    }

}
