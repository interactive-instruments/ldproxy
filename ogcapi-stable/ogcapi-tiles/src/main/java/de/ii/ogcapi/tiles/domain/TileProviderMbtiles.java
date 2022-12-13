/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.VectorLayer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * # Tile-Provider MBTILES
 *
 * @langEn With this tile provider, the tiles are provided via an [MBTiles
 *     file](https://github.com/mapbox/mbtiles-spec). The tile format and all other properties of
 *     the tileset resource are derived from the contents of the MBTiles file. Only the
 *     "WebMercatorQuad" tiling scheme is supported.
 * @langDe Bei diesem Tile-Provider werden die Kacheln über eine
 *     [MBTiles-Datei](https://github.com/mapbox/mbtiles-spec) bereitgestellt. Das Kachelformat und
 *     alle anderen Eigenschaften der Tileset-Ressource ergeben sich aus dem Inhalt der
 *     MBTiles-Datei. Unterstützt wird nur das Kachelschema "WebMercatorQuad".
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableTileProviderMbtiles.Builder.class)
public abstract class TileProviderMbtiles extends TileProvider {

  /**
   * @langEn Fixed value, identifies the tile provider type.
   * @langDe Fester Wert, identifiziert die Tile-Provider-Art.
   * @default `MBTILES`
   */
  @Value.Derived
  public String getType() {
    return "MBTILES";
  }

  /**
   * @langEn Filename of the MBTiles file in the `api-resources/tiles/{apiId}` directory.
   * @langDe Dateiname der MBTiles-Datei im Verzeichnis `api-resources/tiles/{apiId}`.
   * @default `null`
   */
  @Nullable
  public abstract String getFilename();

  /**
   * @langEn Tiling scheme used in the MBTiles file.
   * @langDe Kachelschema, das in der MBTiles-Datei verwendet wird.
   * @default `WebMercatorQuad`
   */
  @Value.Default
  public String getTileMatrixSetId() {
    return "WebMercatorQuad";
  }

  @JsonIgnore
  public abstract Map<String, MinMax> getZoomLevels();

  @Nullable
  @JsonIgnore
  public abstract String getTileEncoding();

  @JsonIgnore
  @Value.Auxiliary
  @Value.Derived
  public List<String> getTileEncodings() {
    return Objects.nonNull(getTileEncoding())
        ? ImmutableList.of(getTileEncoding())
        : ImmutableList.of();
  }

  @JsonIgnore
  public abstract List<Double> getCenter();

  @JsonIgnore
  public abstract Optional<BoundingBox> getBounds();

  @JsonIgnore
  public abstract List<VectorLayer> getVectorLayers();

  @Override
  @JsonIgnore
  @Value.Default
  public boolean requiresQuerySupport() {
    return false;
  }

  @Override
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  public boolean isMultiCollectionEnabled() {
    return true;
  }

  @Override
  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  public boolean isSingleCollectionEnabled() {
    return false;
  }

  @Override
  public TileProvider mergeInto(TileProvider source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderMbtiles)) return this;

    TileProviderMbtiles src = (TileProviderMbtiles) source;

    ImmutableTileProviderMbtiles.Builder builder =
        ImmutableTileProviderMbtiles.builder().from(src).from(this);

    if (!getCenter().isEmpty()) builder.center(getCenter());
    else if (!src.getCenter().isEmpty()) builder.center(src.getCenter());

    Map<String, MinMax> mergedZoomLevels =
        Objects.nonNull(src.getZoomLevels())
            ? Maps.newLinkedHashMap(src.getZoomLevels())
            : Maps.newLinkedHashMap();
    if (Objects.nonNull(getZoomLevels())) getZoomLevels().forEach(mergedZoomLevels::put);
    builder.zoomLevels(mergedZoomLevels);

    return builder.build();
  }
}
