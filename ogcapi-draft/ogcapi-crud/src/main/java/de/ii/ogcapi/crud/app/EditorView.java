/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.crud.app;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.ImmutableMapClient.Builder;
import de.ii.ogcapi.html.domain.ImmutableSource;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.MapClient.Popup;
import de.ii.ogcapi.html.domain.MapClient.Source.TYPE;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.ogcapi.html.domain.OgcApiView;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class EditorView extends OgcApiView {

  private static final Logger LOGGER = LoggerFactory.getLogger(EditorView.class);

  private URI uri;
  public Map<String, String> bbox;
  public URICustomizer uriBuilder;
  public URICustomizer uriBuilderWithFOnly;
  public final MapClient mapClient;

  public final String attribution;
  public final String collectionId;

  public EditorView(
      OgcApiDataV2 apiData,
      String collectionId,
      Optional<BoundingBox> spatialExtent,
      URI uri,
      String attribution,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      Type mapClientType,
      String styleUrl,
      boolean removeZoomLevelConstraints) {
    super(
        "editor.mustache",
        Charsets.UTF_8,
        apiData,
        List.of(),
        htmlConfig,
        true,
        urlPrefix,
        List.of(),
        apiData.getId(),
        "");
    this.uri = uri;
    this.uriBuilder = new URICustomizer(uri);
    this.attribution = attribution;
    this.collectionId = collectionId;

    this.bbox =
        spatialExtent
            .map(
                boundingBox ->
                    ImmutableMap.of(
                        "minLng", Double.toString(boundingBox.getXmin()),
                        "minLat", Double.toString(boundingBox.getYmin()),
                        "maxLng", Double.toString(boundingBox.getXmax()),
                        "maxLat", Double.toString(boundingBox.getYmax())))
            .orElse(null);

    if (mapClientType.equals(Type.OPEN_LAYERS)) {
      this.mapClient =
          new Builder()
              .backgroundUrl(
                  Optional.ofNullable(htmlConfig.getLeafletUrl())
                      .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
              .attribution(getAttribution())
              .bounds(Optional.ofNullable(bbox))
              .type(mapClientType)
              .data(
                  new ImmutableSource.Builder()
                      .type(TYPE.geojson)
                      .url(
                          uriBuilder
                              .cutPathAfterSegments(apiData.getSubPath().toArray(String[]::new))
                              .clearParameters()
                              .toString())
                      .putLayers(collectionId, List.of(collectionId))
                      .build())
              .popup(Popup.HOVER_ID)
              /*.styleUrl(Optional.ofNullable(styleUrl))
              .removeZoomLevelConstraints(removeZoomLevelConstraints)*/
              .build();
    } else {
      LOGGER.error(
          "Configuration error: {} is not a supported map client for the CRUD Editor.",
          mapClientType);
      this.mapClient = null;
    }
  }

  @Override
  public String getAttribution() {
    String basemapAttribution = super.getAttribution();
    if (Objects.nonNull(attribution)) {
      if (Objects.nonNull(basemapAttribution))
        return String.join(" | ", attribution, basemapAttribution);
      else return attribution;
    }
    return basemapAttribution;
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
