/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.domain;

import static de.ii.ogcapi.features.core.domain.SchemaGeneratorFeatureOpenApi.DEFAULT_FLATTENING_SEPARATOR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeatureFormatExtension;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation.Builder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.WithTransformationsApplied;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import javax.ws.rs.core.MediaType;

public abstract class FeaturesFormatBaseHtml implements FeatureFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.TEXT_HTML_TYPE)
          .label("HTML")
          .parameter("html")
          .build();
  public static final ApiMediaType COLLECTION_MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(MediaType.TEXT_HTML_TYPE)
          .label("HTML")
          .parameter("html")
          .build();

  protected final Schema<?> schema = new StringSchema().example("<html>...</html>");
  protected static final String schemaRef = "#/components/schemas/htmlSchema";
  protected static final WithTransformationsApplied SCHEMA_FLATTENER =
      new WithTransformationsApplied(
          ImmutableMap.of(
              PropertyTransformations.WILDCARD,
              new Builder().flatten(DEFAULT_FLATTENING_SEPARATOR).build()));

  @Override
  public ApiMediaType getCollectionMediaType() {
    return COLLECTION_MEDIA_TYPE;
  }

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (!path.matches(getPathPattern())) {
      return null; // TODO
    }
    return new ImmutableApiMediaTypeContent.Builder()
        .schema(schema)
        .schemaRef(schemaRef)
        .ogcApiMediaType(MEDIA_TYPE)
        .build();
  }

  protected boolean isNoIndexEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(HtmlConfiguration.class)
        .map(HtmlConfiguration::getNoIndexEnabled)
        .orElse(true);
  }

  protected POSITION getMapPosition(OgcApiDataV2 apiData) {
    return apiData
        .getExtension(FeaturesHtmlConfiguration.class)
        .map(FeaturesHtmlConfiguration::getMapPosition)
        .orElse(POSITION.AUTO);
  }

  protected POSITION getMapPosition(OgcApiDataV2 apiData, String collectionId) {
    return apiData
        .getExtension(FeaturesHtmlConfiguration.class, collectionId)
        .map(FeaturesHtmlConfiguration::getMapPosition)
        .orElse(POSITION.AUTO);
  }

  protected List<String> getGeometryProperties(OgcApiDataV2 apiData, String collectionId) {
    return apiData
        .getExtension(FeaturesHtmlConfiguration.class, collectionId)
        .map(FeaturesHtmlConfiguration::getGeometryProperties)
        .orElse(ImmutableList.of());
  }

  @Override
  public boolean canEncodeFeatures() {
    return true;
  }
}
