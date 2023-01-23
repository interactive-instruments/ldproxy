/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.tiles.domain.TileProvider;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock MAP_TILES
 * @examplesEn Example configuration:
 *     <p><code>
 * ```yaml
 * - buildingBlock: MAP_TILES
 *   enabled: true
 *   mapProvider:
 *     type: TILESERVER
 *     urlTemplate: 'https://www.example.com/tileserver/styles/topographic/{tileMatrix}/{tileCol}/{tileRow}@2x.{fileExtension}'
 *     tileEncodings:
 *       - WebP
 *       - PNG
 * ```
 *     </code>
 *     <p>An example of a TileServer-GL configuration with the style "topographic", which can use,
 *     e.g., the vector tiles provided by the API as the data source:
 *     <p><code>
 * ```json
 * {
 *   "options": {},
 *   "styles": {
 *     "topographic": {
 *       "style": "topographic.json",
 *       "tilejson": {
 *         "type": "overlay",
 *         "bounds": [35.7550727, 32.3573507, 37.2052764, 33.2671397]
 *       }
 *     }
 *   },
 *   "data": {}
 * }
 * ```
 *     </code>
 * @examplesDe Beispielkonfiguration:
 *     <p><code>
 * ```yaml
 * - buildingBlock: MAP_TILES
 *   enabled: true
 *   mapProvider:
 *     type: TILESERVER
 *     urlTemplate: 'https://www.example.com/tileserver/styles/topographic/{tileMatrix}/{tileCol}/{tileRow}@2x.{fileExtension}'
 *     tileEncodings:
 *       - WebP
 *       - PNG
 * ```
 *     </code>
 *     <p>Ein Beispiel für eine TileServer-GL-Konfiguration mit dem Style "topographic", der z.B.
 *     als Datenquelle die Vector Tiles der API verwenden kann:
 *     <p><code>
 * ```json
 * {
 *   "options": {},
 *   "styles": {
 *     "topographic": {
 *       "style": "topographic.json",
 *       "tilejson": {
 *         "type": "overlay",
 *         "bounds": [35.7550727, 32.3573507, 37.2052764, 33.2671397]
 *       }
 *     }
 *   },
 *   "data": {}
 * }
 * ```
 *     </code>
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableMapTilesConfiguration.Builder.class)
public interface MapTilesConfiguration extends ExtensionConfiguration {

  abstract class Builder extends ExtensionConfiguration.Builder {}

  /**
   * @langEn Specifies the data source for the tiles, currently only
   *     [TileServer-Tile-Provider](tiles.md#tileserver) is supported.
   * @langDe Spezifiziert die Datenquelle für die Kacheln, unterstützt werden derzeit nur
   *     [TileServer-Tile-Provider](tiles.md#tileserver).
   * @default null
   */
  @Nullable
  TileProvider getMapProvider(); // TODO: must be TileServer, generalize and extend to MBTiles

  @JsonIgnore
  @Nullable
  TilesConfiguration.TileCacheType getCache(); // TODO: add caching support

  @Value.Auxiliary
  @Value.Derived
  @JsonIgnore
  default List<String> getTileEncodingsDerived() {
    if (Objects.isNull(getMapProvider())) return ImmutableList.of();
    return Objects.requireNonNullElse(getMapProvider().getTileEncodings(), ImmutableList.of());
  }

  @JsonIgnore
  @Value.Auxiliary
  @Value.Derived
  default boolean isMultiCollectionEnabled() {
    if (Objects.isNull(getMapProvider())) return false;
    return getMapProvider().isMultiCollectionEnabled();
  }

  @JsonIgnore
  @Value.Auxiliary
  @Value.Derived
  default boolean isSingleCollectionEnabled() {
    if (Objects.isNull(getMapProvider())) return false;
    return getMapProvider().isSingleCollectionEnabled();
  }

  @Override
  default MapTilesConfiguration.Builder getBuilder() {
    return new ImmutableMapTilesConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    if (Objects.isNull(source) || !(source instanceof MapTilesConfiguration)) return this;

    MapTilesConfiguration src = (MapTilesConfiguration) source;

    ImmutableMapTilesConfiguration.Builder builder =
        new ImmutableMapTilesConfiguration.Builder().from(src).from(this);

    return builder.build();
  }
}
