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
import de.ii.ogcapi.tiles.domain.provider.TileProviderMbtilesData;
import de.ii.ogcapi.tiles.domain.provider.TileProviderTileServerData;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import java.io.InputStream;
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
    SINGLE_LAYER_TILE,
    MULTI_LAYER_TILE,
    TILE_STREAM,
    EMPTY_TILE,
    MBTILES_TILE,
    TILESERVER_TILE
  }

  @Value.Immutable
  interface QueryInputTileEmpty extends QueryInput {

    Tile getTile();
  }

  @Value.Immutable
  interface QueryInputTileStream extends QueryInput {

    Tile getTile();

    InputStream getTileContent();
  }

  @Value.Immutable
  interface QueryInputTileMbtilesTile extends QueryInput {

    Tile getTile();

    TileProviderMbtilesData getProvider();
  }

  @Value.Immutable
  interface QueryInputTileTileServerTile extends QueryInput {

    Tile getTile();

    TileProviderTileServerData getProvider();
  }

  @Value.Immutable
  interface QueryInputTileMultiLayer extends QueryInput {

    Tile getTile();

    Map<String, Tile> getSingleLayerTileMap();

    Map<String, FeatureQuery> getQueryMap();

    EpsgCrs getDefaultCrs();
  }

  @Value.Immutable
  interface QueryInputTileSingleLayer extends QueryInput {

    Tile getTile();

    FeatureQuery getQuery();

    EpsgCrs getDefaultCrs();
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
