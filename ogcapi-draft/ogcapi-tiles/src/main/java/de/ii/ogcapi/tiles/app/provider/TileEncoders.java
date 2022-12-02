/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.TileEncoder;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MediaType;

public class TileEncoders implements ChainedTileProvider {
  private static final Map<MediaType, TileEncoder> ENCODERS =
      ImmutableMap.of(FeatureEncoderMVT.FORMAT, new TileEncoderMvt());
  private final TileProviderFeaturesData data;
  private final ChainedTileProvider generatorProviderChain;

  public TileEncoders(TileProviderFeaturesData data, ChainedTileProvider generatorProviderChain) {
    this.data = data;
    this.generatorProviderChain = generatorProviderChain;
  }

  @Override
  public Map<String, Map<String, Range<Integer>>> getTmsRanges() {
    return data.getTmsRanges();
  }

  @Override
  public boolean canProvide(TileQuery tile) {
    return ChainedTileProvider.super.canProvide(tile)
        && !data.getLayers().get(tile.getLayer()).getCombine().isEmpty()
        && canEncode(tile.getMediaType());
  }

  @Override
  public TileResult getTile(TileQuery tile) throws IOException {
    return TileResult.found(combine(tile));
  }

  public boolean canEncode(MediaType mediaType) {
    return ENCODERS.containsKey(mediaType);
  }

  public byte[] empty(MediaType mediaType, TileMatrixSet tms) {
    return ENCODERS.get(mediaType).empty(tms);
  }

  public byte[] combine(TileQuery tile) throws IOException {
    return ENCODERS.get(tile.getMediaType()).combine(tile, data, generatorProviderChain);
  }
}
