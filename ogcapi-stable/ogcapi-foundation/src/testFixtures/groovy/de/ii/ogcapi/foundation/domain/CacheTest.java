/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.cache.domain.Cache;
import java.util.Optional;

public class CacheTest implements Cache {

  @Override
  public boolean has(String... key) {
    return false;
  }

  @Override
  public boolean hasValid(String validator, String... key) {
    return false;
  }

  @Override
  public <T> Optional<T> get(Class<T> clazz, String... key) {
    return Optional.empty();
  }

  @Override
  public <T> Optional<T> get(String validator, Class<T> clazz, String... key) {
    return Optional.empty();
  }

  @Override
  public void put(Object value, String... key) {}

  @Override
  public void put(Object value, int ttl, String... key) {}

  @Override
  public void put(String validator, Object value, String... key) {}

  @Override
  public void put(String validator, Object value, int ttl, String... key) {}

  @Override
  public void del(String... key) {}
}
