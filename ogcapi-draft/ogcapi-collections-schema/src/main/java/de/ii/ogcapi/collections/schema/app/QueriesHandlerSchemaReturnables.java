/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.schema.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.schema.domain.QueriesHandlerSchema;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

@Singleton
@AutoBind
public class QueriesHandlerSchemaReturnables implements QueriesHandlerSchema {

  private static final String SCHEMA_TYPE_FEATURE = "feature";
  private static final String SCHEMA_TYPE_COLLECTION = "collection";

  private final FeaturesCoreProviders providers;
  private final I18n i18n;
  private final Map<Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final JsonSchemaCache schemaCache;
  private final JsonSchemaCache schemaCacheCollection;

  @Inject
  public QueriesHandlerSchemaReturnables(
      I18n i18n, FeaturesCoreProviders providers, EntityRegistry entityRegistry) {
    this.i18n = i18n;
    this.providers = providers;
    this.queryHandlers =
        ImmutableMap.of(
            Query.SCHEMA, QueryHandler.with(QueryInputSchema.class, this::getSchemaResponse));
    this.schemaCache =
        new SchemaCacheReturnables(() -> entityRegistry.getEntitiesForType(Codelist.class));
    this.schemaCacheCollection = new SchemaCacheReturnablesCollection();
  }

  @Override
  public List<String> getSupportedTypes() {
    return List.of(SCHEMA_TYPE_FEATURE, SCHEMA_TYPE_COLLECTION);
  }

  @Override
  public Map<Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  private Response getSchemaResponse(
      QueryInputSchema queryInput, ApiRequestContext requestContext) {

    return getResponse(queryInput, requestContext, providers, i18n);
  }

  @Override
  public JsonSchemaDocument getJsonSchema(
      FeatureSchema featureSchema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version,
      String type) {
    JsonSchemaDocument schema = null;

    if (type.equals(SCHEMA_TYPE_FEATURE)) {
      schema = schemaCache.getSchema(featureSchema, apiData, collectionData, schemaUri, version);
    } else if (type.equals(SCHEMA_TYPE_COLLECTION)) {
      schema =
          schemaCacheCollection.getSchema(
              featureSchema, apiData, collectionData, schemaUri, version);
    }

    return schema;
  }
}
