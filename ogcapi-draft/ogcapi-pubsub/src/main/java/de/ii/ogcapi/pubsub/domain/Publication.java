/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutablePublication.Builder.class)
public interface Publication {

  // TODO which topics

  String getBroker();

  @Value.Default
  default MqttQos getMqttQos() {
    return MqttQos.AT_MOST_ONCE;
  }
}
