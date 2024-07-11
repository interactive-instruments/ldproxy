/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.collections.domain.CollectionFormatExtension;
import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title JSON
 */
@Singleton
@AutoBind
public class CollectionFormatJson implements CollectionFormatExtension, ConformanceClass {

  private final Schema<?> schemaCollection;
  private final Map<String, Schema<?>> referencedSchemasCollection;
  private final ObjectMapper mapper;

  @Inject
  public CollectionFormatJson(ClassSchemaCache classSchemaCache) {
    schemaCollection = classSchemaCache.getSchema(OgcApiCollection.class);
    referencedSchemasCollection = classSchemaCache.getReferencedSchemas(OgcApiCollection.class);
    mapper =
        new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .registerModule(new Jdk8Module())
            .registerModule(new GuavaModule())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    return ImmutableList.of("http://www.opengis.net/spec/ogcapi-common-2/0.0/conf/json");
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schemaCollection)
        .schemaRef(OgcApiCollection.SCHEMA_REF)
        .referencedSchemas(referencedSchemasCollection)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return ApiMediaType.JSON_MEDIA_TYPE;
  }

  @Override
  public Object getEntity(
      OgcApiCollection ogcApiCollection, OgcApi api, ApiRequestContext requestContext) {
    try {
      return mapper.writeValueAsBytes(ogcApiCollection);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
