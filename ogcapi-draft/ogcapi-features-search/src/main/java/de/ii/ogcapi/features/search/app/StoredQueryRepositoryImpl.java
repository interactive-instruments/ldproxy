/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.StoredQueriesFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryFormat;
import de.ii.ogcapi.features.search.domain.StoredQueryRepository;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.entities.ImmutableValidationResult;
import de.ii.xtraplatform.web.domain.LastModified;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO move to xtraplatform-spatial
@Singleton
@AutoBind
public class StoredQueryRepositoryImpl implements StoredQueryRepository, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredQueryRepositoryImpl.class);

  private final ExtensionRegistry extensionRegistry;
  private final BlobStore queriesStore;

  @Inject
  public StoredQueryRepositoryImpl(BlobStore blobStore, ExtensionRegistry extensionRegistry) {
    this.extensionRegistry = extensionRegistry;
    this.queriesStore = blobStore.with(SearchBuildingBlock.STORE_RESOURCE_TYPE);
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
    return getAllPaths(apiData).stream()
        .map(
            path -> {
              try {
                return QueryExpression.of(queriesStore.get(path).get());
              } catch (IOException e) {
                LogContext.error(LOGGER, e, "Could not parse stored query '{}'", path);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .collect(ImmutableList.toImmutableList());
  }

  public List<Path> getAllPaths(OgcApiDataV2 apiData) {
    Path parent = Path.of(apiData.getId());
    try (Stream<Path> fileStream =
        queriesStore.walk(
            parent,
            1,
            (path, attributes) ->
                attributes.isValue()
                    && !attributes.isHidden()
                    && path.toString().endsWith(".json"))) {
      return fileStream.sorted().map(parent::resolve).collect(ImmutableList.toImmutableList());
    } catch (IOException e) {
      LogContext.error(LOGGER, e, "Could not parse stored queries");
    }
    return ImmutableList.of();
  }

  @Override
  public QueryExpression get(OgcApiDataV2 apiData, String queryId) {
    if (!exists(apiData, queryId)) {
      throw new NotFoundException(
          MessageFormat.format("The stored query ''{0}'' does not exist in this API.", queryId));
    }

    try {
      return QueryExpression.of(queriesStore.get(getPath(apiData, queryId)).get());
    } catch (IOException e) {
      throw new IllegalStateException(
          MessageFormat.format("The stored query ''{0}'' could not be parsed.", queryId), e);
    }
  }

  @Override
  public boolean exists(OgcApiDataV2 apiData, String queryId) {
    try {
      return queriesStore.has(getPath(apiData, queryId));
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public Date getLastModified(OgcApiDataV2 apiData, String queryId) {
    if (exists(apiData, queryId)) {
      try {
        return LastModified.from(queriesStore.lastModified(getPath(apiData, queryId)));
      } catch (IOException e) {
        // continue
      }
    }
    return null;
  }

  @Override
  public Date getLastModified(OgcApiDataV2 apiData) {
    return getAllPaths(apiData).stream()
        .map(
            path -> {
              try {
                return queriesStore.lastModified(path);
              } catch (IOException e) {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .map(LastModified::from)
        .orElse(null);
  }

  @Override
  public ImmutableValidationResult.Builder validate(
      ImmutableValidationResult.Builder builder, OgcApiDataV2 apiData) {
    getAllPaths(apiData)
        .forEach(
            path -> {
              try {
                QueryExpression.of(queriesStore.get(path).get());
              } catch (Exception e) {
                builder.addErrors(
                    MessageFormat.format(
                        "Could not parse stored query ''{0}''. Reason: {1}.",
                        path.toString(), e.getMessage()));
              }
            });

    return builder;
  }

  @Override
  public Set<String> getIds(OgcApiDataV2 apiData) {
    return getAll(apiData).stream()
        .map(QueryExpression::getId)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public void writeStoredQueryDocument(OgcApiDataV2 apiData, String queryId, QueryExpression query)
      throws IOException {
    byte[] bytes = QueryExpression.asBytes(query);
    queriesStore.put(getPath(apiData, queryId), new ByteArrayInputStream(bytes));
  }

  @Override
  public void deleteStoredQuery(OgcApiDataV2 apiData, String queryId) throws IOException {
    queriesStore.delete(getPath(apiData, queryId));
  }

  private Path getPath(OgcApiDataV2 apiData) {
    return Path.of(apiData.getId());
  }

  private Path getPath(OgcApiDataV2 apiData, String queryId) {
    return getPath(apiData).resolve(String.format("%s.json", queryId));
  }
}
