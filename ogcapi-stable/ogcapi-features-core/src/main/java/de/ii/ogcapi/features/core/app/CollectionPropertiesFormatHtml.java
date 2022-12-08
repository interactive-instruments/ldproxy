/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.core.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesFormat;
import de.ii.ogcapi.features.core.domain.CollectionPropertiesType;
import de.ii.ogcapi.features.core.domain.JsonSchemaAbstractDocument;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class CollectionPropertiesFormatHtml implements CollectionPropertiesFormat {

  static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder().type(MediaType.TEXT_HTML_TYPE).parameter("html").build();

  private final I18n i18n;

  @Inject
  public CollectionPropertiesFormatHtml(I18n i18n) {
    this.i18n = i18n;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(getBuildingBlockConfigurationType())
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false)
        && apiData
            .getExtension(HtmlConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
            .getCollections()
            .get(collectionId)
            .getExtension(getBuildingBlockConfigurationType())
            .map(ExtensionConfiguration::isEnabled)
            .orElse(false)
        && apiData
            .getCollections()
            .get(collectionId)
            .getExtension(HtmlConfiguration.class)
            .map(ExtensionConfiguration::isEnabled)
            .orElse(true);
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
  public Object getEntity(
      JsonSchemaAbstractDocument schemaProperties,
      CollectionPropertiesType type,
      List<Link> links,
      String collectionId,
      OgcApi api,
      ApiRequestContext requestContext) {
    String rootTitle = i18n.get("root", requestContext.getLanguage());
    String collectionsTitle = i18n.get("collectionsTitle", requestContext.getLanguage());
    String collectionPropertiesTitle =
        i18n.get(type.toString() + "Title", requestContext.getLanguage());

    URICustomizer resourceUri = requestContext.getUriCustomizer().copy().clearParameters();
    final List<NavigationDTO> breadCrumbs =
        new ImmutableList.Builder<NavigationDTO>()
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
                    api.getData().getCollections().get(collectionId).getLabel(),
                    resourceUri.copy().removeLastPathSegments(1).toString()))
            .add(new NavigationDTO(collectionPropertiesTitle))
            .build();

    HtmlConfiguration htmlConfig =
        api.getData()
            .getCollections()
            .get(collectionId)
            .getExtension(HtmlConfiguration.class)
            .orElse(null);

    return new CollectionPropertiesView(
        api.getData(),
        schemaProperties,
        type,
        links,
        breadCrumbs,
        requestContext.getStaticUrlPrefix(),
        htmlConfig,
        isNoIndexEnabledForApi(api.getData()),
        i18n,
        requestContext.getLanguage());
  }
}
