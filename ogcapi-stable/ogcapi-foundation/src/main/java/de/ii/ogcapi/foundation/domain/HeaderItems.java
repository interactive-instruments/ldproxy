/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface HeaderItems {
  Optional<Long> getNumberMatched();

  Optional<Long> getNumberReturned();

  static HeaderItems of(Optional<Long> numberMatched, Optional<Long> numberReturned) {
    return new ImmutableHeaderItems.Builder()
        .numberMatched(numberMatched)
        .numberReturned(numberReturned)
        .build();
  }

  static HeaderItems of() {
    return new ImmutableHeaderItems.Builder().build();
  }
}
