/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import akka.stream.QueueOfferResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ldproxy.ogcapi.app.OgcApiDataV2Defaults;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.dropwizard.domain.Jackson;
import de.ii.xtraplatform.dropwizard.domain.JacksonProvider;
import de.ii.xtraplatform.dropwizard.domain.XtraPlatform;
import de.ii.xtraplatform.feature.provider.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.runtime.domain.Constants.ENV;
import de.ii.xtraplatform.runtime.domain.StoreConfiguration;
import de.ii.xtraplatform.runtime.domain.XtraPlatformConfiguration;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.app.EventStoreDefault;
import de.ii.xtraplatform.store.app.EventSubscriptions;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.app.entities.EntityDataDefaultsStoreImpl;
import de.ii.xtraplatform.store.app.entities.EntityDataStoreImpl;
import de.ii.xtraplatform.store.app.entities.EntityFactoryImpl;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.EventStoreDriver;
import de.ii.xtraplatform.store.domain.EventStoreSubscriber;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.TypedEvent;
import de.ii.xtraplatform.store.domain.ValueEncoding.FORMAT;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.EntityState;
import de.ii.xtraplatform.store.domain.entities.EntityState.STATE;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.store.infra.EventStoreDriverFs;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LdproxyCfg {

  private final EntityDataDefaultsStore entityDataDefaultsStore;
  private final EntityDataStore<EntityData> entityDataStore;
  private final Builders builders;
  private final Map<String, EventStoreSubscriber> subscribers;
  private final ObjectMapper objectMapper;
  private final RequiredIncludes requiredIncludes;

  public LdproxyCfg(Path dataDirectory) {
    this.requiredIncludes = new RequiredIncludes();
    this.builders = new Builders() {
    };
    this.subscribers = new LinkedHashMap<>();
    StoreConfiguration storeConfiguration = new StoreConfiguration();
    Jackson jackson = new JacksonProvider(JacksonSubTypes.ids());
    this.objectMapper = new ValueEncodingJackson<EntityData>(jackson, true).getMapper(FORMAT.YML);
    EventStoreDriver storeDriver = new EventStoreDriverFs(dataDirectory.toAbsolutePath().toString(),
        storeConfiguration);
    EventStore eventStore = new EventStoreDefault(storeConfiguration, storeDriver,
        new EventSubscriptions() {
          @Override
          public void addSubscriber(EventStoreSubscriber subscriber) {
            subscriber.getEventTypes().forEach(type -> subscribers.put(type, subscriber));
          }

          @Override
          public CompletableFuture<QueueOfferResult> emitEvent(TypedEvent event) {
            if (subscribers.containsKey(event.type())) {
              subscribers.get(event.type()).onEmit(event);
            }

            return CompletableFuture.completedFuture(null);
          }

          @Override
          public void startListening() {

          }
        });
    EntityRegistry entityRegistryNoOp = new EntityRegistry() {
      @Override
      public <T extends PersistentEntity> List<T> getEntitiesForType(Class<T> type) {
        return null;
      }

      @Override
      public <T extends PersistentEntity> Optional<T> getEntity(Class<T> type, String id) {
        return Optional.empty();
      }

      @Override
      public Optional<PersistentEntity> getEntity(String type, String id) {
        return Optional.empty();
      }

      @Override
      public Optional<STATE> getEntityState(String type, String id) {
        return Optional.empty();
      }

      @Override
      public void addEntityStateListener(Consumer<EntityState> listener) {

      }

      @Override
      public void addEntityListener(BiConsumer<String, PersistentEntity> listener) {

      }

      @Override
      public <T extends PersistentEntity> void addEntityListener(Class<T> type,
          Consumer<T> listener, boolean existing) {

      }

      @Override
      public <T extends PersistentEntity> void addEntityGoneListener(Class<T> type,
          Consumer<T> listener) {

      }
    };
    EntityFactoryImpl entityFactory = new EntityFactoryImpl(null, null, entityRegistryNoOp);
    entityFactory.registerEntityDataClass(Codelist.ENTITY_TYPE, Optional.empty(),
        CodelistData.class, builder().entity().codelist());
    entityFactory.registerEntityDataClass(FeatureProvider2.ENTITY_TYPE, Optional.empty(),
        FeatureProviderDataV2.class, builder().entity().provider());
    entityFactory.registerEntityDataClass(FeatureProvider2.ENTITY_TYPE,
        Optional.of(FeatureProviderSql.ENTITY_SUB_TYPE), FeatureProviderSqlData.class,
        builder().entity().provider());
    entityFactory.registerEntityDataClass(Service.TYPE, Optional.empty(), ServiceData.class,
        builder().entity().api());
    entityFactory.registerEntityDataClass(Service.TYPE, Optional.of(OgcApiDataV2.SERVICE_TYPE),
        OgcApiDataV2.class, builder().entity().api());
    //TODO: register defaults
    entityFactory.registerEntityDataDefaults(Service.TYPE, Optional.of(OgcApiDataV2.SERVICE_TYPE),
        new OgcApiDataV2Defaults(new OgcApiExtensionRegistry()));

    XtraPlatform xtraPlatform = new XtraPlatform() {
      @Override
      public String getApplicationName() {
        return null;
      }

      @Override
      public String getApplicationVersion() {
        return null;
      }

      @Override
      public ENV getApplicationEnvironment() {
        return null;
      }

      @Override
      public XtraPlatformConfiguration getConfiguration() {
        XtraPlatformConfiguration xtraPlatformConfiguration = new XtraPlatformConfiguration();
        xtraPlatformConfiguration.store.failOnUnknownProperties = true;
        return xtraPlatformConfiguration;
      }

      @Override
      public URI getUri() {
        return null;
      }

      @Override
      public URI getServicesUri() {
        return null;
      }
    };
    this.entityDataDefaultsStore = new EntityDataDefaultsStoreImpl(xtraPlatform, eventStore,
        jackson, entityFactory);
    this.entityDataStore = new EntityDataStoreImpl(xtraPlatform, eventStore, jackson, entityFactory,
        entityDataDefaultsStore);
  }
  //TODO: env subst example, patch without lastModified before 3.2 release
  public Builders builder() {
    return builders;
  }

  public <T extends EntityData> void writeEntity(T data, Path... patches) throws IOException {
    try {
      entityDataStore.put(data.getId(), data, getType(data)).join();

      for (Path patch : patches) {
        Map<String, Object> patchMap = objectMapper.readValue(patch.toFile(),
            new TypeReference<Map<String, Object>>() {
            });

        entityDataStore.patch(data.getId(), patchMap, getType(data)).join();
      }
    } catch (CompletionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  //TODO: for which entity type, writes application defaults as well
  public <T extends EntityData> void writeDefaults(T data, Path... defaults) throws IOException {
    Identifier defaultsIdentifier = Identifier.from(data.getId(), getType(data), getSubType(data));
    EntityDataBuilder<EntityData> builder = entityDataDefaultsStore.getBuilder(defaultsIdentifier).fillRequiredFieldsWithPlaceholders();

    for(Path defaultFile: defaults) {
      objectMapper.readerForUpdating(builder).readValue(defaultFile.toFile());
    }

    Map<String, Object> asMap = entityDataDefaultsStore.asMap(Identifier.from(data.getId(), getType(data)), builder.build());
    try {
      entityDataDefaultsStore.patch(data.getId(), asMap, getType(data), getSubType(data)).join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    }
  }

  private <T extends EntityData> String getType(T data) {
    if (data instanceof CodelistData) {
      return Codelist.ENTITY_TYPE;
    }
    if (data instanceof FeatureProviderDataV2) {
      return FeatureProvider2.ENTITY_TYPE;
    }
    if (data instanceof ServiceData) {
      return Service.TYPE;
    }
    return null;
  }

  private <T extends EntityData> String getSubType(T data) {
    if (data instanceof CodelistData) {
      return null;
    }
    if (data instanceof FeatureProviderSqlData) {
      return FeatureProviderSql.ENTITY_SUB_TYPE.replace('/', '.').toLowerCase();
    }
    if (data instanceof OgcApiDataV2) {
      return OgcApiDataV2.SERVICE_TYPE.toLowerCase();
    }
    return null;
  }
}
