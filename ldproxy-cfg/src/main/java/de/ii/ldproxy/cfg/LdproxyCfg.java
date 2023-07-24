/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.ImmutableStoreConfiguration;
import de.ii.xtraplatform.base.domain.ImmutableStoreSourceDefaultV3;
import de.ii.xtraplatform.base.domain.Jackson;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.base.domain.StoreConfiguration;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.codelists.domain.CodelistData;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.services.domain.ServiceData;
import de.ii.xtraplatform.store.app.EventStoreDefault;
import de.ii.xtraplatform.store.app.StoreImpl;
import de.ii.xtraplatform.store.app.ValueEncodingJackson;
import de.ii.xtraplatform.store.app.entities.EntityDataDefaultsStoreImpl;
import de.ii.xtraplatform.store.app.entities.EntityDataStoreImpl;
import de.ii.xtraplatform.store.domain.EventStore;
import de.ii.xtraplatform.store.domain.EventStoreDriver;
import de.ii.xtraplatform.store.domain.Identifier;
import de.ii.xtraplatform.store.domain.ValueEncoding.FORMAT;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaultsStore;
import de.ii.xtraplatform.store.domain.entities.EntityDataStore;
import de.ii.xtraplatform.store.domain.entities.EntityFactoriesImpl;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.infra.EventStoreDriverFs;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LdproxyCfg implements Cfg {

  private final EntityDataDefaultsStore entityDataDefaultsStore;
  private final EntityDataStore<EntityData> entityDataStore;
  private final Path dataDirectory;
  private final Builders builders;
  private final ObjectMapper objectMapper;
  private final RequiredIncludes requiredIncludes;
  private final de.ii.xtraplatform.store.domain.entities.EntityFactories entityFactories;

  public LdproxyCfg(Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    Path store = dataDirectory.resolve(StoreConfiguration.DEFAULT_LOCATION);
    try {
      Files.createDirectories(store);
    } catch (IOException e) {
      throw new IllegalStateException("Could not create " + store);
    }

    this.requiredIncludes = new RequiredIncludes();
    this.builders = new Builders() {};
    StoreConfiguration storeConfiguration =
        new ImmutableStoreConfiguration.Builder()
            .addSources(new ImmutableStoreSourceDefaultV3.Builder().build())
            .build();
    Jackson jackson = new JacksonProvider(JacksonSubTypes::ids, false);
    this.objectMapper = new ValueEncodingJackson<EntityData>(jackson, false).getMapper(FORMAT.YML);
    EventStoreDriver storeDriver = new EventStoreDriverFs(dataDirectory);
    EventStore eventStore =
        new EventStoreDefault(
            new StoreImpl(dataDirectory, storeConfiguration),
            storeDriver,
            new EventSubscriptionsSync());
    ((EventStoreDefault) eventStore).onStart();
    AppContext appContext = new AppContextCfg();
    OgcApiExtensionRegistry extensionRegistry = new OgcApiExtensionRegistry();
    Set<EntityFactory> factories = EntityFactories.factories(extensionRegistry);
    this.entityFactories = new EntityFactoriesImpl(() -> factories);
    this.entityDataDefaultsStore =
        new EntityDataDefaultsStoreImpl(appContext, eventStore, jackson, () -> factories);
    ((EntityDataDefaultsStoreImpl) entityDataDefaultsStore).onStart();
    this.entityDataStore =
        new EntityDataStoreImpl(
            appContext, eventStore, jackson, () -> factories, entityDataDefaultsStore);
    ((EntityDataStoreImpl) entityDataStore).onStart();
  }

  @Override
  public Builders builder() {
    return builders;
  }

  public Path getDataDirectory() {
    return dataDirectory;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public EntityDataDefaultsStore getEntityDataDefaultsStore() {
    return entityDataDefaultsStore;
  }

  public EntityDataStore<EntityData> getEntityDataStore() {
    return entityDataStore;
  }

  public de.ii.xtraplatform.store.domain.entities.EntityFactories getEntityFactories() {
    return entityFactories;
  }

  @Override
  public <T extends EntityData> void writeEntity(T data, Path... patches) throws IOException {
    try {
      entityDataStore.put(data.getId(), data, getType(data)).join();

      for (Path patch : patches) {
        Map<String, Object> patchMap =
            objectMapper.readValue(patch.toFile(), new TypeReference<Map<String, Object>>() {});

        entityDataStore.patch(data.getId(), patchMap, true, getType(data)).join();
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

  // TODO: for which entity type, writes application defaults as well
  @Override
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

  @Override
  public <T extends EntityData> void addEntity(T data) throws IOException {
    Path path = getPath(data);

    path.getParent().toFile().mkdirs();
    objectMapper.writeValue(path.toFile(), data);
  }

  @Override
  public <T extends EntityData> void writeEntity(T data, OutputStream outputStream)
      throws IOException {
    addEntity(data);

    Path path = getPath(data);

    Files.copy(path, outputStream);
  }

  @Override
  public void writeZippedStore(OutputStream outputStream) throws IOException {
    ZipOutputStream zipOut = new ZipOutputStream(outputStream);
    zipFile(dataDirectory.toFile(), dataDirectory.toFile().getName(), zipOut, true);
    zipOut.close();
  }

  private <T extends EntityData> Path getPath(T data) {
    return dataDirectory.resolve(
        Paths.get("store", "entities", getType(data), data.getId() + ".yml"));
  }

  private static <T extends EntityData> String getType(T data) {
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

  private static <T extends EntityData> String getSubType(T data) {
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
