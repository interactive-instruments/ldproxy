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
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TilesQueriesHandler extends OgcApiQueriesHandler<TilesQueriesHandler.Query> {

    @Override
    Map<Query, OgcApiQueryHandler<? extends OgcApiQueryInput>> getQueryHandlers();

    enum Query implements OgcApiQueryIdentifier {TILE_MATRIX_SETS, TILE_MATRIX_SET}

    @Value.Immutable
    interface OgcApiQueryInputTiles extends OgcApiQueryInput {

        // the query
        FeatureProvider2 getFeatureProvider();
        Optional<String> getCollectionId();
        FeatureQuery getQuery();
        EpsgCrs getDefaultCrs();

        // the processing
        FeatureProcessChain getProcesses();
        Map<String, Object> getProcessingParameters();

    }

    @Value.Immutable
    interface OgcApiQueryInputTileMatrixSets extends OgcApiQueryInput {

        List<TileMatrixSet> getTileMatrixSets();
    }

    @Value.Immutable
    interface OgcApiQueryInputTileMatrixSet extends OgcApiQueryInput {

        String getTileMatrixSetId();
    }

}
