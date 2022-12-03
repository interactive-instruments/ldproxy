/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableTileMatrixSetLimits.Builder.class)
public abstract class TileMatrixSetLimits {
  public abstract String getTileMatrix();

  public abstract Integer getMinTileRow();

  public abstract Integer getMaxTileRow();

  public abstract Integer getMinTileCol();

  public abstract Integer getMaxTileCol();

  public boolean contains(int row, int col) {
    return getMaxTileCol() >= col
        && getMinTileCol() <= col
        && getMaxTileRow() >= row
        && getMinTileRow() <= row;
  }

  @Value.Derived
  @Value.Auxiliary
  public long getNumberOfTiles() {
    return ((long) getMaxTileRow() - getMinTileRow() + 1) * (getMaxTileCol() - getMinTileCol() + 1);
  }

  public long getNumberOfTiles(IntPredicate whereColMatches) {
    long numCols =
        IntStream.rangeClosed(getMinTileCol(), getMaxTileCol()).filter(whereColMatches).count();

    return (getMaxTileRow() - getMinTileRow() + 1) * numCols;
  }

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<TileMatrixSetLimits> FUNNEL =
      (from, into) -> {
        into.putString(from.getTileMatrix(), StandardCharsets.UTF_8);
        into.putInt(from.getMinTileRow());
        into.putInt(from.getMaxTileRow());
        into.putInt(from.getMinTileCol());
        into.putInt(from.getMaxTileCol());
      };
}
