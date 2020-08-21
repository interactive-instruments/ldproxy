/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.tileMatrixSet;

import de.ii.ldproxy.ogcapi.domain.QueriesHandler;
import de.ii.ldproxy.ogcapi.domain.QueryHandler;
import de.ii.ldproxy.ogcapi.domain.QueryIdentifier;
import de.ii.ldproxy.ogcapi.domain.QueryInput;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

public interface TileMatrixSetsQueriesHandler extends QueriesHandler<TileMatrixSetsQueriesHandler.Query> {

    @Override
    Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers();

    enum Query implements QueryIdentifier {TILE_MATRIX_SETS, TILE_MATRIX_SET}

    @Value.Immutable
    interface QueryInputTileMatrixSets extends QueryInput {

        List<TileMatrixSet> getTileMatrixSets();
    }

    @Value.Immutable
    interface QueryInputTileMatrixSet extends QueryInput {

        String getTileMatrixSetId();
    }

}
