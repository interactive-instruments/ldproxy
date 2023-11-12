/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import java.util.Optional;

public class StylesheetContent {

  final String descriptor;
  final byte[] content;
  final boolean inStore;
  final Optional<MbStyleStylesheet> mbStyle;

  public StylesheetContent(byte[] content, String descriptor, boolean inStore) {
    this(content, descriptor, inStore, null);
  }

  public StylesheetContent(
      byte[] content, String descriptor, boolean inStore, MbStyleStylesheet mbStyle) {
    this.content = content;
    this.descriptor = descriptor;
    this.inStore = inStore;
    this.mbStyle = Optional.ofNullable(mbStyle);
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

  public Optional<MbStyleStylesheet> getMbStyle() {
    return mbStyle;
  }
}
