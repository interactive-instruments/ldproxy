/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.store.domain.entities.ChangingValue;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface ChangingItemCount extends ChangingValue<Long> {

  static ChangingItemCount of(long itemCount) {
    return new ImmutableChangingItemCount.Builder()
        .value(Objects.requireNonNullElse(itemCount, 0L))
        .build();
  }

  @Override
  default Optional<ChangingValue<Long>> updateWith(ChangingValue<Long> delta) {
    Long deltaCount = delta.getValue();

    if (deltaCount == 0) {
      return Optional.empty();
    }

    return Optional.of(ChangingItemCount.of(this.getValue() + deltaCount));
  }
}
