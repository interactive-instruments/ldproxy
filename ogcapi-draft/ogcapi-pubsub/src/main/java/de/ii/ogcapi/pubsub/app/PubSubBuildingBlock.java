/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.pubsub.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.exceptions.ConnectionClosedException;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.Query;
import de.ii.ogcapi.features.core.domain.FeaturesCoreQueriesHandler.QueryInputFeature;
import de.ii.ogcapi.features.core.domain.ImmutableQueryInputFeature;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableRequestContext;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.features.domain.FeatureChange.Action;
import de.ii.xtraplatform.features.domain.FeatureChangeListener;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
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

  private final ExtensionRegistry extensionRegistry;
  private final FeaturesCoreProviders providers;
  private final FeaturesCoreQueriesHandler queriesHandler;
  private final Map<String, Map<String, List<PublicationContext>>> pubContextMap;
  private final Map<String, String> publisherMap;
  private List<? extends FormatExtension> formats;
  private final ObjectMapper mapper;

  @Inject
  public PubSubBuildingBlock(
      ExtensionRegistry extensionRegistry,
      FeaturesCoreProviders providers,
      FeaturesCoreQueriesHandler queriesHandler) {
    this.extensionRegistry = extensionRegistry;
    this.providers = providers;
    this.queriesHandler = queriesHandler;
    this.pubContextMap = new HashMap<>();
    this.publisherMap = new HashMap<>();
    this.mapper = new ObjectMapper();
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

    String publisher =
        api.getData()
            .getExtension(PubSubConfiguration.class)
            .flatMap(cfg -> Optional.ofNullable(cfg.getPublisher()))
            .orElse("ldproxy");
    publisherMap.put(api.getId(), publisher);

    Set<String> brokersInUse = PubSubConfiguration.getBrokersInUse(api.getData());

    Map<String, Pair<Mqtt3AsyncClient, CompletableFuture<Mqtt3ConnAck>>> clientMap =
        api
            .getData()
            .getExtension(PubSubConfiguration.class)
            .map(PubSubConfiguration::getBrokers)
            .orElse(ImmutableMap.of())
            .entrySet()
            .stream()
            .filter(entry -> brokersInUse.contains(entry.getKey()))
            .map(
                entry -> {
                  Mqtt3AsyncClient client = getClient(publisher, entry.getKey(), entry.getValue());
                  return new SimpleImmutableEntry<>(
                      entry.getKey(),
                      Pair.of(client, client.connectWith().cleanSession(true).send()));
                })
            .collect(
                Collectors.toUnmodifiableMap(
                    SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));

    ImmutableMap.Builder<String, List<PublicationContext>> collectionBuilder =
        ImmutableMap.builder();
    api.getData()
        .getCollections()
        .forEach(
            (collectionId, collectionData) ->
                collectionBuilder.put(
                    collectionId,
                    api
                        .getData()
                        .getExtension(PubSubConfiguration.class, collectionId)
                        .map(PubSubConfiguration::getPublications)
                        .orElse(ImmutableMap.of())
                        .entrySet()
                        .stream()
                        .map(
                            entry -> {
                              String pubId = entry.getKey();
                              Publication pub = entry.getValue();

                              return ImmutablePublicationContext.builder()
                                  .client(clientMap.get(pub.getBroker()).getLeft())
                                  .connAck(clientMap.get(pub.getBroker()).getRight())
                                  .qos(pub.getMqttQos())
                                  .subPath(pubId)
                                  .parameters(pub.getParameters())
                                  .property(pub.getProperty())
                                  .timeout(pub.getTimeout())
                                  .retain(pub.getRetain())
                                  .build();
                            })
                        .collect(Collectors.toUnmodifiableList())));

    pubContextMap.put(api.getId(), collectionBuilder.build());

    return ValidationResult.of();
  }

  private static Mqtt3AsyncClient getClient(String publisher, String brokerId, Broker broker) {
    Mqtt3ClientBuilder clientBuilder =
        MqttClient.builder()
            .useMqttVersion3()
            .identifier(publisher)
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener(
                context -> {
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("PubSub: Connected to broker '{}'", brokerId);
                  }
                })
            .addDisconnectedListener(
                context -> {
                  if (LOGGER.isDebugEnabled()) {
                    if (context instanceof ConnectionClosedException) {
                      LOGGER.debug(
                          "PubSub: Disconnected from broker '{}': {}",
                          brokerId,
                          ((ConnectionClosedException) context).getMessage());
                    } else {
                      LOGGER.debug("PubSub: Disconnected from broker '{}'", brokerId);
                    }
                  }
                })
            .serverHost(broker.getHost())
            .serverPort(broker.getPort());
    if (broker.getSsl()) {
      //noinspection ResultOfMethodCallIgnored
      clientBuilder.sslWithDefaultConfig();
    }
    broker
        .getUsername()
        .ifPresent(
            username ->
                clientBuilder
                    .simpleAuth()
                    .username(username)
                    .password(broker.getPassword().orElse("").getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth());
    return clientBuilder.buildAsync();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return ApiBuildingBlock.super.isEnabledForApi(apiData, collectionId)
        && apiData
            .getExtension(GeoJsonConfiguration.class, collectionId)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  private FeatureChangeListener onFeatureChange(OgcApi api) {
    return change -> {
      String collectionId =
          FeaturesCoreConfiguration.getCollectionId(api.getData(), change.getFeatureType());
      if (isEnabledForApi(api.getData(), collectionId)) {
        pubContextMap
            .get(api.getId())
            .get(collectionId)
            .forEach(
                context -> {
                  change
                      .getFeatureIds()
                      .forEach(
                          featureId -> {
                            if (change.getAction() != Action.DELETE
                                || context.getProperty().isEmpty()) {
                              context
                                  .getConnAck()
                                  .thenCompose(
                                      connAck -> {
                                        ObjectNode geojson;
                                        ObjectNode properties;
                                        switch (change.getAction()) {
                                          case CREATE:
                                          case UPDATE:
                                            geojson =
                                                getCurrentFeature(
                                                    api,
                                                    providers.getFeatureProviderOrThrow(
                                                        api.getData()),
                                                    collectionId,
                                                    featureId);
                                            if (Objects.isNull(geojson)) {
                                              geojson = initFeature(featureId);
                                            } else if (Objects.isNull(geojson.get("properties"))) {
                                              geojson.putObject("properties");
                                            }
                                            break;
                                          default:
                                          case DELETE:
                                            geojson = initFeature(featureId);
                                            break;
                                        }
                                        // add PubSub values
                                        properties = (ObjectNode) geojson.get("properties");
                                        properties.put("$id", UUID.randomUUID().toString());
                                        properties.put(
                                            "$pubtime",
                                            Instant.now()
                                                .truncatedTo(ChronoUnit.MILLIS)
                                                .toString());
                                        properties.put(
                                            "$operation",
                                            change.getAction().toString().toLowerCase());
                                        try {
                                          String topic =
                                              String.format(
                                                  "ogcapi/%s/%s/collections/%s/%s",
                                                  publisherMap.get(api.getId()),
                                                  api.getId(),
                                                  collectionId,
                                                  replaceParameters(
                                                      context.getSubPath(),
                                                      context.getParameters(),
                                                      properties));

                                          return context
                                              .getClient()
                                              .publishWith()
                                              .topic(topic)
                                              .qos(context.getQos())
                                              .payload(
                                                  mapper.writeValueAsBytes(
                                                      context
                                                          .getProperty()
                                                          .map(properties::get)
                                                          .orElse(geojson)))
                                              .retain(context.getRetain())
                                              .send();
                                        } catch (JsonProcessingException e) {
                                          throw new IllegalStateException(e);
                                        }
                                      })
                                  .orTimeout(context.getTimeout(), TimeUnit.SECONDS)
                                  .thenAccept(
                                      publish -> {
                                        if (LOGGER.isTraceEnabled()) {
                                          LOGGER.trace(
                                              "PubSub action '{}', collection '{}', feature '{}': Message sent.",
                                              change.getAction(),
                                              collectionId,
                                              featureId);
                                        }
                                      })
                                  .whenComplete(
                                      (ignore, e) -> {
                                        if (e != null) {
                                          if (LOGGER.isWarnEnabled()) {
                                            LOGGER.warn(
                                                "PubSub action '{}', collection '{}', feature '{}': Error during message publication. Reason: {}",
                                                change.getAction(),
                                                collectionId,
                                                featureId,
                                                e.getMessage());
                                          }
                                          if (LOGGER.isDebugEnabled(MARKER.STACKTRACE)) {
                                            LOGGER.debug("Stacktrace: ", e);
                                          }
                                        }
                                      });
                            }
                          });
                });
      }
    };
  }

  public static String replaceParameters(
      String input, Map<String, String> parameters, ObjectNode properties) {
    Pattern pattern = Pattern.compile("\\{(\\w+)}");
    Matcher matcher = pattern.matcher(input);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String property = parameters.get(matcher.group(1));
      if (Objects.nonNull(property)) {
        JsonNode value = properties.get(property);
        if (Objects.nonNull(value)) {
          matcher.appendReplacement(result, value.asText());
        } else {
          matcher.appendReplacement(result, "null");
        }
      } else {
        matcher.appendReplacement(result, "unknown");
        if (LOGGER.isErrorEnabled()) {
          // TODO add to validation at startup
          LOGGER.error(
              "PubSub: Invalid configuration, unknown feature property {}", matcher.group(1));
        }
      }
    }

    matcher.appendTail(result);
    return result.toString();
  }

  @NotNull
  private ObjectNode initFeature(String featureId) {
    ObjectNode geojson;
    geojson = mapper.createObjectNode();
    geojson.putObject("properties");
    geojson.putNull("geometry");
    geojson.put("type", "Feature");
    geojson.put("id", featureId);
    return geojson;
  }

  private ObjectNode getCurrentFeature(
      OgcApi api, FeatureProvider2 provider, String collectionId, String featureId) {
    try {
      if (formats == null) {
        formats = extensionRegistry.getExtensionsForType(FeatureFormatExtension.class);
      }

      ApiRequestContext requestContextGeoJson =
          new ImmutableRequestContext.Builder()
              .api(api)
              .request(Optional.empty())
              .requestUri(
                  new URICustomizer(api.getUri())
                      .ensureLastPathSegments("collections", collectionId, "items", featureId)
                      .build())
              .mediaType(
                  new ImmutableApiMediaType.Builder()
                      .type(new MediaType("application", "geo+json"))
                      .label("GeoJSON")
                      .parameter("json")
                      .build())
              .alternateMediaTypes(
                  formats.stream()
                      .filter(f -> f.isEnabledForApi(api.getData(), collectionId))
                      .map(FormatExtension::getMediaType)
                      .filter(
                          mediaType -> !"geo+json".equalsIgnoreCase(mediaType.type().getSubtype()))
                      .collect(Collectors.toUnmodifiableSet()))
              .build();

      FeaturesCoreConfiguration coreConfiguration =
          api.getData().getExtension(FeaturesCoreConfiguration.class, collectionId).get();
      FeatureQuery query =
          ImmutableFeatureQuery.builder()
              .type(coreConfiguration.getFeatureType().orElse(collectionId))
              .filter(In.of(ScalarLiteral.of(featureId)))
              .returnsSingleFeature(true)
              .build();

      QueryInputFeature queryInput =
          new ImmutableQueryInputFeature.Builder()
              .collectionId(collectionId)
              .featureId(featureId)
              .query(query)
              .featureProvider(provider)
              .defaultCrs(coreConfiguration.getDefaultEpsgCrs())
              .build();

      Response response = queriesHandler.handle(Query.FEATURE, queryInput, requestContextGeoJson);

      if (response.getStatus() == 200) {
        if (response.getEntity() instanceof byte[]) {
          return (ObjectNode) mapper.readTree((byte[]) response.getEntity());
        } else {
          if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(
                "PubSub: Could not retrieve feature in collection '{}' with id '{}'. Reason: feature payload must be of type 'byte[]', found: {}",
                collectionId,
                featureId,
                response.getEntity().getClass().getSimpleName());
          }
        }
      } else {
        if (LOGGER.isWarnEnabled()) {
          LOGGER.warn(
              "PubSub: Could not retrieve feature in collection '{}' with id '{}'. Reason: feature query failed, status: {}",
              collectionId,
              featureId,
              response.getStatus());
        }
      }

    } catch (URISyntaxException | IOException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "PubSub: Could not retrieve feature in collection '{}' with id '{}'. Reason: feature query failed with an exception: {}",
            collectionId,
            featureId,
            e.getMessage());
      }
      if (LOGGER.isDebugEnabled(MARKER.STACKTRACE)) {
        LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
      }
    }
    return null;
  }
}
