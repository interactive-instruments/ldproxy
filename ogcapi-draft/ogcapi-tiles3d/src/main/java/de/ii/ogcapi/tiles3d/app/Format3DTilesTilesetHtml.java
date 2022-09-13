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
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles3d.domain.Format3dTilesTileset;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.ogcapi.tiles3d.domain.Tileset3dTiles;
import de.ii.xtraplatform.services.domain.ServicesContext;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class Format3DTilesTilesetHtml implements Format3dTilesTileset {

  private static final Logger LOGGER = LoggerFactory.getLogger(Format3DTilesTilesetHtml.class);
  public static final String MEDIA_TYPE_STRING = "application/json";
  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.TEXT_HTML_TYPE)
          .label("HTML")
          .parameter("html")
          .build();

  private final Schema<?> schema;
  private final URI servicesUri;
  public static final String SCHEMA_REF_STYLE = "#/components/schemas/htmlSchema";

  @Inject
  public Format3DTilesTilesetHtml(ServicesContext servicesContext) {
    schema = new StringSchema().example("<html>...</html>");
    this.servicesUri = servicesContext.getUri();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(Tiles3dConfiguration.class)
        .filter(
            config ->
                config.isEnabled()
                    && (!config.shouldClampToEllipsoid()
                        || config.getIonAccessToken().isPresent()
                        || config.getMaptilerApiKey().isPresent()))
        .isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Tiles3dConfiguration.class;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(SCHEMA_REF_STYLE)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public Object getEntity(
      Tileset3dTiles tileset,
      List<Link> links,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext) {

    return new Tileset3dTilesView(tileset, api, collectionId, requestContext.getStaticUrlPrefix());
  }
}
