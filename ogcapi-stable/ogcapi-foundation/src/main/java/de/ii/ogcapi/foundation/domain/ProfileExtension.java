/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.List;
import java.util.Optional;

@AutoMultiBind
public interface ProfileExtension extends ApiExtension {

  /**
   * @return the URI of the profile
   */
  static String getUri(String value) {
    if (value.startsWith("val")) {
      return String.format("https://def.ldproxy.net/profile/%s", value);
    }
    return String.format("http://www.opengis.net/def/profile/ogc/0/%s", value);
  }

  /**
   * @return the prefix of the profile extension
   */
  String getPrefix();

  /**
   * @return the profile values of the profile extension
   */
  List<String> getValues();

  /**
   * @return a default value, if applicable for all formats
   */
  default Optional<String> getDefault() {
    return Optional.empty();
  }
}
