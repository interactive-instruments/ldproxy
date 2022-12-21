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
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import java.nio.charset.StandardCharsets;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableTileMatrixSetLimitsOgcApi.Builder.class)
public abstract class TileMatrixSetLimitsOgcApi implements TileMatrixSetLimits {

  public static TileMatrixSetLimitsOgcApi of(TileMatrixSetLimits limits) {
    return new ImmutableTileMatrixSetLimitsOgcApi.Builder().from(limits).build();
  }

  @Override
  public abstract String getTileMatrix();

  @Override
  public abstract Integer getMinTileRow();

  @Override
  public abstract Integer getMaxTileRow();

  @Override
  public abstract Integer getMinTileCol();

  @Override
  public abstract Integer getMaxTileCol();

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<TileMatrixSetLimitsOgcApi> FUNNEL =
      (from, into) -> {
        into.putString(from.getTileMatrix(), StandardCharsets.UTF_8);
        into.putInt(from.getMinTileRow());
        into.putInt(from.getMaxTileRow());
        into.putInt(from.getMinTileCol());
        into.putInt(from.getMaxTileCol());
      };
}
