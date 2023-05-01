/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

public class StylesheetContent {

  final String descriptor;
  final byte[] content;
  final boolean inStore;

  public StylesheetContent(byte[] content, String descriptor, boolean inStore) {
    this.content = content;
    this.descriptor = descriptor;
    this.inStore = inStore;
  }

  public byte[] getContent() {
    return content;
  }

  public String getDescriptor() {
    return descriptor;
  }

  public boolean getInStore() {
    return inStore;
  }
}
