/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.search.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.search.domain.QueryExpression;
import de.ii.ogcapi.features.search.domain.StoredQueries;
import de.ii.ogcapi.features.search.domain.StoredQueriesFormat;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class StoredQueriesFormatJson implements StoredQueriesFormat {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoredQueriesFormatJson.class);

  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.APPLICATION_JSON_TYPE)
          .label("JSON")
          .parameter("json")
          .fileExtension("json")
          .build();

  private final Schema<?> schema;
  private final Map<String, Schema<?>> referencedSchemas;

  @Inject
  public StoredQueriesFormatJson(ClassSchemaCache classSchemaCache) {
    this.schema = classSchemaCache.getSchema(QueryExpression.class);
    referencedSchemas = classSchemaCache.getReferencedSchemas(QueryExpression.class);
  }

  @Override
  public boolean canSupportTransactions() {
    return true;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(QueryExpression.SCHEMA_REF)
        .referencedSchemas(referencedSchemas)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public ApiMediaTypeContent getRequestContent(
      OgcApiDataV2 apiData, String path, HttpMethods method) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(QueryExpression.SCHEMA_REF)
        .referencedSchemas(referencedSchemas)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public Object getEntity(
      StoredQueries queries, OgcApiDataV2 apiData, ApiRequestContext requestContext) {
    return queries;
  }
}
