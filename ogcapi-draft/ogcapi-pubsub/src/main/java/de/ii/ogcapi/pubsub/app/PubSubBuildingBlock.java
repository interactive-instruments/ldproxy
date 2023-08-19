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
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.features.domain.FeatureChangeListener;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title PubSub
 * @langEn TODO.
 * @langDe TODO.
 * @conformanceEn TODO.
 * @conformanceDe TODO.
 * @ref:cfg {@link de.ii.ogcapi.pubsub.app.PubSubConfiguration}
 * @ref:cfgProperties {@link de.ii.ogcapi.pubsub.app.ImmutablePubSubConfiguration}
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
    return ImmutablePubSubConfiguration.builder().enabled(false).build();
  }

  @Override
  public ValidationResult onStartup(OgcApi api, MODE apiValidation) {

    providers
        .getFeatureProvider(api.getData())
        .ifPresent(provider -> provider.getChangeHandler().addListener(onFeatureChange(api)));

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
                pub -> {
                  Mqtt3ClientBuilder builder =
                      MqttClient.builder()
                          .useMqttVersion3()
                          .identifier(publisher)
                          .automaticReconnectWithDefaultConfig()
                          .addConnectedListener(
                              context -> {
                                if (LOGGER.isDebugEnabled()) {
                                  LOGGER.debug(
                                      "PubSub: Connected to broker '{}'.", pub.getBroker());
                                }
                              })
                          .addDisconnectedListener(
                              context -> {
                                if (LOGGER.isDebugEnabled()) {
                                  LOGGER.debug(
                                      "PubSub: Disconnected from broker '{}'.", pub.getBroker());
                                }
                              })
                          .serverHost(brokers.get(pub.getBroker()).getHost())
                          .serverPort(brokers.get(pub.getBroker()).getPort());
                  if (brokers.get(pub.getBroker()).getSsl()) {
                    //noinspection ResultOfMethodCallIgnored
                    builder.sslWithDefaultConfig();
                  }
                  brokers
                      .get(pub.getBroker())
                      .getUsername()
                      .ifPresent(
                          username ->
                              builder
                                  .simpleAuth()
                                  .username(username)
                                  .password(
                                      brokers
                                          .get(pub.getBroker())
                                          .getPassword()
                                          .orElse("")
                                          .getBytes(StandardCharsets.UTF_8))
                                  .applySimpleAuth());
                  Mqtt3AsyncClient client = builder.buildAsync();
                  return ImmutablePublicationContext.builder()
                      .client(client)
                      .connAck(client.connectWith().cleanSession(true).send())
                      .qos(pub.getMqttQos())
                      .timeout(pub.getTimeout())
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
        switch (change.getAction()) {
          case CREATE:
          case UPDATE:
          case DELETE:
            pubContextMap
                .get(api.getId())
                .forEach(
                    context -> {
                      context
                          .getConnAck()
                          .thenCompose(
                              connAck ->
                                  context
                                      .getClient()
                                      .publishWith()
                                      .topic(
                                          String.format(
                                              "ogcapi/%s/%s/collections/%s/items",
                                              publisherMap.get(api.getId()),
                                              api.getId(),
                                              collectionId))
                                      .qos(context.getQos())
                                      .payload(change.toString().getBytes()) // TODO
                                      .send())
                          .orTimeout(context.getTimeout(), TimeUnit.SECONDS)
                          .thenAccept(
                              publish -> {
                                if (LOGGER.isTraceEnabled()) {
                                  LOGGER.trace("PubSub: Message sent.");
                                }
                              })
                          .whenComplete(
                              (ignore, e) -> {
                                if (e != null) {
                                  if (LOGGER.isWarnEnabled()) {
                                    LOGGER.warn(
                                        "PubSub: Error during message publication. Reason: {}",
                                        e.getMessage());
                                  }
                                  if (LOGGER.isDebugEnabled(MARKER.STACKTRACE)) {
                                    LOGGER.debug("Stacktrace: ", e);
                                  }
                                }
                              });
                    });
            break;
        }
      }
    };
  }
}
