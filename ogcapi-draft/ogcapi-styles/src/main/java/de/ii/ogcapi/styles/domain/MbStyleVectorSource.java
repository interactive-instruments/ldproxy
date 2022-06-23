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
@JsonDeserialize(as = ImmutableMbStyleVectorSource.class)
public abstract class MbStyleVectorSource extends MbStyleSource {
  public final String getType() {
    return "vector";
  }

  public abstract Optional<String> getUrl();

  public abstract Optional<List<String>> getTiles();

  public abstract Optional<List<Double>>
      getBounds(); // { return Optional.of(ImmutableList.of(-180.0,-85.051129,180.0,85.051129)); }

  @Value.Default
  public Scheme getScheme() {
    return Scheme.xyz;
  }

  public abstract Optional<Integer> getMinzoom(); // { return Optional.of(0); }

  public abstract Optional<Integer> getMaxzoom(); // { return Optional.of(22); }

  public abstract Optional<String> getAttribution();
}
