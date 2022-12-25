/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tilematrixsets.app;

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
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetOgcApi;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSets;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetsFormatExtension;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class TileMatrixSetsFormatHtml implements TileMatrixSetsFormatExtension {

  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder().type(MediaType.TEXT_HTML_TYPE).parameter("html").build();

  private final I18n i18n;

  @Inject
  public TileMatrixSetsFormatHtml(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(new StringSchema().example("<html>...</html>"))
        .schemaRef("#/components/schemas/htmlSchema")
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  private boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(HtmlConfiguration.class)
        .map(HtmlConfiguration::getNoIndexEnabled)
        .orElse(true);
  }

  @Override
  public Object getTileMatrixSetsEntity(
      TileMatrixSets tileMatrixSets, OgcApi api, ApiRequestContext requestContext) {
    String rootTitle = i18n.get("root", requestContext.getLanguage());
    String tileMatrixSetsTitle = i18n.get("tileMatrixSetsTitle", requestContext.getLanguage());

    URICustomizer resourceUri = requestContext.getUriCustomizer().copy().clearParameters();
    final List<NavigationDTO> breadCrumbs =
        new ImmutableList.Builder<NavigationDTO>()
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
            .add(new NavigationDTO(tileMatrixSetsTitle))
            .build();

    HtmlConfiguration htmlConfig = api.getData().getExtension(HtmlConfiguration.class).orElse(null);

    return ImmutableTileMatrixSetsView.builder()
        .apiData(api.getData())
        .urlPrefix(requestContext.getStaticUrlPrefix())
        .tileMatrixSets(tileMatrixSets.getTileMatrixSets())
        .breadCrumbs(breadCrumbs)
        .htmlConfig(htmlConfig)
        .noIndex(isNoIndexEnabledForApi(api.getData()))
        .uriCustomizer(requestContext.getUriCustomizer())
        .i18n(i18n)
        .language(requestContext.getLanguage().orElse(null))
        .description(i18n.get("tileMatrixSetsDescription", requestContext.getLanguage()))
        .title(i18n.get("tileMatrixSetsTitle", requestContext.getLanguage()))
        .rawLinks(tileMatrixSets.getLinks())
        .build();
  }

  @Override
  public Object getTileMatrixSetEntity(
      TileMatrixSetOgcApi tileMatrixSet, OgcApi api, ApiRequestContext requestContext) {
    String rootTitle = i18n.get("root", requestContext.getLanguage());
    String tileMatrixSetsTitle = i18n.get("tileMatrixSetsTitle", requestContext.getLanguage());
    String title = tileMatrixSet.getTitle().orElse(tileMatrixSet.getId());

    URICustomizer resourceUri = requestContext.getUriCustomizer().copy().clearParameters();
    final List<NavigationDTO> breadCrumbs =
        new ImmutableList.Builder<NavigationDTO>()
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
            .add(
                new NavigationDTO(
                    tileMatrixSetsTitle, resourceUri.copy().removeLastPathSegments(1).toString()))
            .add(new NavigationDTO(title))
            .build();

    HtmlConfiguration htmlConfig = api.getData().getExtension(HtmlConfiguration.class).orElse(null);
    return ImmutableTileMatrixSetView.builder()
        .apiData(api.getData())
        .tileMatrixSet(tileMatrixSet)
        .breadCrumbs(breadCrumbs)
        .rawLinks(tileMatrixSet.getLinks())
        .urlPrefix(requestContext.getStaticUrlPrefix())
        .htmlConfig(htmlConfig)
        .noIndex(isNoIndexEnabledForApi(api.getData()))
        .uriCustomizer(requestContext.getUriCustomizer())
        .i18n(i18n)
        .language(requestContext.getLanguage())
        .build();
  }
}
