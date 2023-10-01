/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityFactories;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Utility to read, write, validate and migrate ldproxy YAML configuration files. */
public interface LdproxyCfg extends LdproxyCfgWriter {

  /**
   * Create a new instance for the given FS store.
   *
   * @param store the root directory of an FS store
   * @return the new {@link LdproxyCfg}
   */
  static LdproxyCfg create(Path store) throws IOException {
    LdproxyCfgImpl ldproxyCfg = new LdproxyCfgImpl(store, true);
    ldproxyCfg.init();

    return ldproxyCfg;
  }

  void initStore();

  Path getDataDirectory();

  Path getEntitiesPath();

  ObjectMapper getObjectMapper();

  EntityDataDefaultsStore getEntityDataDefaultsStore();

  EntityDataStore<EntityData> getEntityDataStore();

  EntityFactories getEntityFactories();

  List<Identifier> getEntityIdentifiers();

  void ignoreEventsFor(String type, Identifier identifier);

  Migrations migrations();

  Set<ValidationMessage> validateEntity(Path entityPath, String entityType) throws IOException;
}
