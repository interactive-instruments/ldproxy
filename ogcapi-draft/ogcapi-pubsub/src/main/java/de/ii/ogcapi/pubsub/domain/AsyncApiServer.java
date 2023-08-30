/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public interface AsyncApiServer {

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
