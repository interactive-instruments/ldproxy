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
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import de.ii.ogcapi.pubsub.domain.ImmutablePublicationContext.Builder;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = Builder.class)
public interface PublicationContext {

  Mqtt3BlockingClient getClient();

  MqttQos getQos();
}
