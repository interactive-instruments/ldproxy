/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableTileMatrixSetsConfiguration;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsConfiguration;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityMigration;
import java.util.Optional;

public class TileMatrixSetsMigrationV4 extends EntityMigration<OgcApiDataV2, OgcApiDataV2> {

  public TileMatrixSetsMigrationV4(EntityMigrationContext context) {
    super(context);
  }

  @Override
  public String getSubject() {
    return "building block TILE_MATRIX_SETS";
  }

  @Override
  public String getDescription() {
    return "auto-activation is deprecated and will be upgraded to explicit activation";
  }

  @Override
  public boolean isApplicable(EntityData entityData, Optional<EntityData> defaults) {
    if (!(entityData instanceof OgcApiDataV2)) {
      return false;
    }

    OgcApiDataV2 apiData = (OgcApiDataV2) entityData;

    boolean tmsPresentAndEnabled =
        apiData
            .getExtension(TileMatrixSetsConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .isPresent();

    boolean tilesPresentAndEnabled =
        apiData
            .getExtension(TilesConfiguration.class)
            .filter(ExtensionConfiguration::isEnabled)
            .isPresent();

    return tilesPresentAndEnabled && !tmsPresentAndEnabled;
  }

  @Override
  public OgcApiDataV2 migrate(OgcApiDataV2 entityData, Optional<OgcApiDataV2> defaults) {
    Optional<TileMatrixSetsConfiguration> tmsConfigurationOld =
        entityData.getExtension(TileMatrixSetsConfiguration.class);

    TileMatrixSetsConfiguration tmsConfiguration =
        (tmsConfigurationOld.isPresent()
                ? new ImmutableTileMatrixSetsConfiguration.Builder().from(tmsConfigurationOld.get())
                : new ImmutableTileMatrixSetsConfiguration.Builder())
            .enabled(true)
            .build();

    return OgcApiDataV2.replaceOrAddExtensions(entityData, tmsConfiguration);
  }
}
