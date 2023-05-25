/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.pubsub.domain.Broker;
import de.ii.ogcapi.pubsub.domain.ImmutablePubSubConfiguration;
import de.ii.ogcapi.pubsub.domain.ImmutablePublicationContext;
import de.ii.ogcapi.pubsub.domain.PubSubConfiguration;
import de.ii.ogcapi.pubsub.domain.PublicationContext;
import de.ii.xtraplatform.features.domain.FeatureChangeListener;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title PubSub
 * @langEn TODO.
 * @langDe TODO.
 * @conformanceEn TODO.
 * @conformanceDe TODO.
 * @ref:cfg {@link de.ii.ogcapi.pubsub.domain.PubSubConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.pubsub.domain.ImmutablePubSubConfiguration}
 */
@Singleton
@AutoBind
public class PubSubBuildingBlock implements ApiBuildingBlock {

  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubBuildingBlock.class);

  private final FeaturesCoreProviders providers;
  private final Map<String, List<PublicationContext>> pubContextMap;
  private final Map<String, String> publisherMap;

  @Inject
  public PubSubBuildingBlock(FeaturesCoreProviders providers) {
    this.providers = providers;
    this.pubContextMap = new HashMap<>();
    this.publisherMap = new HashMap<>();
  }

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new ImmutablePubSubConfiguration.Builder().enabled(false).build();
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {
    providers
        .getFeatureProvider(api.getData())
        .ifPresent(
            provider -> {
              provider.getChangeHandler().addListener(onFeatureChange(api));
            });

    Map<String, Broker> brokers =
        api.getData()
            .getExtension(PubSubConfiguration.class)
            .map(PubSubConfiguration::getBrokers)
            .orElse(ImmutableMap.of());

    String publisher =
        api.getData()
            .getExtension(PubSubConfiguration.class)
            .flatMap(cfg -> Optional.ofNullable(cfg.getPublisher()))
            .orElse("ldproxy");
    publisherMap.put(api.getId(), publisher);

    pubContextMap.put(
        api.getId(),
        api
            .getData()
            .getExtension(PubSubConfiguration.class)
            .map(PubSubConfiguration::getPublications)
            .orElse(ImmutableList.of())
            .stream()
            .map(
                // TODO check config validity
                pub -> {
                  Mqtt3ClientBuilder builder =
                      MqttClient.builder()
                          .useMqttVersion3()
                          .identifier(publisher)
                          .automaticReconnectWithDefaultConfig()
                          .serverHost(brokers.get(pub.getBroker()).getHost())
                          .serverPort(brokers.get(pub.getBroker()).getPort());
                  //noinspection ResultOfMethodCallIgnored
                  brokers
                      .get(pub.getBroker())
                      .getUsername()
                      .ifPresent(
                          username ->
                              builder.simpleAuth(
                                  Mqtt3SimpleAuth.builder()
                                      .username(username)
                                      .password(
                                          brokers
                                              .get(pub.getBroker())
                                              .getPassword()
                                              .orElse("")
                                              .getBytes(StandardCharsets.UTF_8))
                                      .build()));
                  Mqtt3BlockingClient client = builder.buildBlocking();
                  @NotNull Mqtt3ConnAck connAck = client.connectWith().cleanSession(true).send();
                  assert connAck.isSessionPresent(); // TODO
                  return ImmutablePublicationContext.builder()
                      .client(client)
                      .qos(pub.getMqttQos())
                      .build();
                })
            .collect(Collectors.toUnmodifiableList()));

    return ValidationResult.of();
  }

  private FeatureChangeListener onFeatureChange(OgcApi api) {
    return change -> {
      String collectionId =
          FeaturesCoreConfiguration.getCollectionId(api.getData(), change.getFeatureType());
      if (isEnabledForApi(api.getData(), collectionId)) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Feature Change (PubSub): {}", change);
        }
        switch (change.getAction()) {
          case CREATE:
          case UPDATE:
          case DELETE:
            // TODO
            // send message to brokers
            pubContextMap
                .get(api.getId())
                .forEach(
                    context ->
                        context
                            .getClient()
                            .publish(
                                Mqtt3Publish.builder()
                                    .topic(
                                        String.format(
                                            "ogcapi/%s/%s/collections/%s/items",
                                            publisherMap.get(api.getId()),
                                            api.getId(),
                                            collectionId))
                                    .qos(context.getQos())
                                    .payload(change.toString().getBytes()) // TODO
                                    .build()));
            break;
        }
      }
    };
  }
}
