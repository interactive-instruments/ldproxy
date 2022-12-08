/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.schema.domain.QueriesHandlerSchema;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument.VERSION;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.QueryHandler;
import de.ii.ogcapi.foundation.domain.QueryInput;
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
public class QueriesHandlerSchemaCrud implements QueriesHandlerSchema {

  private static final String SCHEMA_TYPE_RECEIVABLES = "receivables";
  private static final String SCHEMA_TYPE_CREATE = "create";
  private static final String SCHEMA_TYPE_REPLACE = "replace";
  private static final String SCHEMA_TYPE_UPDATE = "update";

  private final FeaturesCoreProviders providers;
  private final I18n i18n;
  private final Map<QueriesHandlerSchema.Query, QueryHandler<? extends QueryInput>> queryHandlers;
  private final JsonSchemaCache schemaCache;
  private final JsonSchemaCache schemaCacheCreate;
  private final JsonSchemaCache schemaCacheReplace;
  private final JsonSchemaCache schemaCacheUpdate;

  @Inject
  public QueriesHandlerSchemaCrud(
      I18n i18n, FeaturesCoreProviders providers, EntityRegistry entityRegistry) {
    this.i18n = i18n;
    this.providers = providers;
    this.queryHandlers =
        ImmutableMap.of(
            QueriesHandlerSchema.Query.SCHEMA,
            QueryHandler.with(
                QueriesHandlerSchema.QueryInputSchema.class, this::getSchemaResponse));
    this.schemaCache = new SchemaCacheReceivables(false, false, false, true);
    // TODO: removeId=false if id!=primaryKey
    this.schemaCacheCreate = new SchemaCacheReceivables(true, false, false, true);
    this.schemaCacheReplace = new SchemaCacheReceivables(true, false, false, true);
    this.schemaCacheUpdate = new SchemaCacheReceivables(true, true, true, true);
  }

  @Override
  public List<String> getSupportedTypes() {
    return List.of(
        SCHEMA_TYPE_RECEIVABLES, SCHEMA_TYPE_CREATE, SCHEMA_TYPE_REPLACE, SCHEMA_TYPE_UPDATE);
  }

  @Override
  public Map<QueriesHandlerSchema.Query, QueryHandler<? extends QueryInput>> getQueryHandlers() {
    return queryHandlers;
  }

  private Response getSchemaResponse(
      QueryInputSchema queryInput, ApiRequestContext requestContext) {

    // TODO: filter for PropertyTransformations that removes everything but flattening
    // visitor for FeatureSchema that filters properties with scope=QUERIES/MUTATIONS (and removes
    // constants)
    // apply in SchemaCacheReturnables
    // also apply in transformation pipeline to return with features with replace schema

    return getResponse(queryInput, requestContext, providers, i18n);
  }

  @Override
  public JsonSchemaAbstractDocument getJsonSchema(
      FeatureSchema featureSchema,
      OgcApiDataV2 apiData,
      FeatureTypeConfigurationOgcApi collectionData,
      Optional<String> schemaUri,
      VERSION version,
      String type) {

    switch (type) {
      case SCHEMA_TYPE_RECEIVABLES:
        return schemaCache.getSchema(featureSchema, apiData, collectionData, schemaUri, version);
      case SCHEMA_TYPE_CREATE:
        return schemaCacheCreate.getSchema(
            featureSchema, apiData, collectionData, schemaUri, version);
      case SCHEMA_TYPE_REPLACE:
        return schemaCacheReplace.getSchema(
            featureSchema, apiData, collectionData, schemaUri, version);
      case SCHEMA_TYPE_UPDATE:
        return schemaCacheUpdate.getSchema(
            featureSchema, apiData, collectionData, schemaUri, version);
      default:
        return null;
    }
  }
}
