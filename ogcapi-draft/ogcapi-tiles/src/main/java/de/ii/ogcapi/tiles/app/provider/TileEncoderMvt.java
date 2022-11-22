/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.provider.ChainedTileProvider;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileQuery;
import de.ii.ogcapi.tiles.domain.provider.LayerOptionsFeatures;
import de.ii.ogcapi.tiles.domain.provider.TileEncoder;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationParametersTransient;
import de.ii.ogcapi.tiles.domain.provider.TileProviderFeaturesData;
import de.ii.ogcapi.tiles.domain.provider.TileQuery;
import de.ii.ogcapi.tiles.domain.provider.TileResult;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileEncoder;

public class TileEncoderMvt implements TileEncoder {

  public TileEncoderMvt() {}

  @Override
  public byte[] empty(TileMatrixSet tms) {
    return new VectorTileEncoder(tms.getTileExtent()).encode();
  }

  @Override
  public byte[] combine(
      TileQuery tile, TileProviderFeaturesData data, ChainedTileProvider tileProvider)
      throws IOException {
    LayerOptionsFeatures combinedLayer = data.getLayers().get(tile.getLayer());
    List<String> subLayers =
        getSubLayers(data, combinedLayer, tile.getGenerationParametersTransient());
    VectorTileEncoder encoder = new VectorTileEncoder(tile.getTileMatrixSet().getTileExtent());
    VectorTileDecoder decoder = new VectorTileDecoder();

    for (String subLayer : subLayers) {
      TileQuery tileQuery = ImmutableTileQuery.builder().from(tile).layer(subLayer).build();
      TileResult subTile = tileProvider.get(tileQuery);

      if (subTile.isError()) {
        // TODO
      }

      if (subTile.isAvailable()) {
        decoder
            .decode(subTile.getContent().get())
            .forEach(
                feature ->
                    encoder.addFeature(
                        feature.getLayerName(),
                        feature.getAttributes(),
                        feature.getGeometry(),
                        feature.getId()));
      }
    }

    return encoder.encode();
  }

  private List<String> getSubLayers(
      TileProviderFeaturesData data,
      LayerOptionsFeatures combinedLayer,
      Optional<TileGenerationParametersTransient> userParameters) {
    return combinedLayer.getCombine().stream()
        .flatMap(
            layer -> {
              if (Objects.equals(layer, LayerOptionsFeatures.COMBINE_ALL)) {
                return data.getLayers().entrySet().stream()
                    .filter(entry -> !entry.getValue().isCombined())
                    .map(Entry::getKey);
              }
              return Stream.of(layer);
            })
        .filter(
            layer ->
                userParameters.isEmpty()
                    || userParameters.get().getLayers().isEmpty()
                    || userParameters.get().getLayers().contains(layer))
        .distinct()
        .collect(Collectors.toList());
  }
}
