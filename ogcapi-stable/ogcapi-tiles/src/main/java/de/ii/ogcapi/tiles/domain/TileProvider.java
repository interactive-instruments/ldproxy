/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import org.immutables.value.Value;

/**
 * @langEn *Deprecated (from v4.0 on you have to use [Tile Provider](../../providers/tile/README.md)
 *     entities)*
 *     <p>
 *     <p>There are currently three types of Tile providers supported:
 *     <p><code>
 * - `FEATURES`: The tiles are derived from a feature provider.
 * - `MBTILES`: The tiles of a tileset in the "WebMercatorQuad" tiling scheme are available in an MBTiles archive.
 * - `TILESERVER`: The tiles are retrieved from a TileServer GL instance.
 *     </code>
 * @langDe *Deprecated (von v4.0 an müssen [Tile-Provider](../../providers/tile/README.md) Entities
 *     verwendet werden )*<br>
 *     <p>Es werden aktuell drei Arten von Tile-Providern unterstützt:
 *     <p><code>
 * - `FEATURES`: Die Kacheln werden aus einem Feature-Provider abgeleitet.
 * - `MBTILES`: Die Kacheln eines Tileset im Kachelschema "WebMercatorQuad" liegen in einem MBTiles-Archiv vor.
 * - `TILESERVER`: Die Kacheln werden von einer TileServer-GL-Instanz abgerufen.
 *     </code>
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = TileProviderFeatures.class, name = "FEATURES"),
  @JsonSubTypes.Type(value = TileProviderMbtiles.class, name = "MBTILES"),
  @JsonSubTypes.Type(value = TileProviderTileServer.class, name = "TILESERVER")
})
public abstract class TileProvider {

  @JsonIgnore
  @Value.Default
  public boolean tilesMayBeCached() {
    return false;
  }

  @JsonIgnore
  @Value.Default
  public boolean requiresQuerySupport() {
    return true;
  }

  @JsonIgnore
  @Value.Default
  public boolean supportsTilesHints() {
    return false;
  }

  public abstract boolean isMultiCollectionEnabled();

  public abstract boolean isSingleCollectionEnabled();

  public abstract List<String> getTileEncodings();

  public abstract TileProvider mergeInto(TileProvider tileProvider);
}
