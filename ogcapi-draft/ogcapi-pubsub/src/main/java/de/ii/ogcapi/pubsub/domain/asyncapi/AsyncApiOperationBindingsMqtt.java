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
public interface AsyncApiOperationBindingsMqtt {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiOperationBindingsMqtt> FUNNEL =
      (from, into) -> {
        from.getQos().ifPresent(into::putInt);
        from.getRetain().ifPresent(into::putBoolean);
        into.putString(from.getBindingVersion(), StandardCharsets.UTF_8);
      };

  Optional<Integer> getQos();

  Optional<Boolean> getRetain();

  default String getBindingVersion() {
    return "0.1.0";
  }
}
