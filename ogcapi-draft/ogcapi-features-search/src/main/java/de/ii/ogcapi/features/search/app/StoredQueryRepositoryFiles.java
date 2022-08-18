/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import static de.ii.ogcapi.foundation.domain.FoundationConfiguration.API_RESOURCES_DIR;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.StoredQueriesFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult.Builder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class StoredQueryRepositoryFiles implements StoredQueryRepository, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredQueryRepositoryFiles.class);

  private final ExtensionRegistry extensionRegistry;
  private final Path store;
  private final I18n i18n;
  private final StoredQueriesLinkGenerator linkGenerator;

  @Inject
  public StoredQueryRepositoryFiles(
      AppContext appContext, ExtensionRegistry extensionRegistry, I18n i18n) {
    this.store = appContext.getDataDir().resolve(API_RESOURCES_DIR).resolve("queries");
    this.i18n = i18n;
    this.extensionRegistry = extensionRegistry;
    this.linkGenerator = new StoredQueriesLinkGenerator();
  }

  @Override
  public void onStart() {
    try {
      Files.createDirectories(store);
    } catch (IOException e) {
      LOGGER.error("Could not create query repository: " + e.getMessage());
    }
  }

  @Override
  public Stream<StoredQueriesFormat> getStoredQueriesFormatStream(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(StoredQueriesFormat.class).stream()
        .filter(format -> format.isEnabledForApi(apiData));
  }

  @Override
  public Stream<StoredQueryFormat> getStoredQueryFormatStream(OgcApiDataV2 apiData) {
    return extensionRegistry.getExtensionsForType(StoredQueryFormat.class).stream()
        .filter(format -> format.isEnabledForApi(apiData));
  }

  @Override
  public List<ApiMediaType> getStoredQueryMediaTypes(OgcApiDataV2 apiData, String queryId) {
    if (!exists(apiData, queryId)) {
      return ImmutableList.of();
    }

    return getStoredQueryFormatStream(apiData)
        .map(StoredQueryFormat::getMediaType)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<QueryExpression> getAll(OgcApiDataV2 apiData) {
    if (!exists(apiData)) {
      return ImmutableList.of();
    }

    try (Stream<Path> paths = Files.walk(getPath(apiData))) {
      return paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".json"))
          .map(
              path -> {
                try {
                  return QueryExpression.of(Files.readAllBytes(path));
                } catch (IOException e) {
                  throw new IllegalStateException(
                      MessageFormat.format("Could not parse stored query '{}'.", path.toString()),
                      e);
                }
              })
          .collect(Collectors.toUnmodifiableList());
    } catch (IOException e) {
      throw new IllegalStateException("Could not parse stored queries.", e);
    }
  }

  @Override
  public QueryExpression get(OgcApiDataV2 apiData, String queryId) {
    if (!exists(apiData, queryId)) {
      throw new NotFoundException(
          MessageFormat.format("The stored query ''{0}'' does not exist in this API.", queryId));
    }

    try {
      return QueryExpression.of(Files.readAllBytes(getPath(apiData, queryId)));
    } catch (IOException e) {
      throw new IllegalStateException(
          MessageFormat.format("The stored query ''{0}'' could not be parsed.", queryId), e);
    }
  }

  @Override
  public boolean exists(OgcApiDataV2 apiData, String queryId) {
    return getPath(apiData, queryId).toFile().exists();
  }

  private boolean exists(OgcApiDataV2 apiData) {
    return getPath(apiData).toFile().exists();
  }

  @Override
  public Date getLastModified(OgcApiDataV2 apiData, String queryId) {
    if (!exists(apiData, queryId)) {
      return null;
    }
    return Date.from(Instant.ofEpochMilli(getPath(apiData, queryId).toFile().lastModified()));
  }

  @Override
  public Date getLastModified(OgcApiDataV2 apiData) {
    if (!exists(apiData)) {
      return null;
    }
    return Date.from(Instant.ofEpochMilli(getPath(apiData).getParent().toFile().lastModified()));
  }

  @Override
  public Builder validate(Builder builder, OgcApiDataV2 apiData) {
    return builder;
  }

  @Override
  public Set<String> getIds(OgcApiDataV2 apiData) {
    return getAll(apiData).stream()
        .map(q -> q.getId().orElseThrow())
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public void writeStoredQueryDocument(OgcApiDataV2 apiData, String queryId, QueryExpression query)
      throws IOException {
    QueryExpression.toFile(query, getPath(apiData, queryId));
  }

  @Override
  public void deleteStoredQuery(OgcApiDataV2 apiData, String queryId) throws IOException {
    Files.deleteIfExists(getPath(apiData, queryId));
  }

  private Path getPath(OgcApiDataV2 apiData) {
    return store.resolve(apiData.getId());
  }

  private Path getPath(OgcApiDataV2 apiData, String queryId) {
    Path dir = getPath(apiData);
    if (!dir.toFile().exists()) dir.toFile().mkdirs();
    return dir.resolve(String.format("%s.json", queryId));
  }

  private Optional<String> getTitle(OgcApiDataV2 apiData, String queryId) {
    return get(apiData, queryId).getTitle();
  }
}
