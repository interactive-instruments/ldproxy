/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.immutables.value.Value;

@Value.Immutable
public interface PublicationContext {

  Mqtt3AsyncClient getClient();

  CompletableFuture<Mqtt3ConnAck> getConnAck();

  MqttQos getQos();

  String getSubPath();

  Map<String, String> getParameters();

  Optional<String> getProperty();

  @Value.Default
  default int getTimeout() {
    return 60;
  }

  @Value.Default
  default boolean getRetain() {
    return false;
  }
}
