/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import java.util.Map;
import java.util.Objects;
import org.immutables.value.Value;

/**
 * # MbTiles
 *
 * @langEn With this tile provider, the tiles are provided via an [MBTiles
 *     file](https://github.com/mapbox/mbtiles-spec). The tile format and all other properties of
 *     the tileset resource are derived from the contents of the MBTiles file. Only the
 *     "WebMercatorQuad" tiling scheme is supported.
 * @langDe Bei diesem Tile-Provider werden die Kacheln über eine
 *     [MBTiles-Datei](https://github.com/mapbox/mbtiles-spec) bereitgestellt. Das Kachelformat und
 *     alle anderen Eigenschaften der Tileset-Ressource ergeben sich aus dem Inhalt der
 *     MBTiles-Datei. Unterstützt wird nur das Kachelschema "WebMercatorQuad".
 *     <p>{@docTable:properties}
 * @propertyTable {@link de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderMbtilesData}
 */
@DocFile(
    path = "providers/tile",
    name = "mbtiles.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    })
@Value.Immutable
@JsonDeserialize(builder = ImmutableTileProviderMbtilesData.Builder.class)
public interface TileProviderMbtilesData extends TileProviderData {

  String PROVIDER_SUBTYPE = "MBTILES";
  String ENTITY_SUBTYPE =
      String.format("%s/%s", TileProviderData.PROVIDER_TYPE, PROVIDER_SUBTYPE).toLowerCase();

  /**
   * @langEn Fixed value, identifies the tile provider type.
   * @langDe Fester Wert, identifiziert die Tile-Provider-Art.
   * @default `MBTILES`
   */
  @Override
  default String getTileProviderType() {
    return PROVIDER_SUBTYPE;
  }

  // TODO: error when using interface
  @Value.Default
  @Override
  default ImmutableLayerOptionsMbTilesDefault getLayerDefaults() {
    return new ImmutableLayerOptionsMbTilesDefault.Builder().build();
  }

  // TODO: Buildable, merge defaults into layers
  @Override
  Map<String, LayerOptionsMbTiles> getLayers();

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderMbtilesData)) return this;

    TileProviderMbtilesData src = (TileProviderMbtilesData) source;

    ImmutableTileProviderMbtilesData.Builder builder =
        new ImmutableTileProviderMbtilesData.Builder().from(src).from(this);

    // if (!getCenter().isEmpty()) builder.center(getCenter());
    // else if (!src.getCenter().isEmpty()) builder.center(src.getCenter());

    return builder.build();
  }

  abstract class Builder implements EntityDataBuilder<TileProviderMbtilesData> {}
}
