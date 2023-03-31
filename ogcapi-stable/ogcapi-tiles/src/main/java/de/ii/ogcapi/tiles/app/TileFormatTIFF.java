/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ConformanceClass;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

/**
 * @title TIFF
 */
@Singleton
@AutoBind
public class TileFormatTIFF extends TileFormatExtension implements ConformanceClass {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("image", "tiff"))
          .label("TIFF")
          .parameter("tiff")
          .build();

  @Inject
  TileFormatTIFF() {}

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(BINARY_SCHEMA)
        .schemaRef(BINARY_SCHEMA_REF)
        .ogcApiMediaType(getMediaType())
        .build();
  }

  @Override
  public String getExtension() {
    return "tiff";
  }

  @Override
  public DataType getDataType() {
    return TileSet.DataType.map;
  }

  @Override
  public List<String> getConformanceClassUris(OgcApiDataV2 apiData) {
    if (isEnabledForApi(apiData)
        || apiData.getCollections().keySet().stream()
            .anyMatch(collectionId -> isEnabledForApi(apiData, collectionId))) {
      return ImmutableList.of("http://www.opengis.net/spec/ogcapi-tiles-1/1.0/conf/tiff");
    }

    return ImmutableList.of();
  }
}
