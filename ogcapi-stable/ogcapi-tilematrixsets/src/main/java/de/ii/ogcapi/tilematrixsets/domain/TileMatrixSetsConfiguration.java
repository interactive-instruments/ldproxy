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
import java.util.List;
import java.util.Objects;
import org.immutables.value.Value;

/**
 * @buildingBlock TILE_MATRIX_SETS
 * @langEn ### Custom Tiling Schemes
 *     <p>Additional tile schemas can be configured as JSON files according to the current draft OGC
 *     standard [Two Dimensional Tile Matrix Set and Tile Set Metadata
 *     2.0](https://docs.ogc.org/DRAFTS/17-083r4.html) in the data directory at
 *     `api-resources/tile-matrix-sets/{tileMatrixSetId}.json`.
 * @langDe ### Benutzerdefinierte Kachelschemas
 *     <p>Weitere Kachelschemas können als JSON-Datei gemäß dem aktuellen Entwurf für den
 *     OGC-Standard [Two Dimensional Tile Matrix Set and Tile Set Metadata
 *     2.0](https://docs.ogc.org/DRAFTS/17-083r3.html) im Datenverzeichnis unter
 *     `api-resources/tile-matrix-sets/{tileMatrixSetId}.json` konfiguriert werden.
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTileMatrixSetsConfiguration.Builder.class)
public interface TileMatrixSetsConfiguration extends ExtensionConfiguration, CachingConfiguration {

  enum TileCacheType {
    FILES,
    MBTILES,
    NONE
  }

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
