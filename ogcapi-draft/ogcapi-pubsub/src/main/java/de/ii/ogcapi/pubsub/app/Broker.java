/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBroker.Builder.class)
public interface Broker {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<Broker> FUNNEL =
      (from, into) -> {
        into.putString(from.getHost(), StandardCharsets.UTF_8);
        into.putInt(from.getPort());
        from.getUsername().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getPassword().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
      };

  String getHost();

  @Value.Default
  default int getPort() {
    return 1883;
  }

  Optional<String> getUsername();

  Optional<String> getPassword();
}
