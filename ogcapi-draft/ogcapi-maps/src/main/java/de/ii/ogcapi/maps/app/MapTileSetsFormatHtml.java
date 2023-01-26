/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.maps.domain.MapTileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.ImmutableTileSetsView;
import de.ii.ogcapi.tiles.domain.TileSets;
import de.ii.xtraplatform.tiles.domain.TileMatrixSet;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title HTML
 */
@Singleton
@AutoBind
public class MapTileSetsFormatHtml implements MapTileSetsFormatExtension {

  private final I18n i18n;
  private final TileMatrixSetRepository tileMatrixSetRepository;

  @Inject
  public MapTileSetsFormatHtml(I18n i18n, TileMatrixSetRepository tileMatrixSetRepository) {
    this.i18n = i18n;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
  }

  @Override
  public ApiMediaType getMediaType() {
    return ApiMediaType.HTML_MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent() {
    return FormatExtension.HTML_CONTENT;
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
    String tilesTitle = i18n.get("mapTilesTitle", requestContext.getLanguage());

    URICustomizer resourceUri = requestContext.getUriCustomizer().copy().clearParameters();
    final List<NavigationDTO> breadCrumbs =
        collectionId.isPresent()
            ? new ImmutableList.Builder<NavigationDTO>()
                .add(
                    new NavigationDTO(
                        rootTitle,
                        resourceUri
                            .copy()
                            .removeLastPathSegments(api.getData().getSubPath().size() + 4)
                            .toString()))
                .add(
                    new NavigationDTO(
                        api.getData().getLabel(),
                        resourceUri.copy().removeLastPathSegments(4).toString()))
                .add(
                    new NavigationDTO(
                        collectionsTitle, resourceUri.copy().removeLastPathSegments(3).toString()))
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
                            .removeLastPathSegments(api.getData().getSubPath().size() + 2)
                            .toString()))
                .add(
                    new NavigationDTO(
                        api.getData().getLabel(),
                        resourceUri.copy().removeLastPathSegments(2).toString()))
                .add(new NavigationDTO(tilesTitle))
                .build();

    Optional<HtmlConfiguration> htmlConfig =
        collectionId.isPresent()
            ? api.getData().getExtension(HtmlConfiguration.class, collectionId.get())
            : api.getData().getExtension(HtmlConfiguration.class);

    Map<String, TileMatrixSet> tileMatrixSets = tileMatrixSetRepository.getAll();

    return new ImmutableTileSetsView.Builder()
        .apiData(api.getData())
        .tiles(tiles)
        .collectionId(collectionId)
        .tileMatrixSets(tileMatrixSets)
        .breadCrumbs(breadCrumbs)
        .rawLinks(tiles.getLinks())
        .urlPrefix(requestContext.getStaticUrlPrefix())
        .mapClientType(Type.MAP_LIBRE)
        .styleUrl(null)
        .removeZoomLevelConstraints(false)
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
