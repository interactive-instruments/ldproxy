/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.tiles3d.domain.TileResourceDescriptor;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
abstract class SubtreeWalkerResponse {

  static SubtreeWalkerResponse of() {
    return ImmutableSubtreeWalkerResponse.builder().build();
  }

  static SubtreeWalkerResponse of(List<TileResourceDescriptor> next) {
    return ImmutableSubtreeWalkerResponse.builder().next(next).build();
  }

  static SubtreeWalkerResponse of(List<TileResourceDescriptor> next, long skipped) {
    return ImmutableSubtreeWalkerResponse.builder().next(next).skipped(skipped).build();
  }

  static SubtreeWalkerResponse of(long skipped) {
    return ImmutableSubtreeWalkerResponse.builder().skipped(skipped).build();
  }

  @Value.Default
  List<TileResourceDescriptor> getNext() {
    return ImmutableList.of();
  }

  @Value.Default
  long getSkipped() {
    return 0L;
  }
}
