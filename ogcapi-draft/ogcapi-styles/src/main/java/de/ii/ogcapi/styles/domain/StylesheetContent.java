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
  final Optional<Tiles3dStylesheet> tiles3dStyle;

  public StylesheetContent(byte[] content, String descriptor, boolean inStore) {
    this(content, descriptor, inStore, null, null);
  }

  public StylesheetContent(
      byte[] content, String descriptor, boolean inStore, MbStyleStylesheet mbStyle) {
    this(content, descriptor, inStore, mbStyle, null);
  }

  public StylesheetContent(
      byte[] content, String descriptor, boolean inStore, Tiles3dStylesheet tiles3dStyle) {
    this(content, descriptor, inStore, null, tiles3dStyle);
  }

  public StylesheetContent(
      byte[] content,
      String descriptor,
      boolean inStore,
      MbStyleStylesheet mbStyle,
      Tiles3dStylesheet tiles3dStyle) {
    this.content = content;
    this.descriptor = descriptor;
    this.inStore = inStore;
    this.mbStyle = Optional.ofNullable(mbStyle);
    this.tiles3dStyle = Optional.ofNullable(tiles3dStyle);
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

  public Optional<Tiles3dStylesheet> get3dTilesStyle() {
    return tiles3dStyle;
  }
}
