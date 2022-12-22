/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import de.ii.ogcapi.foundation.domain.QueriesHandler;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryIdentifier;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileCoordinates;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

public interface TilesQueriesHandler extends QueriesHandler<TilesQueriesHandler.Query> {

  @Override
  Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers();

  enum Query implements QueryIdentifier {
    TILE_SETS,
    TILE_SET,
    TILE,
  }

  @Value.Immutable
  interface QueryInputTile extends QueryInput, TileCoordinates {

    Optional<String> getCollectionId();

    TileFormatExtension getOutputFormat();

    QueryParameterSet getParameters();
  }

  @Value.Immutable
  interface QueryInputTileSets extends QueryInput {

    Optional<String> getCollectionId();

    List<Double> getCenter();

    Map<String, MinMax> getTileMatrixSetZoomLevels();

    String getPath();

    boolean getOnlyWebMercatorQuad();

    List<String> getTileEncodings();
  }

  @Value.Immutable
  interface QueryInputTileSet extends QueryInput {

    Optional<String> getCollectionId();

    String getTileMatrixSetId();

    List<Double> getCenter();

    MinMax getZoomLevels();

    String getPath();
  }
}
