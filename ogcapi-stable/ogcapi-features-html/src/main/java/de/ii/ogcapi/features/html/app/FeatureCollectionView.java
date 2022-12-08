/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import de.ii.ogcapi.features.html.domain.FeatureCollectionBaseView;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * @author zahnen
 */
public class FeatureCollectionView extends FeatureCollectionBaseView {

  public FeatureCollectionView(
      Optional<BoundingBox> spatialExtent,
      String template,
      URI uri,
      String name,
      String title,
      String description,
      String attribution,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      String persistentUri,
      boolean noIndex,
      POSITION mapPosition,
      Type mapClientType,
      String styleUrl,
      boolean removeZoomLevelConstraints,
      boolean hideMap,
      Map<String, String> queryables,
      List<String> geometryProperties) {
    super(
        spatialExtent,
        template,
        uri,
        name,
        title,
        description,
        attribution,
        urlPrefix,
        htmlConfig,
        persistentUri,
        noIndex,
        mapPosition,
        mapClientType,
        styleUrl,
        removeZoomLevelConstraints,
        hideMap,
        geometryProperties);

    if (Objects.nonNull(queryables)) {
      filterEditor =
          new ImmutableFilterEditor.Builder()
              .backgroundUrl(
                  Optional.ofNullable(htmlConfig.getLeafletUrl())
                      .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
              .attribution(htmlConfig.getBasemapAttribution())
              .fields(queryables.entrySet())
              .build();
    }
  }

  public String getQueryWithoutPage() {
    List<NameValuePair> query =
        URLEncodedUtils.parse(getQuery().substring(1), Charset.forName("utf-8")).stream()
            .filter(kvp -> !kvp.getName().equals("offset") && !kvp.getName().equals("limit"))
            .collect(Collectors.toList());

    return '?' + URLEncodedUtils.format(query, '&', Charset.forName("utf-8")) + '&';
  }

  public Function<String, String> getCurrentUrlWithSegment() {
    return segment ->
        uriBuilderWithFOnly
            .copy()
            .ensureLastPathSegment(segment)
            .ensureNoTrailingSlash()
            .toString();
  }

  public Function<String, String> getCurrentUrlWithSegmentClearParams() {
    return segment ->
        uriBuilder
            .copy()
            .ensureLastPathSegment(segment)
            .ensureNoTrailingSlash()
            .clearParameters()
            .toString();
  }
}
