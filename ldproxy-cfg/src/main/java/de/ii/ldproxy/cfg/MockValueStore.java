/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.values.domain.KeyValueStore;
import de.ii.xtraplatform.values.domain.StoredValue;
import de.ii.xtraplatform.values.domain.ValueStore;
import de.ii.xtraplatform.values.domain.Values;
import java.util.concurrent.CompletableFuture;

public class MockValueStore implements ValueStore {

  @Override
  public <U extends StoredValue> KeyValueStore<U> forTypeWritable(Class<U> type) {
    return null;
  }

  @Override
  public <U extends StoredValue> Values<U> forType(Class<U> type) {
    return null;
  }

  @Override
  public CompletableFuture<Void> onReady() {
    return CompletableFuture.completedFuture(null);
  }
}
