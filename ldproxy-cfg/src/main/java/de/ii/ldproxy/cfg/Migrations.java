/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ogcapi.resources.app.StylesMigrationV4;
import de.ii.ogcapi.text.search.app.QueryablesMigrationV4;
import de.ii.ogcapi.tiles.app.TileMatrixSetsMigrationV4;
import de.ii.ogcapi.tiles.domain.TilesMigrationV4;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.entities.domain.EntityMigration;
import de.ii.xtraplatform.entities.domain.EntityMigration.EntityMigrationContext;
import java.util.List;

public interface Migrations {

  static Migrations create(EntityDataStore<?> entityDataStore) {
    EntityMigrationContext context = entityDataStore::has;

    return () ->
        List.of(
            new TilesMigrationV4(context),
            new TileMatrixSetsMigrationV4(context),
            new QueryablesMigrationV4(context),
            new StylesMigrationV4(context));
  }

  List<EntityMigration<?, ?>> entity();
}
