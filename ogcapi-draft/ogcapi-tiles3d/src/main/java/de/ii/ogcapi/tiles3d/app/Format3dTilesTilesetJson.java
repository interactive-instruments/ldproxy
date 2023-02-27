/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles3d.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles3d.domain.Format3dTilesTileset;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.ogcapi.tiles3d.domain.Tileset;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class Format3dTilesTilesetJson implements Format3dTilesTileset {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("application", "json"))
          .label("3D Tiles JSON")
          .parameter("json")
          .build();

  private final Schema<?> schema;
  private final Map<String, Schema<?>> referencedSchemas;

  @Inject
  public Format3dTilesTilesetJson(ClassSchemaCache classSchemaCache) {
    schema = classSchemaCache.getSchema(Tileset.class);
    referencedSchemas = classSchemaCache.getReferencedSchemas(Tileset.class);
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Tiles3dConfiguration.class;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(getBuildingBlockConfigurationType())
        .map(ExtensionConfiguration::isEnabled)
        .orElse(false);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
        .getCollections()
        .get(collectionId)
        .getExtension(getBuildingBlockConfigurationType())
        .map(ExtensionConfiguration::isEnabled)
        .orElse(false);
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .referencedSchemas(referencedSchemas)
        .schemaRef(Tileset.SCHEMA_REF)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public Object getEntity(
      Tileset tileset,
      List<Link> links,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext) {
    return tileset;
  }
}
