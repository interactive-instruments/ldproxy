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
import de.ii.ogcapi.collections.schema.domain.SchemaFormatExtension;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument.VERSION;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import io.swagger.v3.oas.models.media.ObjectSchema;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class SchemaFormatJsonSchema202012 implements SchemaFormatExtension {
  static ApiMediaType JSON_SCHEMA_202012_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(
              new MediaType(
                  "application", "schema+json", ImmutableMap.of("schema", VERSION.V202012.url())))
          .label("JSON Schema (2020-12)")
          .parameter("json")
          .fileExtension("schema.json")
          .build();

  static ApiMediaTypeContent SCHEMA_CONTENT =
      new ImmutableApiMediaTypeContent.Builder()
          .schema(new ObjectSchema())
          .schemaRef("#/components/schemas/JsonSchema202012")
          .ogcApiMediaType(JSON_SCHEMA_202012_MEDIA_TYPE)
          .build();

  @Inject
  SchemaFormatJsonSchema202012() {}

  @Override
  public ApiMediaType getMediaType() {
    return JSON_SCHEMA_202012_MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return SCHEMA_CONTENT;
  }

  @Override
  public VERSION getVersion() {
    return VERSION.V202012;
  }
}
