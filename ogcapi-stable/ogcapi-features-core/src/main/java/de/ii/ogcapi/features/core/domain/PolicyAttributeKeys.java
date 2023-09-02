/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

import java.util.Objects;

public interface PolicyAttributeKeys {

  String PREFIX_ID = "ldproxy:feature:id";
  String PREFIX_GEO = "ldproxy:feature:geometry";
  String PREFIX_PROPERTY = "ldproxy:feature:property:";
  String KEY_ID = "id";
  String KEY_GEOMETRY = "geometry";
  String KEY_PROPERTIES = "properties";

  static String getFullKey(String key) {
    return Objects.equals(key, KEY_ID)
        ? PREFIX_ID
        : Objects.equals(key, KEY_GEOMETRY) ? PREFIX_GEO : (PREFIX_PROPERTY + key);
  }
}
