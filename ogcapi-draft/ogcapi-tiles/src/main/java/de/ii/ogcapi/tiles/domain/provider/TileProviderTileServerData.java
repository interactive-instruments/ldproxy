/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain.provider;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileProviderTileServerData.Builder;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * # Tile-Provider TILESERVER
 *
 * @langEn With this tile provider, the tiles are obtained from [TileServer-GL
 *     instance](https://github.com/maptiler/tileserver-gl). Only the "WebMercatorQuad" tile scheme
 *     is supported.
 *     <p>In the current version, this provider is only supported in the [Map Tiles](map-tiles.md)
 *     module. Only bitmap tile formats are supported. Seeding or caching are not supported.
 *     <p>This tile provider is experimental and its configuration options may change in future
 *     versions.
 * @langDe Bei diesem Tile-Provider werden die Kacheln über eine
 *     [TileServer-GL-Instanz](https://github.com/maptiler/tileserver-gl) bezogen. Unterstützt wird
 *     nur das Kachelschema "WebMercatorQuad".
 *     <p>In der aktuellen Version wird dieser Provider nur im Modul [Map Tiles](map-tiles.md)
 *     unterstützt. Unterstützt werden nur die Bitmap-Kachelformate. Seeding oder Caching werden
 *     nicht unterstützt.
 *     <p>Dieser Tile-Provider ist experimentell und seine Konfigurationsoptionen können sich in
 *     zukünftigen Versionen ändern.
 */
@Value.Immutable
@JsonDeserialize(builder = Builder.class)
public interface TileProviderTileServerData extends TileProviderData {

  /**
   * @langEn Fixed value, identifies the tile provider type.
   * @langDe Fester Wert, identifiziert die Tile-Provider-Art.
   * @default `TILESERVER`
   */
  default String getType() {
    return "TILESERVER";
  }

  // TODO add optional support for multiple styles once the specification is stable

  /**
   * @langEn URL template for accessing tiles. Parameters to use are `{tileMatrix}`, `{tileRow}`,
   *     `{tileCol}` and `{fileExtension}`.
   * @langDe URL-Template für den Zugriff auf Kacheln. Zu verwenden sind die Parameter
   *     `{tileMatrix}`, `{tileRow}`, `{tileCol}` und `{fileExtension}`.
   * @default `null`
   */
  @Nullable
  public abstract String getUrlTemplate();

  /**
   * @langEn URL template for accessing tiles for a collection.
   * @langDe URL-Template für den Zugriff auf Kacheln für eine Collection.
   * @default `null`
   */
  @Nullable
  public abstract String getUrlTemplateSingleCollection();

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderTileServerData)) return this;

    TileProviderTileServerData src = (TileProviderTileServerData) source;

    ImmutableTileProviderTileServerData.Builder builder =
        new ImmutableTileProviderTileServerData.Builder().from(src).from(this);

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

  abstract class Builder implements EntityDataBuilder<TileProviderTileServerData> {}
}
