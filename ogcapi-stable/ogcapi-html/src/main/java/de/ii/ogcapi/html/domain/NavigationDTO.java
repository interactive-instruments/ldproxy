/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.domain;

/**
 * @author zahnen
 */
public class NavigationDTO {
  public String label;
  public String url;
  public boolean active;

  public NavigationDTO(String label) {
    this.label = label;
  }

  public NavigationDTO(String label, String url) {
    this.label = label;
    this.url = url;
  }

  public NavigationDTO(String label, boolean active) {
    this.label = label;
    this.active = active;
  }
}
