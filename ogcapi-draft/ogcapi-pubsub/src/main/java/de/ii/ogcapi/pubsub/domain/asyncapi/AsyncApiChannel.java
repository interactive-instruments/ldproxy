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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncApiChannel {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiChannel> FUNNEL =
      (from, into) -> {
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        AsyncApiOperation.FUNNEL.funnel(from.getSubscribe(), into);
        from.getServers().stream()
            .sorted()
            .forEachOrdered(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getParameters().keySet().stream()
            .sorted()
            .forEachOrdered(
                key -> AsyncApiParameter.FUNNEL.funnel(from.getParameters().get(key), into));
        from.getBindings().ifPresent(v -> AsyncApiChannelBindingsMqtt.FUNNEL.funnel(v, into));
      };

  Optional<String> getDescription();

  // Not implemented
  // AsyncApiOperation getPublish();
  AsyncApiOperation getSubscribe();

  List<String> getServers();

  Map<String, AsyncApiParameter> getParameters();

  Optional<AsyncApiChannelBindingsMqtt> getBindings();
}
