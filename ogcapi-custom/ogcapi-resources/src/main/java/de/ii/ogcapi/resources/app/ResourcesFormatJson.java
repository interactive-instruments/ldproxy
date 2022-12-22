/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.resources.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.resources.domain.ResourcesFormatExtension;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class ResourcesFormatJson implements ResourcesFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("JSON")
          .parameter("json")
          .build();

  private final Schema<?> schemaResources;
  private final Map<String, Schema<?>> referencedSchemas;

  @Inject
  public ResourcesFormatJson(ClassSchemaCache classSchemaCache) {
    schemaResources = classSchemaCache.getSchema(Resources.class);
    referencedSchemas = classSchemaCache.getReferencedSchemas(Resources.class);
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {

    // TODO add examples
    if (path.equals("/resources"))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schemaResources)
          .schemaRef(Resources.SCHEMA_REF)
          .referencedSchemas(referencedSchemas)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();

    throw new RuntimeException("Unexpected path: " + path);
  }

  @Override
  public Object getResourcesEntity(
      Resources resources, OgcApiDataV2 apiData, ApiRequestContext requestContext) {
    return resources;
  }
}
