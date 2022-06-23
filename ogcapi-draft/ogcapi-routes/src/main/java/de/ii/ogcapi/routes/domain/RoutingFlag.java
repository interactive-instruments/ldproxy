/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
@JsonDeserialize(builder = ImmutableRoutingFlag.Builder.class)
public interface RoutingFlag {
  String getLabel();

  @Value.Default
  default boolean getDefault() {
    return false;
  }

  Optional<String> getProviderFlag();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<RoutingFlag> FUNNEL =
      (from, into) -> {
        into.putString(from.getLabel(), StandardCharsets.UTF_8);
        into.putBoolean(from.getDefault());
        from.getProviderFlag().ifPresent(val -> into.putString(val, StandardCharsets.UTF_8));
      };
}
