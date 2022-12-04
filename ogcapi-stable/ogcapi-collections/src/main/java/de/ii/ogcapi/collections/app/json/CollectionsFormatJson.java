/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.json;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.Collections;
import de.ii.ogcapi.collections.domain.CollectionsFormatExtension;
import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class CollectionsFormatJson implements CollectionsFormatExtension, ConformanceClass {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("JSON")
          .parameter("json")
          .build();

  private final Schema<?> schemaCollections;
  private final Map<String, Schema<?>> referencedSchemasCollections;
  private final Schema<?> schemaCollection;
  private final Map<String, Schema<?>> referencedSchemasCollection;

  @Inject
  public CollectionsFormatJson(ClassSchemaCache classSchemaCache) {
    schemaCollections = classSchemaCache.getSchema(Collections.class);
    referencedSchemasCollections = classSchemaCache.getReferencedSchemas(Collections.class);
    schemaCollection = classSchemaCache.getSchema(OgcApiCollection.class);
    referencedSchemasCollection = classSchemaCache.getReferencedSchemas(OgcApiCollection.class);
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-2/0.0/conf/json");
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, @NotNull String path) {

    if ("/collections".equals(path)) {
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schemaCollections)
          .schemaRef(Collections.SCHEMA_REF)
          .referencedSchemas(referencedSchemasCollections)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();
    } else if ("/collections/{collectionId}".equals(path)
        || path.matches("^/collections/" + COLLECTION_ID_PATTERN + "/?$")) {
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schemaCollection)
          .schemaRef(OgcApiCollection.SCHEMA_REF)
          .referencedSchemas(referencedSchemasCollection)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();
    }

    throw new IllegalStateException("Unexpected path: " + path);
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public Object getCollectionsEntity(
      Collections collections, OgcApi api, ApiRequestContext requestContext) {
    return collections;
  }

  @Override
  public Object getCollectionEntity(
      OgcApiCollection ogcApiCollection, OgcApi api, ApiRequestContext requestContext) {
    return ogcApiCollection;
  }
}
