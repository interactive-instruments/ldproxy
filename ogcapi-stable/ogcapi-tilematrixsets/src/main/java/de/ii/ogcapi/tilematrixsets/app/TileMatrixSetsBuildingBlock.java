/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableTileMatrixSetsConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Tile Matrix Sets
 * @langEn The *Tile Matrix Sets* module enables the "Tile Matrix Sets" and "Tile Matrix Set"
 *     resources.
 *     <p>The module is based on the draft of [OGC Two Dimensional Tile Matrix Set and Tile Set
 *     Metadata](https://docs.ogc.org/DRAFTS/17-083r4.html). The implementation will change as the
 *     draft is further standardized.
 *     <p>As preconfigured tiling schemes are available:
 *     <p>- [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62) -
 *     [WorldCRS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#63) -
 *     [WorldMercatorWGS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#64) -
 *     AdV_25832 (Tiling scheme of the AdV for Germany) - EU_25832 (Tiling scheme of the BKG, based
 *     on AdV_25832, extended to Europe) - gdi_de_25832 (tile scheme recommended by the GDI-DE)
 *     <p>Additional tile schemas can be configured as JSON files according to the current draft OGC
 *     standard [Two Dimensional Tile Matrix Set and Tile Set Metadata
 *     2.0](https://docs.ogc.org/DRAFTS/17-083r4.html) in the data directory at
 *     `api-resources/tile-matrix-sets/{tileMatrixSetId}.json`.
 * @langDe Das Modul *Tile Matrix Sets* aktiviert die Ressourcen "Tile Matrix Sets" und "Tile Matrix
 *     Set".
 *     <p>Das Modul basiert auf dem Entwurf von [OGC Two Dimensional Tile Matrix Set and Tile Set
 *     Metadata](https://docs.ogc.org/DRAFTS/17-083r4.html). Die Implementierung wird sich im Zuge
 *     der weiteren Standardisierung des Entwurfs noch ändern.
 *     <p>Als vorkonfigurierte Kachelschemas stehen zur Verfügung:
 *     <p>- [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62) -
 *     [WorldCRS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#63) -
 *     [WorldMercatorWGS84Quad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#64) -
 *     AdV_25832 (Kachelschema der AdV für Deutschland) - EU_25832 (Kachelschema des BKG, basierend
 *     auf AdV_25832, erweitert auf Europa) - gdi_de_25832 (von der GDI-DE empfohlenes Kachelschema)
 *     <p>Weitere Kachelschemas können als JSON-Datei gemäß dem aktuellen Entwurf für den
 *     OGC-Standard [Two Dimensional Tile Matrix Set and Tile Set Metadata
 *     2.0](https://docs.ogc.org/DRAFTS/17-083r3.html) im Datenverzeichnis unter
 *     `api-resources/tile-matrix-sets/{tileMatrixSetId}.json` konfiguriert werden.
 * @example {@link de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsConfiguration}
 * @propertyTable {@link de.ii.ogcapi.tilematrixsets.domain.ImmutableTileMatrixSetsConfiguration}
 * @endpointTable {@link de.ii.ogcapi.tilematrixsets.infra.EndpointTileMatrixSets}
 * @queryParameter {@link de.ii.ogcapi.tilematrixsets.app.QueryParameterFTileMatrixSets}
 */
@Singleton
@AutoBind
public class TileMatrixSetsBuildingBlock implements ApiBuildingBlock {

  @Inject
  public TileMatrixSetsBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {

    return new ImmutableTileMatrixSetsConfiguration.Builder().enabled(false).build();
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    // since building block / capability components are currently always enabled,
    // we need to test, if the TILE MATRIX SETS module is enabled for the API and stop, if not
    OgcApiDataV2 apiData = api.getData();
    if (!apiData
        .getExtension(TileMatrixSetsConfiguration.class)
        .map(ExtensionConfiguration::isEnabled)
        .orElse(false)) {
      return ValidationResult.of();
    }

    ImmutableValidationResult.Builder builder =
        ImmutableValidationResult.builder().mode(apiValidation);

    return builder.build();
  }
}
