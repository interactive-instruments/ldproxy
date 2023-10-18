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
import de.ii.xtraplatform.entities.domain.ImmutableValidationResult;
import de.ii.xtraplatform.values.domain.KeyValueStore;
import de.ii.xtraplatform.values.domain.ValueStore;
import de.ii.xtraplatform.web.domain.LastModified;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class StoredQueryRepositoryImpl implements StoredQueryRepository, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredQueryRepositoryImpl.class);

  private final ExtensionRegistry extensionRegistry;
  private final KeyValueStore<QueryExpression> queriesStore;

  @Inject
  public StoredQueryRepositoryImpl(ValueStore valueStore, ExtensionRegistry extensionRegistry) {
    this.extensionRegistry = extensionRegistry;
    this.queriesStore = valueStore.forType(QueryExpression.class);
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
    return queriesStore.identifiers(apiData.getId()).stream()
        .map(queriesStore::get)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public QueryExpression get(OgcApiDataV2 apiData, String queryId) {
    if (!exists(apiData, queryId)) {
      throw new NotFoundException(
          MessageFormat.format("The stored query ''{0}'' does not exist in this API.", queryId));
    }

    return queriesStore.get(queryId, apiData.getId());
  }

  @Override
  public boolean exists(OgcApiDataV2 apiData, String queryId) {
    return queriesStore.has(queryId, apiData.getId());
  }

  @Override
  public Date getLastModified(OgcApiDataV2 apiData, String queryId) {
    if (exists(apiData, queryId)) {
      return LastModified.from(queriesStore.lastModified(queryId, apiData.getId()));
    }
    return null;
  }

  @Override
  public Date getLastModified(OgcApiDataV2 apiData) {
    return queriesStore.identifiers(apiData.getId()).stream()
        .map(queriesStore::lastModified)
        .max(Comparator.naturalOrder())
        .map(LastModified::from)
        .orElse(null);
  }

  @Override
  public ImmutableValidationResult.Builder validate(
      ImmutableValidationResult.Builder builder, OgcApiDataV2 apiData) {
    queriesStore
        .identifiers(apiData.getId())
        .forEach(
            identifier -> {
              try {
                queriesStore.get(identifier);
              } catch (Throwable e) {
                builder.addErrors(
                    MessageFormat.format(
                        "Could not parse stored query ''{0}''. Reason: {1}.",
                        identifier.asPath(), e.getMessage()));
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
    try {
      queriesStore.put(queryId, query, apiData.getId()).join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    }
  }

  @Override
  public void deleteStoredQuery(OgcApiDataV2 apiData, String queryId) throws IOException {
    try {
      queriesStore.delete(queryId, apiData.getId()).join();
    } catch (CompletionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    }
  }
}
