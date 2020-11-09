/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import de.ii.ldproxy.ogcapi.features.core.domain.processing.FeatureProcessChain;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface TilesQueriesHandler extends QueriesHandler<TilesQueriesHandler.Query> {

    @Override
    Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers();

    enum Query implements QueryIdentifier {TILE_SETS, TILE_SET, SINGLE_LAYER_TILE, MULTI_LAYER_TILE, TILE_FILE, EMPTY_TILE}

    @Value.Immutable
    interface QueryInputTileEmpty extends QueryInput {

        Tile getTile();
    }

    @Value.Immutable
    interface QueryInputTileFile extends QueryInput {

        Tile getTile();
        Path getTileFile();
    }

    @Value.Immutable
    interface QueryInputTileMultiLayer extends QueryInput {

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
    interface QueryInputTileSingleLayer extends QueryInput {

        Tile getTile();
        FeatureQuery getQuery();
        EpsgCrs getDefaultCrs();

        // the processing
        Optional<OutputStream> getOutputStream();
        Optional<FeatureProcessChain> getProcesses();
        Map<String, Object> getProcessingParameters();
    }

    @Value.Immutable
    interface QueryInputTileSets extends QueryInput {

        Optional<String> getCollectionId();
        double[] getCenter();
        Map<String, MinMax> getTileMatrixSetZoomLevels();
    }

    @Value.Immutable
    interface QueryInputTileSet extends QueryInput {

        Optional<String> getCollectionId();
        String getTileMatrixSetId();
        double[] getCenter();
        MinMax getZoomLevels();
    }

}
