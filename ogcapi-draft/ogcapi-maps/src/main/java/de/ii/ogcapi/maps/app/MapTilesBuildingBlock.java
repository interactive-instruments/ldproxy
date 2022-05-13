/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.maps.domain.ImmutableMapTilesConfiguration;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * # Modul "Map Tiles" (MAP_TILES)
 * @langEn The " Map Tiles" module activates the "Map Tilesets", "Map Tileset" and "Map Tile" resources.
 *
 * The module is based on the drafts of [OGC API - Maps](https://github.com/opengeospatial/OGC-API-Maps).
 * The implementation will change as the drafts are further standardized.
 *
 * The supported tile formats are:
 *
 * - PNG
 * - WebP
 * - JPEG
 *
 * Only the [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62) tiling scheme is available.
 * @langDe Das Modul "Map Tiles" aktiviert die Ressourcen "Map Tilesets", "Map Tileset" und "Map Tile".
 *
 * Das Modul basiert auf den Entwürfen von [OGC API - Maps](https://github.com/opengeospatial/OGC-API-Maps).
 * Die Implementierung wird sich im Zuge der weiteren Standardisierung der Entwürfe noch ändern.
 *
 * Die unterstützten Kachelformate sind:
 *
 * - PNG
 * - WebP
 * - JPEG
 *
 * Es steht nur das Kachelschema [WebMercatorQuad](http://docs.opengeospatial.org/is/17-083r2/17-083r2.html#62) zur Verfügung.
 * @propertyTable {@link de.ii.ogcapi.maps.domain.MapTilesConfiguration}
 * @endpointTable {@link de.ii.ogcapi.maps.infra.EndpointMapTileMultiCollection},
 * {@link de.ii.ogcapi.maps.infra.EndpointMapTileSetMultiCollection},
 * {@link de.ii.ogcapi.maps.infra.EndpointMapTileSetSingleCollection},
 * {@link de.ii.ogcapi.maps.infra.EndpointMapTileSetsMultiCollection},
 * {@link de.ii.ogcapi.maps.infra.EndpointMapTileSetsSingleCollection},
 * {@link de.ii.ogcapi.maps.infra.EndpointMapTileSingleCollection}
 */
@Singleton
@AutoBind
public class MapTilesBuildingBlock implements ApiBuildingBlock {

    @Inject
    public MapTilesBuildingBlock() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {

        return new ImmutableMapTilesConfiguration.Builder()
            .enabled(false)
            .build();
    }

    @Override
    public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
        // since building block / capability components are currently always enabled,
        // we need to test, if the TILES and MAP_TILES modules are enabled for the API and stop, if not
        if (!api.getData().getExtension(MapTilesConfiguration.class)
                    .map(ExtensionConfiguration::isEnabled)
                    .orElse(false) ||
            !api.getData().getExtension(TilesConfiguration.class)
                .map(ExtensionConfiguration::isEnabled)
                .orElse(false)) {
            return ValidationResult.of();
        }

        if (apiValidation== MODE.NONE) {
            return ValidationResult.of();
        }

        ImmutableValidationResult.Builder builder = ImmutableValidationResult.builder()
                .mode(apiValidation);

        // TODO
        // check url templates (string, mandatory, no default): a URL template for the access to the map tile from the tileserver-gl deployment. The URL template must include the parameters tileMatrix, tileRow, tileCol and fileExtension;
        // check tile encodings (string array, default is ["PNG"]) with "PNG", "JPG" and "WebP" as recognized values.

        return builder.build();
    }
}
