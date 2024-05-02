/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import de.ii.ogcapi.foundation.domain.CachingConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.docs.JsonDynamicSubType;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock TILE_MATRIX_SETS
 * @langEn ### Custom Tiling Schemes
 *     <p>Additional tiling schemes can be configured as JSON files according to the [OGC Two
 *     Dimensional Tile Matrix Set and Tile Set Metadata 2.0
 *     Standard](https://docs.ogc.org/is/17-083r4/17-083r4.html) in the data directory at
 *     `api-resources/tile-matrix-sets/{tileMatrixSetId}.json`.
 * @langDe ### Benutzerdefinierte Kachelschemas
 *     <p>Weitere Kachelschemas können als JSON-Datei gemäß dem Standard [OGC Two Dimensional Tile
 *     Matrix Set and Tile Set Metadata 2.0](https://docs.ogc.org/is/17-083r4/17-083r4.html) im
 *     Datenverzeichnis unter `api-resources/tile-matrix-sets/{tileMatrixSetId}.json` konfiguriert
 *     werden.
 * @examplesEn The JSON representation of the pre-defined tiling schemes are available on
 *     [GitHub](https://github.com/interactive-instruments/xtraplatform-spatial/tree/master/xtraplatform-tiles/src/main/resources/tilematrixsets).
 *     <p>The list of tiling schemas does not need to be configured, the list is derived from the
 *     configuration of the tile providers used in the TILES building block.
 *     <p>An explicit configuration for an API that provides tiles in three tiling schemas could be the following:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILE_MATRIX_SETS
 *   enabled: true
 *   includePredefined:
 *   - WebMercatorQuad
 *   - WorldCRS84Quad
 *   - WorldMercatorWGS84Quad
 * ```
 *     </code>
 * @examplesDe Die vordefinierten Kachelschemas in JSON sind auf
 *     [GitHub](https://github.com/interactive-instruments/xtraplatform-spatial/tree/master/xtraplatform-tiles/src/main/resources/tilematrixsets)
 *     verfügbar.
 *     <p>Die Liste der Kachelschemas muss nicht explizit konfiguriert werden, die Liste wird aus der Konfiguration des im TILES-Baustein verwendeten Tile Providers abgeleitet.
 *     <p>Eine explizite Konfiguration für eine API, die Kacheln in drei Kachelschemas bereitstellt, könnte wie folgt aussehen:
 *     <p><code>
 * ```yaml
 * - buildingBlock: TILE_MATRIX_SETS
 *   enabled: true
 *   includePredefined:
 *   - WebMercatorQuad
 *   - WorldCRS84Quad
 *   - WorldMercatorWGS84Quad
 * ```
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDynamicSubType(superType = ExtensionConfiguration.class, id = "TILE_MATRIX_SETS")
@JsonDeserialize(builder = ImmutableTileMatrixSetsConfiguration.Builder.class)
public interface TileMatrixSetsConfiguration extends ExtensionConfiguration, CachingConfiguration {

  enum TileCacheType {
    FILES,
    MBTILES,
    NONE
  }

  /**
   * @default Tiles.enabled()
   */
  @Nullable
  @Override
  Boolean getEnabled();

  /**
   * @langEn The list of pre-defined tile matrix sets that are included in the API. All tile matrix
   *     sets used by the tile provider of the API are automatically added to the list.
   * @langDe Die Liste der vordefinierten Kachelschemas, die in der API enthalten sind. Alle
   *     Kachelschemas, die vom Tile Provider der API verwendet werden, werden automatisch
   *     hinzugefügt.
   * @default []
   */
  List<String> getIncludePredefined();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return new ImmutableTileMatrixSetsConfiguration.Builder();
  }

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    ImmutableTileMatrixSetsConfiguration.Builder builder =
        ((ImmutableTileMatrixSetsConfiguration.Builder) source.getBuilder())
            .from(source)
            .from(this);

    TileMatrixSetsConfiguration src = (TileMatrixSetsConfiguration) source;

    List<String> includes =
        Objects.nonNull(src.getIncludePredefined())
            ? Lists.newArrayList(src.getIncludePredefined())
            : Lists.newArrayList();
    getIncludePredefined()
        .forEach(
            include -> {
              if (!includes.contains(include)) {
                includes.add(include);
              }
            });
    builder.includePredefined(includes);

    return builder.build();
  }
}
