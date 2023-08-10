/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * @buildingBlock PUB_SUB
 * @examplesAll <code>
 * ```yaml
 * - buildingBlock: PUB_SUB
 *   enabled: true
 *   broker:
 *     eclipse:
 *       host: mqtt.eclipseprojects.io
 *   publisher: t19.ldproxy.net
 *   publications:
 *     - broker: eclipse
 *       mqttQos: AT_MOST_ONCE
 * ```
 * </code>
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutablePubSubConfiguration.Builder.class)
public interface PubSubConfiguration extends ExtensionConfiguration {

  /** TODO */
  Map<String, Broker> getBrokers();

  /** TODO */
  @Nullable
  String getPublisher();

  /** TODO */
  List<Publication> getPublications();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return ImmutablePubSubConfiguration.builder();
  }
}
