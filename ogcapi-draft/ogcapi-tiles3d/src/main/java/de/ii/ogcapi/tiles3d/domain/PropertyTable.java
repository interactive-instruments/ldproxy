/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutablePropertyTable.Builder.class)
public interface PropertyTable {

  interface Property {
    int getValues();

    Optional<Integer> getArrayOffsets();

    Optional<Integer> getStringOffsets();

    @Value.Default
    default String getArrayOffsetType() {
      return "UINT32";
    }

    @Value.Default
    default String getStringOffsetType() {
      return "UINT32";
    }

    // TODO these can be a number an array or an array of an array
    Optional<Number> getOffset();

    Optional<Number> getScale();

    Optional<Number> getMin();

    Optional<Number> getMax();
  }

  Optional<String> getName();

  @JsonProperty("class")
  String getClass_();

  int getCount();

  Map<String, Property> getProperties();
}
