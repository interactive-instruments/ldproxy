/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import de.ii.xtraplatform.entities.domain.ChangingValue;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface ChangingLastModified extends ChangingValue<Instant> {

  static ChangingLastModified of(Instant lastModified) {
    return new ImmutableChangingLastModified.Builder()
        .value(Objects.requireNonNullElse(lastModified, Instant.MIN))
        .build();
  }

  @Override
  default Optional<ChangingValue<Instant>> updateWith(ChangingValue<Instant> delta) {
    Instant deltaInstant = delta.getValue();

    if (!this.getValue().isBefore(deltaInstant)) {
      return Optional.empty();
    }

    return Optional.of(delta);
  }
}
