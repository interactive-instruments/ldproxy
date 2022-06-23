/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true)
@JsonDeserialize(as = ImmutableMbStyleLight.class)
public abstract class MbStyleLight {
  public enum Anchor {
    map,
    viewport
  }

  @Value.Default
  public Anchor getAnchor() {
    return Anchor.viewport;
  }

  public abstract Optional<List<Double>>
      getPosition(); // { return Optional.of(ImmutableList.of(1.15,210.0,30.0)); }

  public abstract Optional<String> getColor(); // { return Optional.of("#ffffff"); }

  // public abstract Optional<String> setColor(Optional<String> color); // { return color; }

  public abstract Optional<Double> getIntensity(); // { return Optional.of(0.5); }
}
