/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.gml.app;

import de.ii.xtraplatform.geometries.domain.CoordinatesWriter;
import java.io.IOException;
import java.io.Writer;
import org.immutables.value.Value;

@Value.Immutable
public abstract class CoordinatesWriterGml implements CoordinatesWriter<Writer> {

  @Override
  public void onStart() throws IOException {}

  @Override
  public void onSeparator() throws IOException {
    getDelegate().append(' ');
  }

  @Override
  public void onX(char[] chars, int offset, int length) throws IOException {
    onValue(chars, offset, length, true);
  }

  @Override
  public void onY(char[] chars, int offset, int length) throws IOException {
    onValue(chars, offset, length, getDimension() == 3);
  }

  @Override
  public void onZ(char[] chars, int offset, int length) throws IOException {
    onValue(chars, offset, length, false);
  }

  @Override
  public void onEnd() throws IOException {}

  private void onValue(char[] chars, int offset, int length, boolean separator) throws IOException {
    getDelegate().write(chars, offset, length);

    if (separator) {
      getDelegate().append(' ');
    }
  }
}
