/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import com.google.common.collect.Range;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.LayerOptionsFeatures;
import de.ii.ogcapi.tiles.domain.provider.TileEncoder;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import java.io.IOException;
import java.util.Map;
import no.ecc.vectortile.VectorTileEncoder;

public class TileEncoderMvt implements TileEncoder, ChainedTileProvider {
  private final TileProviderFeaturesData data;

  public TileEncoderMvt(TileProviderFeaturesData data) {
    this.data = data;
  }

  @Override
  public Map<String, Range<Integer>> getTmsRanges() {
    // TODO: combination of defaults and all layers
    return data.getLayerDefaults().getTmsRanges();
  }

  @Override
  public boolean canProvide(TileQuery tile) {
    return ChainedTileProvider.super.canProvide(tile)
        && !data.getLayers().get(tile.getLayer()).getCombine().isEmpty();
  }

  @Override
  public TileResult getTile(TileQuery tile) throws IOException {
    return TileResult.found(combine(tile));
  }

  @Override
  public byte[] empty(TileMatrixSet tms) {
    return new VectorTileEncoder(tms.getTileExtent()).encode();
  }

  @Override
  public byte[] combine(TileQuery tile) {
    LayerOptionsFeatures layer = data.getLayers().get(tile.getLayer());

    // TODO: has to be above and beyond the cache at the same time

    return new byte[0];
  }
}
