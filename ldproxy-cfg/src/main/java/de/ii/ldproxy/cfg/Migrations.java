/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.ogcapi.tiles.domain.TilesMigrationV4;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityMigration;
import de.ii.xtraplatform.store.domain.entities.EntityMigration.EntityMigrationContext;
import java.util.List;

public interface Migrations {

  static Migrations create(EntityDataStore<?> entityDataStore) {
    EntityMigrationContext context = entityDataStore::has;

    return new Migrations() {
      @Override
      public List<EntityMigration<?, ?>> entity() {
        return List.of(new TilesMigrationV4(context));
      }
    };
  }

  List<EntityMigration<?, ?>> entity();
}
