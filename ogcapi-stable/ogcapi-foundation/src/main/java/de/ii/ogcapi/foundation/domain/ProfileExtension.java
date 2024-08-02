/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;

@AutoMultiBind
public interface ProfileExtension extends ApiExtension {

  /**
   * @return the name of the profile used in the query parameter "profile"
   */
  String getName();

  /**
   * @return the URI of the profile
   */
  default String getUri() {
    return String.format("http://www.opengis.net/def/profile/ogc/0/%s", getName());
  }

  /**
   * @return {@code true}, if the profile should be enabled by default
   */
  default boolean isEnabledByDefault() {
    return false;
  }
}
