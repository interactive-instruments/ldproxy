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
 * # HTTP
 *
 * @langEn With this tile provider, the tiles are obtained from [TileServer-GL
 *     instance](https://github.com/maptiler/tileserver-gl).
 * @langDe Bei diesem Tile-Provider werden die Kacheln über eine
 *     [TileServer-GL-Instanz](https://github.com/maptiler/tileserver-gl) bezogen.
 *     <p>{@docTable:properties}
 * @propertyTable {@link de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderHttpData}
 */
@DocFile(
    path = "providers/tile",
    name = "http.md",
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
@JsonDeserialize(builder = ImmutableTileProviderHttpData.Builder.class)
public interface TileProviderHttpData extends TileProviderData {

  String PROVIDER_SUBTYPE = "HTTP";
  String ENTITY_SUBTYPE =
      String.format("%s/%s", TileProviderData.PROVIDER_TYPE, PROVIDER_SUBTYPE).toLowerCase();

  /**
   * @langEn Fixed value, identifies the tile provider type.
   * @langDe Fester Wert, identifiziert die Tile-Provider-Art.
   * @default `HTTP`
   */
  @Override
  default String getTileProviderType() {
    return PROVIDER_SUBTYPE;
  }

  @Value.Default
  @Override
  default ImmutableLayerOptionsHttpDefault getLayerDefaults() {
    return new ImmutableLayerOptionsHttpDefault.Builder().build();
  }

  // TODO: Buildable, merge defaults into layers
  @Override
  Map<String, LayerOptionsHttp> getLayers();

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderHttpData)) return this;

    TileProviderHttpData src = (TileProviderHttpData) source;

    ImmutableTileProviderHttpData.Builder builder =
        new ImmutableTileProviderHttpData.Builder().from(src).from(this);

    /*List<String> tileEncodings =
        Objects.nonNull(src.getTileEncodings())
            ? Lists.newArrayList(src.getTileEncodings())
            : Lists.newArrayList();
    getTileEncodings()
        .forEach(
            tileEncoding -> {
              if (!tileEncodings.contains(tileEncoding)) {
                tileEncodings.add(tileEncoding);
              }
            });
    builder.tileEncodings(tileEncodings);*/

    return builder.build();
  }

  abstract class Builder implements EntityDataBuilder<TileProviderHttpData> {}
}
