/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutablePublication.Builder.class)
public interface Publication {

  /**
   * @langEn ID of the broker to use for the publication.
   * @langDe ID des Brokers, der für die Veröffentlichung verwendet werden soll.
   */
  String getBroker();

  /**
   * @langEn Dictionary of parameters and their corresponding feature property used in the MQTT
   *     topic.
   * @langDe Verzeichnis der Parameter und deren entsprechende Feature-Eigenschaft, die im
   *     MQTT-Topic verwendet werden.
   * @default {}
   */
  Map<String, String> getParameters();

  /**
   * @langEn Feature property that should be sent in the publication. If not set, the feature will
   *     be sent.
   * @langDe Feature-Eigenschaft, die in der Veröffentlichung gesendet werden soll. Wenn nicht
   *     festgelegt, wird das Feature gesendet.
   */
  Optional<String> getProperty();

  /**
   * @langEn The MQTT QoS value for the messages, `AT_MOST_ONCE` (default), `AT_LEAST_ONCE`, or
   *     `EXACTLY_ONCE`.
   * @langDe Der MQTT QoS-Wert für die Nachrichten, `AT_MOST_ONCE` (Standard), `AT_LEAST_ONCE`, oder
   *     `EXACTLY_ONCE`.
   */
  @Value.Default
  default MqttQos getMqttQos() {
    return MqttQos.AT_MOST_ONCE;
  }

  /**
   * @langEn The timeout in seconds.
   * @langDe Der Timeout in Sekunden.
   * @default 60
   */
  @Value.Default
  default int getTimeout() {
    return 60;
  }

  /**
   * @langEn Flag whether the broker should retain the message.
   * @langDe Schalter, ob der Broker die Nachricht aufbewahren soll.
   * @default false
   */
  @Value.Default
  default boolean getRetain() {
    return false;
  }
}
