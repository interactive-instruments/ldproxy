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
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.styles.domain.StyleFormatExtension;
import de.ii.ogcapi.styles.domain.StyleRepository;
import de.ii.ogcapi.tiles3d.domain.Format3dTilesTileset;
import de.ii.ogcapi.tiles3d.domain.Tiles3dConfiguration;
import de.ii.ogcapi.tiles3d.domain.Tileset;
import de.ii.xtraplatform.services.domain.ServicesContext;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class Format3DTilesTilesetHtml implements Format3dTilesTileset {

  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.TEXT_HTML_TYPE)
          .label("HTML")
          .parameter("html")
          .build();

  private static final String SCHEMA_REF = "#/components/schemas/htmlSchema";
  private final Schema<?> schema;
  private final URI servicesUri;
  private final StyleRepository styleRepository;

  @Inject
  public Format3DTilesTilesetHtml(
      ServicesContext servicesContext, StyleRepository styleRepository) {
    this.servicesUri = servicesContext.getUri();
    this.styleRepository = styleRepository;
    this.schema = new StringSchema().example("<html>...</html>");
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(Tiles3dConfiguration.class)
        .filter(
            config ->
                config.isEnabled()
                    && (config.shouldClampToEllipsoid()
                        || config.getIonAccessToken().isPresent()
                        || config.getMaptilerApiKey().isPresent()))
        .isPresent();
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return Tiles3dConfiguration.class;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(SCHEMA_REF)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public Object getEntity(
      Tileset tileset,
      List<Link> links,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext) {

    URICustomizer uriCustomizer =
        new URICustomizer(servicesUri)
            .ensureLastPathSegments(api.getData().getSubPath().toArray(String[]::new));
    String serviceUrl = uriCustomizer.toString();
    Tiles3dConfiguration tiles3dConfig =
        api.getData().getExtension(Tiles3dConfiguration.class, collectionId).orElseThrow();
    HtmlConfiguration htmlConfig =
        api.getData().getExtension(HtmlConfiguration.class, collectionId).orElseThrow();
    String styleId = tiles3dConfig.getStyle();
    if (Objects.isNull(styleId) || "DEFAULT".equals(styleId)) {
      styleId = htmlConfig.getDefaultStyle();
    }

    Optional<StyleFormatExtension> format =
        styleRepository
            .getStyleFormatStream(api.getData(), Optional.ofNullable(collectionId))
            .filter(f -> "3dtiles".equals(f.getFileExtension()))
            .findFirst();
    Optional<String> style =
        Objects.isNull(styleId) || "NONE".equals(styleId) || format.isEmpty()
            ? Optional.empty()
            : Optional.of(
                new String(
                    styleRepository
                        .getStylesheet(
                            api.getData(),
                            Optional.ofNullable(collectionId),
                            styleId,
                            format.get(),
                            requestContext)
                        .getContent(),
                    StandardCharsets.UTF_8));
    return ImmutableTilesetView.builder()
        .apiData(api.getData())
        .collectionId(collectionId)
        .tileset(tileset)
        .urlPrefix(requestContext.getStaticUrlPrefix())
        .style(style)
        .htmlConfig(htmlConfig)
        .rawLinks(links)
        .uriCustomizer(requestContext.getUriCustomizer().copy())
        .user(requestContext.getUser())
        .build();
  }
}
