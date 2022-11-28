/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.html;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.tiles.domain.ImmutableTileSetsView;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.domain.TileSets;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSetRepository;
import de.ii.xtraplatform.services.domain.ServicesContext;
import io.swagger.v3.oas.models.media.StringSchema;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class TileSetsFormatHtml implements TileSetsFormatExtension {

  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder().type(MediaType.TEXT_HTML_TYPE).parameter("html").build();

  private final I18n i18n;
  private final URI servicesUri;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  public TileSetsFormatHtml(
      I18n i18n, ServicesContext servicesContext, TileMatrixSetRepository tileMatrixSetRepository) {
    this.i18n = i18n;
    this.servicesUri = servicesContext.getUri();
    this.tileMatrixSetRepository = tileMatrixSetRepository;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public String getPathPattern() {
    return "^(?:/collections/" + COLLECTION_ID_PATTERN + ")?/tiles/?$";
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (path.equals("/tiles") || path.equals("/collections/{collectionId}/tiles"))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(new StringSchema().example("<html>...</html>"))
          .schemaRef("#/components/schemas/htmlSchema")
          .ogcApiMediaType(MEDIA_TYPE)
          .build();

    return null;
  }

  private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(HtmlConfiguration.class)
        .map(HtmlConfiguration::getNoIndexEnabled)
        .orElse(true);
  }

  @Override
  public Object getTileSetsEntity(
      TileSets tiles, Optional<String> collectionId, OgcApi api, ApiRequestContext requestContext) {
    String rootTitle = i18n.get("root", requestContext.getLanguage());
    String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());
    String tilesTitle = i18n.get("tilesTitle", requestContext.getLanguage());

    URICustomizer resourceUri = requestContext.getUriCustomizer().copy().clearParameters();
    final List<NavigationDTO> breadCrumbs =
        collectionId.isPresent()
            ? new ImmutableList.Builder<NavigationDTO>()
                .add(
                    new NavigationDTO(
                        rootTitle,
                        resourceUri
                            .copy()
                            .removeLastPathSegments(api.getData().getSubPath().size() + 3)
                            .toString()))
                .add(
                    new NavigationDTO(
                        api.getData().getLabel(),
                        resourceUri.copy().removeLastPathSegments(3).toString()))
                .add(
                    new NavigationDTO(
                        collectionsTitle, resourceUri.copy().removeLastPathSegments(2).toString()))
                .add(
                    new NavigationDTO(
                        api.getData().getCollections().get(collectionId.get()).getLabel(),
                        resourceUri.copy().removeLastPathSegments(1).toString()))
                .add(new NavigationDTO(tilesTitle))
                .build()
            : new ImmutableList.Builder<NavigationDTO>()
                .add(
                    new NavigationDTO(
                        rootTitle,
                        resourceUri
                            .copy()
                            .removeLastPathSegments(api.getData().getSubPath().size() + 1)
                            .toString()))
                .add(
                    new NavigationDTO(
                        api.getData().getLabel(),
                        resourceUri.copy().removeLastPathSegments(1).toString()))
                .add(new NavigationDTO(tilesTitle))
                .build();

    Optional<HtmlConfiguration> htmlConfig =
        collectionId.isPresent()
            ? api.getData().getExtension(HtmlConfiguration.class, collectionId.get())
            : api.getData().getExtension(HtmlConfiguration.class);

    Map<String, TileMatrixSet> tileMatrixSets = tileMatrixSetRepository.getAll();

    Optional<TilesConfiguration> tilesConfig =
        collectionId.isEmpty()
            ? api.getData().getExtension(TilesConfiguration.class)
            : api.getData().getExtension(TilesConfiguration.class, collectionId.get());
    MapClient.Type mapClientType =
        tilesConfig.map(TilesConfiguration::getMapClientType).orElse(MapClient.Type.MAP_LIBRE);
    String serviceUrl =
        new URICustomizer(servicesUri)
            .ensureLastPathSegments(api.getData().getSubPath().toArray(String[]::new))
            .toString();
    String styleUrl =
        htmlConfig
            .map(
                cfg ->
                    cfg.getStyle(
                        tilesConfig.map(TilesConfiguration::getStyle), collectionId, serviceUrl))
            .orElse(null);
    boolean removeZoomLevelConstraints =
        tilesConfig.map(TilesConfiguration::getRemoveZoomLevelConstraints).orElse(false);

    return new ImmutableTileSetsView.Builder()
        .apiData(api.getData())
        .tiles(tiles)
        .collectionId(collectionId)
        .unprocessedSpatialExtent(api.getSpatialExtent(collectionId))
        .unprocessedTemporalExtent(api.getTemporalExtent(collectionId))
        .tileMatrixSets(tileMatrixSets)
        .breadCrumbs(breadCrumbs)
        .links(tiles.getLinks())
        .urlPrefix(requestContext.getStaticUrlPrefix())
        .mapClientType(mapClientType)
        .styleUrl(styleUrl)
        .removeZoomLevelConstraints(removeZoomLevelConstraints)
        .htmlConfig(htmlConfig.orElseThrow())
        .noIndex(isNoIndexEnabledForApi(api.getData()))
        .uriCustomizer(requestContext.getUriCustomizer())
        .i18n(i18n)
        .description(tiles.getDescription().orElse(null))
        .title(tiles.getTitle().orElse(null))
        .language(requestContext.getLanguage())
        .build();
  }
}
