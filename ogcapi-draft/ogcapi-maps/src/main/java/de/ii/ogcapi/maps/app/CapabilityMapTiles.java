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
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.maps.domain.ImmutableMapTilesConfiguration;
import de.ii.ogcapi.maps.domain.MapTilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@AutoBind
public class CapabilityMapTiles implements ApiBuildingBlock {

    @Inject
    public CapabilityMapTiles() {
    }

    @Override
    public ExtensionConfiguration getDefaultConfiguration() {

        return new ImmutableMapTilesConfiguration.Builder()
            .enabled(false)
            .build();
    }

    @Override
    public ValidationResult onStartup(OgcApiDataV2 apiData, MODE apiValidation) {
        // since building block / capability components are currently always enabled,
        // we need to test, if the TILES and MAP_TILES modules are enabled for the API and stop, if not
        if (!apiData.getExtension(MapTilesConfiguration.class)
                    .map(ExtensionConfiguration::getEnabled)
                    .orElse(false) ||
            !apiData.getExtension(TilesConfiguration.class)
                .map(ExtensionConfiguration::getEnabled)
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
