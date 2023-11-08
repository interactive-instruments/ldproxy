/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.base.app.StoreImpl;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.ImmutableStoreConfiguration;
import de.ii.xtraplatform.base.domain.ImmutableStoreConfiguration.Builder;
import de.ii.xtraplatform.base.domain.ImmutableStoreSourceDefault;
import de.ii.xtraplatform.base.domain.ImmutableStoreSourceFsV3;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.base.domain.StoreSource;
import de.ii.xtraplatform.base.domain.StoreSourceDefault;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.entities.app.EntityDataDefaultsStoreImpl;
import de.ii.xtraplatform.entities.app.EntityDataStoreImpl;
import de.ii.xtraplatform.entities.app.EventStoreDefault;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.EntityDataDefaultsStore;
import de.ii.xtraplatform.entities.domain.EntityDataStore;
import de.ii.xtraplatform.entities.domain.EntityFactoriesImpl;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.entities.domain.EventStore;
import de.ii.xtraplatform.entities.domain.EventStoreDriver;
import de.ii.xtraplatform.entities.domain.EventStoreSubscriber;
import de.ii.xtraplatform.entities.domain.ReplayEvent;
import de.ii.xtraplatform.entities.infra.EventStoreDriverFs;
import de.ii.xtraplatform.features.domain.ProviderData;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.streams.domain.Event;
import de.ii.xtraplatform.values.api.ValueEncodingJackson;
import de.ii.xtraplatform.values.domain.Identifier;
import de.ii.xtraplatform.values.domain.ValueEncoding.FORMAT;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LdproxyCfgImpl implements LdproxyCfg {

  private static final Logger LOGGER = LoggerFactory.getLogger(LdproxyCfgImpl.class);

  private final EntityDataDefaultsStore entityDataDefaultsStore;
  private final EntityDataStore<EntityData> entityDataStore;
  private final Path dataDirectory;
  private final StoreConfiguration storeConfiguration;
  private final Builders builders;
  private final Migrations migrations;
  private final ObjectMapper objectMapper;
  private final RequiredIncludes requiredIncludes;
  private final de.ii.xtraplatform.entities.domain.EntityFactories entityFactories;
  private final Map<String, JsonSchema> entitySchemas;
  private final List<Identifier> entityIdentifiers;
  private final EventSubscriptionsSync eventSubscriptions;

  public LdproxyCfgImpl(Path dataDirectory) {
    this(dataDirectory, false);
  }

  public LdproxyCfgImpl(Path dataDirectory, boolean noDefaults, boolean layoutV3) {
    this(
        dataDirectory,
        layoutV3
            ? new ImmutableStoreConfiguration.Builder()
                .addSources(
                    new ImmutableStoreSourceFsV3.Builder().src(dataDirectory.toString()).build())
                .build()
            : new ImmutableStoreConfiguration.Builder()
                .addSources(
                    new ImmutableStoreSourceDefault.Builder().src(dataDirectory.toString()).build())
                .build(),
        noDefaults);
  }

  public LdproxyCfgImpl(Path dataDirectory, boolean noDefaults) {
    this(dataDirectory, detectStore(dataDirectory), noDefaults);
  }

  public LdproxyCfgImpl(Path dataDirectory, StoreConfiguration sc, boolean noDefaults) {
    this.dataDirectory = dataDirectory;
    this.storeConfiguration = sc;
    this.requiredIncludes = new RequiredIncludes();
    this.builders = new Builders() {};
    Jackson jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    this.objectMapper = new ValueEncodingJackson<EntityData>(jackson, false).getMapper(FORMAT.YML);
    this.eventSubscriptions = new EventSubscriptionsSync();
    EventStoreDriver storeDriver = new EventStoreDriverFs(dataDirectory);
    EventStore eventStore =
        new EventStoreDefault(
            new StoreImpl(dataDirectory, storeConfiguration), storeDriver, eventSubscriptions);
    ((EventStoreDefault) eventStore).onStart();
    this.entityIdentifiers = new ArrayList<>();
    eventStore.subscribe(
        new EventStoreSubscriber() {
          @Override
          public List<String> getEventTypes() {
            return List.of(
                EntityDataDefaultsStore.EVENT_TYPE,
                EntityDataStore.EVENT_TYPE_ENTITIES,
                EntityDataStore.EVENT_TYPE_OVERRIDES);
          }

          @Override
          public void onEmit(Event event) {
            if (event instanceof ReplayEvent) {
              // System.out.println("EVENT " + ((ReplayEvent) event).asPath());
              if (Objects.equals(((ReplayEvent) event).type(), EntityDataStore.EVENT_TYPE_ENTITIES)
                  && Objects.equals(((ReplayEvent) event).format().toLowerCase(), "yml")) {
                entityIdentifiers.add(((ReplayEvent) event).identifier());
              }
            }
          }
        });
    AppContext appContext = new AppContextCfg();
    ResourceStore mockResourceStore = new MockResourceStore();
    OgcApiExtensionRegistry extensionRegistry = new OgcApiExtensionRegistry(appContext);
    Set<EntityFactory> factories =
        EntityFactories.factories(appContext, extensionRegistry, mockResourceStore);
    this.entityFactories = new EntityFactoriesImpl(() -> factories);
    this.entityDataDefaultsStore =
        new EntityDataDefaultsStoreImpl(appContext, eventStore, jackson, () -> factories);
    this.entityDataStore =
        new EntityDataStoreImpl(
            appContext,
            eventStore,
            jackson,
            () -> factories,
            entityDataDefaultsStore,
            mockResourceStore,
            new MockValueStore(),
            noDefaults);
    this.entitySchemas = new HashMap<>();
    this.migrations = Migrations.create(entityDataStore);
  }

  private static StoreConfiguration detectStore(Path dataDirectory) {
    Optional<ImmutableStoreConfiguration> sc =
        LayoutImpl.detectSource(dataDirectory)
            .map(storeSourceFs -> new Builder().addSources(storeSourceFs).build());

    if (sc.isEmpty()) {
      throw new IllegalArgumentException("No store detected in " + dataDirectory);
    }

    return sc.get();
  }

  @Override
  public Path getEntitiesPath() {
    StoreSource storeSource = storeConfiguration.getSources(dataDirectory).get(0);

    return storeSource instanceof StoreSourceDefault
        ? dataDirectory.resolve("entities/instances")
        : dataDirectory.resolve("store/entities");
  }

  @Override
  public Builders builder() {
    return builders;
  }

  @Override
  public Migrations migrations() {
    return migrations;
  }

  @Override
  public Path getDataDirectory() {
    return dataDirectory;
  }

  @Override
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public EntityDataDefaultsStore getEntityDataDefaultsStore() {
    return entityDataDefaultsStore;
  }

  @Override
  public EntityDataStore<EntityData> getEntityDataStore() {
    return entityDataStore;
  }

  @Override
  public de.ii.xtraplatform.entities.domain.EntityFactories getEntityFactories() {
    return entityFactories;
  }

  @Override
  public List<Identifier> getEntityIdentifiers() {
    return entityIdentifiers;
  }

  @Override
  public void ignoreEventsFor(String type, Identifier identifier) {
    eventSubscriptions.addIgnore(type, identifier);
  }

  public void init() throws IOException {
    ObjectMapper jsonMapper = entityDataStore.getValueEncoding().getMapper(FORMAT.JSON);
    JsonMetaSchema metaSchema =
        JsonMetaSchema.builder(
                "https://json-schema.org/draft/2020-12/schema", JsonMetaSchema.getV202012())
            .addKeyword(new DeprecatedKeyword())
            .build();
    JsonSchemaFactory factory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(VersionFlag.V202012))
            .addMetaSchema(metaSchema)
            .objectMapper(jsonMapper)
            .build();

    for (String entityType : List.of("codelists", "providers", "services", "users")) {
      URL schemaResource =
          Resources.getResource(
              LdproxyCfgImpl.class, String.format("/json-schema/entities/%s.json", entityType));

      JsonSchema schema = factory.getSchema(Resources.asByteSource(schemaResource).openStream());
      schema.initializeValidators();

      this.entitySchemas.put(entityType, schema);
    }
  }

  @Override
  public void initStore() {
    ((EntityDataDefaultsStoreImpl) entityDataDefaultsStore).onStart();
    ((EntityDataStoreImpl) entityDataStore).onStart();
  }

  @Override
  public Set<ValidationMessage> validateEntity(Path entityPath, String entityType)
      throws IOException {
    if (!entitySchemas.containsKey(entityType)) {
      throw new IllegalStateException();
    }
    // System.out.println("VALIDATE " + entityPath);

    JsonNode jsonNode = objectMapper.readTree(entityPath.toFile());

    return entitySchemas.get(entityType).validate(jsonNode);
  }

  @Override
  public <T extends EntityData> void writeEntity(T data, Path... patches) throws IOException {
    Path path = getPath(data);
    Identifier identifier = Identifier.from(data.getId(), getType(data));

    T patched = applyPatch(identifier, data, patches);
    Map<String, Object> asMap = getEntityDataStore().asMap(identifier, patched);

    Map<String, Object> withoutDefaults =
        getEntityDataDefaultsStore()
            .subtractDefaults(identifier, data.getEntitySubType(), asMap, List.of("enabled"));

    path.getParent().toFile().mkdirs();
    objectMapper.writeValue(path.toFile(), withoutDefaults);
  }

  @Override
  public <T extends EntityData> void writeEntity(T data, OutputStream outputStream)
      throws IOException {
    writeEntity(data);

    Path path = getPath(data);

    Files.copy(path, outputStream);
  }

  @Override
  public void writeZippedStore(OutputStream outputStream) throws IOException {
    ZipOutputStream zipOut = new ZipOutputStream(outputStream);
    zipFile(dataDirectory.toFile(), dataDirectory.toFile().getName(), zipOut, true);
    zipOut.close();
  }

  private <T extends EntityData> T applyPatch(Identifier identifier, T data, Path... patches)
      throws IOException {
    if (patches.length == 0) {
      return data;
    }

    EntityDataBuilder<EntityData> builder =
        entityDataStore.getBuilder(identifier, Optional.ofNullable(getSubType(data))).from(data);

    for (Path patch : patches) {
      objectMapper.readerForUpdating(builder).readValue(patch.toFile());
    }

    return (T) builder.build();
  }

  // TODO: for which entity type, writes application defaults as well
  public <T extends EntityData> void writeDefaults(T data, Path... defaults) throws IOException {
    Identifier defaultsIdentifier = Identifier.from(data.getId(), getType(data), getSubType(data));
    EntityDataBuilder<EntityData> builder =
        entityDataDefaultsStore.getBuilder(defaultsIdentifier).fillRequiredFieldsWithPlaceholders();

    for (Path defaultFile : defaults) {
      objectMapper.readerForUpdating(builder).readValue(defaultFile.toFile());
    }

    Map<String, Object> asMap =
        entityDataDefaultsStore.asMap(
            Identifier.from(data.getId(), getType(data)), builder.build());
    try {
      entityDataDefaultsStore.patch(data.getId(), asMap, getType(data), getSubType(data)).join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    }
  }

  private <T extends EntityData> Path getPath(T data) {
    return dataDirectory.resolve(
        Paths.get("store", "entities", getType(data), data.getId() + ".yml"));
  }

  private static <T extends EntityData> String getType(T data) {
    if (data instanceof ProviderData) {
      return ProviderData.ENTITY_TYPE;
    }
    if (data instanceof ServiceData) {
      return Service.TYPE;
    }
    return null;
  }

  private static <T extends EntityData> String getSubType(T data) {
    if (data instanceof Codelist) {
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

  private static void zipFile(
      File fileToZip, String fileName, ZipOutputStream zipOut, boolean skipParentDir)
      throws IOException {
    if (fileToZip.isHidden()) {
      return;
    }
    if (fileToZip.isDirectory()) {
      if (skipParentDir) {
        File[] children = fileToZip.listFiles();
        for (File childFile : children) {
          zipFile(childFile, childFile.getName(), zipOut, false);
        }
        return;
      }
      if (fileName.endsWith("/")) {
        zipOut.putNextEntry(new ZipEntry(fileName));
        zipOut.closeEntry();
      } else {
        zipOut.putNextEntry(new ZipEntry(fileName + "/"));
        zipOut.closeEntry();
      }
      File[] children = fileToZip.listFiles();
      for (File childFile : children) {
        zipFile(childFile, fileName + "/" + childFile.getName(), zipOut, false);
      }
      return;
    }
    FileInputStream fis = new FileInputStream(fileToZip);
    ZipEntry zipEntry = new ZipEntry(fileName);
    zipOut.putNextEntry(zipEntry);
    byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
      zipOut.write(bytes, 0, length);
    }
    fis.close();
  }
}
