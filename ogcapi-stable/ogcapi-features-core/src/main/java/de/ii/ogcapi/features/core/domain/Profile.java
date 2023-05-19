/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.domain;

public enum Profile {
  AS_KEY("rel-as-key"),
  AS_LINK("rel-as-link"),
  AS_URI("rel-as-uri");

  public static Profile getDefault() {
    return AS_LINK;
  }

  private final String profileName;

  Profile(String name) {
    this.profileName = name;
  }

  public String getProfileName() {
    return profileName;
  }

  public String getUri() {
    return String.format("http://www.opengis.net/def/profile/ogc/0/%s", profileName);
  }
}
