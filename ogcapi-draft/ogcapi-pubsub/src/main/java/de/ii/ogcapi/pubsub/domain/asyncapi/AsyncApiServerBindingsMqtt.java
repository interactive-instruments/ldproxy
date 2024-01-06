/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain.asyncapi;

import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncApiServerBindingsMqtt {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiServerBindingsMqtt> FUNNEL =
      (from, into) -> {
        from.getClientId().ifPresent(v -> into.putString(v, StandardCharsets.UTF_8));
        from.getCleanSession().ifPresent(into::putBoolean);
        from.getKeepAlive().ifPresent(into::putInt);
        into.putString(from.getBindingVersion(), StandardCharsets.UTF_8);
      };

  Optional<String> getClientId();

  Optional<Boolean> getCleanSession();

  Optional<Integer> getKeepAlive();

  default String getBindingVersion() {
    return "0.1.0";
  }
}
