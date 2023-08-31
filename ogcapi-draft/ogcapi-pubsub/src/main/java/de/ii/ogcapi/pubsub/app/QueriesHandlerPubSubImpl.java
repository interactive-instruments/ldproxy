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
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaObject;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaString;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.ogcapi.foundation.domain.QueryInputGeneric;
import de.ii.ogcapi.pubsub.domain.AsyncApi;
import de.ii.ogcapi.pubsub.domain.AsyncApiChannel;
import de.ii.ogcapi.pubsub.domain.AsyncApiDefinitionFormatExtension;
import de.ii.ogcapi.pubsub.domain.AsyncApiServer;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApi;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiChannel;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiComponents;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiContact;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiInfo;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiLicense;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiMessage;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiOperation;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiOperationBindingsMqtt;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiReference;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiSecurity;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiServer;
import de.ii.ogcapi.pubsub.domain.ImmutableAsyncApiServerBindingsMqtt;
import de.ii.ogcapi.pubsub.domain.QueriesHandlerPubSub;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class QueriesHandlerPubSubImpl implements QueriesHandlerPubSub {

  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final Map<String, AsyncApi> asyncApiDefinitions;

  @Inject
  public QueriesHandlerPubSubImpl() {
    this.queryHandlers =
        ImmutableMap.of(
            Query.ASYNC_API_DEFINITION,
            QueryHandler.with(QueryInputGeneric.class, this::getAsyncApiDefinitionResponse));
    this.asyncApiDefinitions = new HashMap<>();
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  private Response getAsyncApiDefinitionResponse(
      QueryInput queryInput, ApiRequestContext requestContext) {

    AsyncApiDefinitionFormatExtension outputFormatExtension =
        requestContext
            .getApi()
            .getOutputFormat(
                AsyncApiDefinitionFormatExtension.class,
                requestContext.getMediaType(),
                Optional.empty())
            .orElseThrow(
                () ->
                    new NotAcceptableException(
                        MessageFormat.format(
                            "The requested media type ''{0}'' is not supported for this resource.",
                            requestContext.getMediaType())));

    Date lastModified = getLastModified(queryInput);
    // TODO support ETag
    EntityTag etag = null;
    Response.ResponseBuilder response = evaluatePreconditions(requestContext, lastModified, etag);
    if (Objects.nonNull(response)) {
      return response.build();
    }

    OgcApiDataV2 apiData = requestContext.getApi().getData();

    if (!asyncApiDefinitions.containsKey(apiData.getId())) {

      ImmutableAsyncApiComponents.Builder components = ImmutableAsyncApiComponents.builder();

      ImmutableAsyncApiInfo.Builder info =
          ImmutableAsyncApiInfo.builder()
              .title(apiData.getLabel())
              .description(apiData.getDescription().orElse(""));

      if (apiData.getMetadata().isPresent()) {
        ApiMetadata md = apiData.getMetadata().get();
        md.getVersion().ifPresent(info::version);
        if (md.getContactName().isPresent()
            || md.getContactUrl().isPresent()
            || md.getContactEmail().isPresent()) {
          ImmutableAsyncApiContact.Builder contact = ImmutableAsyncApiContact.builder();
          md.getContactName().ifPresent(contact::name);
          md.getContactUrl().ifPresent(v -> contact.url(URI.create(v)));
          md.getContactEmail().ifPresent(contact::email);
          info.contact(contact.build());
        }
        if (md.getLicenseName().isPresent()) {
          // license name is required
          ImmutableAsyncApiLicense.Builder license =
              ImmutableAsyncApiLicense.builder().name(md.getLicenseName().get());
          md.getLicenseUrl().ifPresent(v -> license.url(URI.create(v)));
          info.license(license.build());
        }
      }

      PubSubConfiguration cfg = apiData.getExtension(PubSubConfiguration.class).get();
      Set<String> brokersInUse = PubSubConfiguration.getBrokersInUse(apiData);
      ImmutableMap.Builder<String, AsyncApiServer> servers = ImmutableMap.builder();
      cfg.getBrokers()
          .forEach(
              (key, value) -> {
                if (brokersInUse.contains(key)) {
                  ImmutableAsyncApiServer.Builder server =
                      ImmutableAsyncApiServer.builder()
                          .url(String.format("%s:%d", value.getHost(), value.getPort()));

                  if (value.getSsl()) {
                    server.protocol("secure-mqtt");
                  }

                  if (value.getUsername().isPresent() && value.getPassword().isPresent()) {
                    server.security(
                        ImmutableList.of(ImmutableMap.of("userPassword", ImmutableList.of())));
                    components.putSecuritySchemes(
                        "userPassword", ImmutableAsyncApiSecurity.builder().build());
                  }

                  server.bindings(
                      ImmutableAsyncApiServerBindingsMqtt.builder()
                          .clientId(Objects.requireNonNullElse(cfg.getPublisher(), "ldproxy"))
                          .cleanSession(true)
                          .build());

                  servers.put(key, server.build());
                }
              });

      ImmutableMap.Builder<String, AsyncApiChannel> channels = ImmutableMap.builder();
      apiData
          .getCollections()
          .forEach(
              (collectionId, collectionData) -> {
                PubSubConfiguration collectionCfg =
                    collectionData.getExtension(PubSubConfiguration.class).get();
                if (collectionCfg.isEnabled() && !collectionCfg.getPublications().isEmpty()) {
                  components.putMessages(
                      String.format("featureChange_%s", collectionId),
                      ImmutableAsyncApiMessage.builder()
                          .name("featureChangeMessage")
                          .title("Feature Change")
                          .summary("Information about a new, updated or deleted feature.")
                          // .description("TODO")
                          .payload(
                              new ImmutableJsonSchemaObject.Builder()
                                  // TODO
                                  .build())
                          .build());

                  collectionCfg
                      .getPublications()
                      .forEach(
                          (pubId, pub) ->
                              channels.put(
                                  String.format(
                                      "ogcapi/%s/%s/collections/%s/%s",
                                      cfg.getPublisher(), apiData.getId(), collectionId, pubId),
                                  ImmutableAsyncApiChannel.builder()
                                      .subscribe(
                                          ImmutableAsyncApiOperation.builder()
                                              .operationId(
                                                  String.format(
                                                      "featureChange_%s_%s",
                                                      collectionId,
                                                      pubId
                                                          .replace("{", "")
                                                          .replace("}", "")
                                                          .replace("/", "_")))
                                              // .summary("TODO")
                                              .parameters(
                                                  // TODO map to proper schema of the property
                                                  pub.getParameters().entrySet().stream()
                                                      .collect(
                                                          Collectors.toUnmodifiableMap(
                                                              Entry::getKey,
                                                              e ->
                                                                  new ImmutableJsonSchemaString
                                                                          .Builder()
                                                                      .build())))
                                              .bindings(
                                                  ImmutableAsyncApiOperationBindingsMqtt.builder()
                                                      .qos(pub.getMqttQos().getCode())
                                                      .retain(pub.getRetain())
                                                      .build())
                                              .message(
                                                  ImmutableAsyncApiReference.builder()
                                                      .ref(
                                                          String.format(
                                                              "#/components/messages/featureChange_%s",
                                                              collectionId))
                                                      .build())
                                              .build())
                                      .servers(ImmutableList.of(pub.getBroker()))
                                      .build()));
                }
              });

      asyncApiDefinitions.put(
          apiData.getId(),
          ImmutableAsyncApi.builder()
              .info(info.build())
              .servers(servers.build())
              .channels(channels.build())
              .components(components.build())
              .build());
    }

    // TODO support headers
    return outputFormatExtension.getResponse(
        asyncApiDefinitions.get(apiData.getId()), requestContext);
  }
}
