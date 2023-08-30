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
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

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
  Map<String, Publication> getPublications();

  abstract class Builder extends ExtensionConfiguration.Builder {}

  @Override
  default Builder getBuilder() {
    return ImmutablePubSubConfiguration.builder();
  }

  @NotNull
  static Set<String> getBrokersInUse(OgcApiDataV2 apiData) {
    return apiData.getCollections().values().stream()
        .map(
            collectionData ->
                collectionData
                    .getExtension(PubSubConfiguration.class)
                    .filter(ExtensionConfiguration::isEnabled)
                    .map(
                        cfg ->
                            cfg.getPublications().values().stream()
                                .map(Publication::getBroker)
                                .collect(Collectors.toUnmodifiableSet())))
        .flatMap(Optional::stream)
        .flatMap(Set::stream)
        .collect(Collectors.toUnmodifiableSet());
  }
}
