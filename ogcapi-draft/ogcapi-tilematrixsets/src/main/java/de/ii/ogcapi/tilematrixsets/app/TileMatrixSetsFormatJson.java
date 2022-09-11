/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ClassSchemaCache;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetData;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSets;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsFormatExtension;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class TileMatrixSetsFormatJson implements TileMatrixSetsFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.APPLICATION_JSON_TYPE)
          .label("JSON")
          .parameter("json")
          .build();

  private final Schema<?> schemaStyleTileMatrixSets;
  private final Map<String, Schema<?>> referencedSchemasTileMatrixSets;
  private final Schema<?> schemaStyleTileMatrixSet;
  private final Map<String, Schema<?>> referencedSchemasTileMatrixSet;

  @Inject
  public TileMatrixSetsFormatJson(ClassSchemaCache classSchemaCache) {
    schemaStyleTileMatrixSet = classSchemaCache.getSchema(TileMatrixSetData.class);
    referencedSchemasTileMatrixSet = classSchemaCache.getReferencedSchemas(TileMatrixSetData.class);
    schemaStyleTileMatrixSets = classSchemaCache.getSchema(TileMatrixSets.class);
    referencedSchemasTileMatrixSets = classSchemaCache.getReferencedSchemas(TileMatrixSets.class);
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (path.equals("/tileMatrixSets"))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schemaStyleTileMatrixSets)
          .schemaRef(TileMatrixSets.SCHEMA_REF)
          .referencedSchemas(referencedSchemasTileMatrixSets)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();
    else if (path.equals("/tileMatrixSets/{tileMatrixSetId}"))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(schemaStyleTileMatrixSet)
          .schemaRef(TileMatrixSetData.SCHEMA_REF)
          .referencedSchemas(referencedSchemasTileMatrixSet)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();

    throw new RuntimeException("Unexpected path: " + path);
  }

  @Override
  public Object getTileMatrixSetsEntity(
      TileMatrixSets tileMatrixSets, OgcApi api, ApiRequestContext requestContext) {
    return tileMatrixSets;
  }

  @Override
  public Object getTileMatrixSetEntity(
      TileMatrixSetData tileMatrixSet, OgcApi api, ApiRequestContext requestContext) {
    return tileMatrixSet;
  }
}
