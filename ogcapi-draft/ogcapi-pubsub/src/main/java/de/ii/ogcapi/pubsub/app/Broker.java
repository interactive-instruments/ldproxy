/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBroker.Builder.class)
public interface Broker {

  /**
   * @langEn The hostname of the broker.
   * @langDe Der Hostname des Brokers.
   */
  String getHost();

  /**
   * @langEn The port used to connect the broker.
   * @langDe Der Port für die Verbindung zum Brokers.
   * @default 1883
   */
  @Value.Default
  default int getPort() {
    return 1883;
  }

  /**
   * @langEn Flag, if the connection should use SSL.
   * @langDe Schalter, ob die Verbindung SSL verwenden soll.
   * @default true, if port=8883, otherwise false
   */
  @Value.Default
  default boolean getSsl() {
    return getPort() == 8883;
  }

  /**
   * @langEn Optional username, if credentials are required.
   * @langDe Optionaler Benutzername, wenn Anmeldedaten erforderlich sind.
   */
  Optional<String> getUsername();

  /**
   * @langEn Optional password, if credentials are required.
   * @langDe Optionales Passwort, wenn Anmeldedaten erforderlich sind.
   */
  Optional<String> getPassword();
}
