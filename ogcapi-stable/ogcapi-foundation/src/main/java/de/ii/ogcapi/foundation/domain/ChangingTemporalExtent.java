/**
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
public interface ChangingTemporalExtent extends ChangingValue<TemporalExtent> {

  static ChangingTemporalExtent of(TemporalExtent interval) {
    return new ImmutableChangingTemporalExtent.Builder()
        .value(
            Objects.requireNonNullElse(interval, TemporalExtent.of(Long.MIN_VALUE, Long.MAX_VALUE)))
        .build();
  }

  @Override
  default Optional<ChangingValue<TemporalExtent>> updateWith(ChangingValue<TemporalExtent> delta) {
    TemporalExtent deltaExtent = delta.getValue();
    long currentStart = Objects.requireNonNullElse(getValue().getStart(), Long.MIN_VALUE);
    long currentEnd = Objects.requireNonNullElse(getValue().getEnd(), Long.MAX_VALUE);
    long deltaStart = Objects.requireNonNullElse(deltaExtent.getStart(), Long.MIN_VALUE);
    long deltaEnd = Objects.requireNonNullElse(deltaExtent.getEnd(), Long.MAX_VALUE);

    if (currentStart <= deltaStart && currentEnd >= deltaEnd) return Optional.empty();

    return Optional.of(
        ChangingTemporalExtent.of(
            TemporalExtent.of(Math.min(currentStart, deltaStart), Math.max(currentEnd, deltaEnd))));
  }
}
