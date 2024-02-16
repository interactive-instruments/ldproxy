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
public interface AsyncApiServer {

  @SuppressWarnings("UnstableApiUsage")
  Funnel<AsyncApiServer> FUNNEL =
      (from, into) -> {
        into.putString(from.getProtocol(), StandardCharsets.UTF_8);
        into.putString(from.getProtocolVersion(), StandardCharsets.UTF_8);
        into.putString(from.getUrl(), StandardCharsets.UTF_8);
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getSecurity()
            .forEach(
                s ->
                    s.keySet().stream()
                        .sorted()
                        .forEachOrdered(
                            key ->
                                s.get(key)
                                    .forEach(v -> into.putString(v, StandardCharsets.UTF_8))));
        from.getBindings().ifPresent(v -> AsyncApiServerBindingsMqtt.FUNNEL.funnel(v, into));
      };

  @Value.Default
  default String getProtocol() {
    return "mqtt";
  }

  @Value.Default
  default String getProtocolVersion() {
    return "3.1.1";
  }

  String getUrl();

  Optional<String> getDescription();

  List<Map<String, List<String>>> getSecurity();

  Optional<AsyncApiServerBindingsMqtt> getBindings();
}
