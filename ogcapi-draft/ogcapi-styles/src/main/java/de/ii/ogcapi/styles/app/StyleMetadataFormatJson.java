/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.styles.domain.StyleMetadata;
import de.ii.ogcapi.styles.domain.StyleMetadataFormatExtension;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @title JSON
 */
@Singleton
@AutoBind
public class StyleMetadataFormatJson implements StyleMetadataFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("JSON")
          .parameter("json")
          .build();

  private final Schema<?> schemaStyleMetadata;
  private final Map<String, Schema<?>> referencedSchemas;

  @Inject
  public StyleMetadataFormatJson(ClassSchemaCache classSchemaCache) {
    schemaStyleMetadata = classSchemaCache.getSchema(StyleMetadata.class);
    referencedSchemas = classSchemaCache.getReferencedSchemas(StyleMetadata.class);
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public boolean canSupportTransactions() {
    return true;
  }

  @Override
  public Object getStyleMetadataEntity(
      StyleMetadata metadata,
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      ApiRequestContext requestContext) {
    return metadata;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

    // TODO add examples
    if (path.endsWith("/styles/{styleId}/metadata"))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schemaStyleMetadata)
          .schemaRef(StyleMetadata.SCHEMA_REF)
          .referencedSchemas(referencedSchemas)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();

    throw new RuntimeException("Unexpected path: " + path);
  }

  @Override
  public ApiMediaTypeContent getRequestContent(
      OgcApiDataV2 apiData, String path, HttpMethods method) {
    if (path.endsWith("/styles/{styleId}/metadata")
        && (method == HttpMethods.PUT || method == HttpMethods.PATCH))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schemaStyleMetadata)
          .schemaRef(StyleMetadata.SCHEMA_REF)
          .referencedSchemas(referencedSchemas)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();

    throw new RuntimeException("Unexpected path: " + path);
  }

  public StyleMetadata parse(byte[] content, boolean strict, boolean inStore) {

    // prepare Jackson mapper for deserialization
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, strict);
    StyleMetadata parsedContent;
    try {
      // parse input
      parsedContent = mapper.readValue(content, StyleMetadata.class);
    } catch (IOException e) {
      if (inStore) {
        // this is invalid style metadata already in the store: server error
        throw new RuntimeException("The style metadata is invalid.", e);
      } else {
        // style metadata provided by a client: client error
        throw new IllegalArgumentException("The style metadata is invalid.", e);
      }
    }

    return parsedContent;
  }
}
