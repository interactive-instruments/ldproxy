/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.store.domain.Migration;
import de.ii.xtraplatform.store.domain.Migration.MigrationContext;
import java.util.List;

public interface Migrator<T extends MigrationContext, U, V extends Migration<T, U>> {

  boolean isApplicable(V migration);

  List<String> getPreview(V migration);

  void execute(V migration);
}
