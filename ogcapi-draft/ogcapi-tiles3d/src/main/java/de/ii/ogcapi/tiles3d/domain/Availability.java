/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableAvailability.Builder.class)
public interface Availability {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Availability> FUNNEL =
      (from, into) -> {
        from.getBitstream().ifPresent(into::putInt);
        from.getAvailabilityCount().ifPresent(into::putInt);
        from.getConstant().ifPresent(into::putInt);
      };

  Optional<Integer> getBitstream();

  Optional<Integer> getAvailabilityCount();

  Optional<Integer> getConstant();
}
