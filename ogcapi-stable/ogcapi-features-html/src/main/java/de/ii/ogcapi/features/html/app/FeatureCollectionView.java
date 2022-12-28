/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.html.app;

import com.github.mustachejava.util.DecoratedCollection;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.common.domain.OgcApiDatasetView;
import de.ii.ogcapi.features.html.domain.FeaturesHtmlConfiguration.POSITION;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.html.domain.DatasetView;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.ImmutableSource;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.MapClient.Popup;
import de.ii.ogcapi.html.domain.MapClient.Source.TYPE;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */

// Todo Look following classes: FeaturesFormatHtml
@Value.Immutable
@Value.Style(builder = "new")
@Modifiable
public abstract class FeatureCollectionView extends OgcApiDatasetView {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCollectionView.class);

  FeatureCollectionView(String templateName) {
    super(templateName);
  }

  FeatureCollectionView() {
    super("featureCollection.mustache");
  }

  public abstract Optional<FeatureTypeConfigurationOgcApi> collectionData();

  public abstract String name();

  @Nullable
  public abstract String attribution();

  public abstract List<NavigationDTO> formats();

  public abstract Optional<Boolean> bare();

  public abstract I18n i18n();

  public abstract Optional<Locale> language();

  public abstract Type mapClientType();

  @Nullable
  public abstract String styleUrl();

  public abstract boolean removeZoomLevelConstraints();

  public abstract Map<String, String> queryables();

  public abstract List<String> geometryProperties();

  @Nullable
  public abstract URI uri();

  @Nullable
  public abstract List<NavigationDTO> pagination();

  public abstract List<NavigationDTO> metaPagination();

  public abstract POSITION mapPosition();

  public abstract Set<Map.Entry<String, String>> filterFields();

  public abstract Optional<BoundingBox> spatialExtent();

  public abstract URICustomizer uriBuilderWithFOnly();

  @Nullable
  public abstract Object data();

  @Nullable
  public abstract String url();

  @Nullable
  public abstract String version();

  @Nullable
  public abstract String license();

  @Nullable
  public abstract String metadataUrl();

  @Value.Default
  public List<String> keywords() {
    return new ArrayList<>();
  }

  @Value.Default
  public List<DatasetView> featureTypes() {
    return new ArrayList<>();
  }

  public abstract Optional<String> PersistentUri();

  @Nullable
  public abstract Boolean spatialSearch();

  @Value.Default
  public List<FeatureHtml> features() {
    return new ArrayList<>();
  }

  @Value.Default
  public boolean hideMap() {
    return false;
  }

  @Nullable
  @Value.Derived
  public Map<String, String> bbox() {
    return spatialExtent()
        .map(
            boundingBox ->
                ImmutableMap.of(
                    "minLng", Double.toString(boundingBox.getXmin()),
                    "minLat", Double.toString(boundingBox.getYmin()),
                    "maxLng", Double.toString(boundingBox.getXmax()),
                    "maxLat", Double.toString(boundingBox.getYmax())))
        .orElse(null);
  }

  @Value.Derived
  public URICustomizer uriBuilder() {
    return new URICustomizer(uri());
  }

  @Value.Derived
  public boolean isCollection() {
    return !"featureDetails".equals(getTemplateName());
  }

  @Value.Derived
  public boolean schemaOrgFeatures() {
    return (Objects.nonNull(htmlConfig())
        && Objects.equals(htmlConfig().getSchemaOrgEnabled(), true));
  }

  @Value.Derived
  public MapClient mapClient() {
    if (mapClientType().equals(MapClient.Type.MAP_LIBRE)) {

      return new ImmutableMapClient.Builder()
          .backgroundUrl(
              Optional.ofNullable(htmlConfig().getLeafletUrl())
                  .or(() -> Optional.ofNullable(htmlConfig().getBasemapUrl())))
          .attribution(getAttribution())
          .bounds(Optional.ofNullable(bbox()))
          .data(
              new ImmutableSource.Builder()
                  .type(TYPE.geojson)
                  .url(uriBuilder().removeParameters("f").ensureParameter("f", "json").toString())
                  .build())
          .popup(Popup.HOVER_ID)
          .styleUrl(Optional.ofNullable(styleUrl()))
          .removeZoomLevelConstraints(removeZoomLevelConstraints())
          .useBounds(true)
          .build();
    } else if (mapClientType().equals(MapClient.Type.CESIUM)) {
      return new ImmutableMapClient.Builder()
          .type(mapClientType())
          .backgroundUrl(
              Optional.ofNullable(htmlConfig().getLeafletUrl())
                  .or(() -> Optional.ofNullable(htmlConfig().getBasemapUrl()))
                  .map(
                      url ->
                          url.replace("{z}", "{TileMatrix}")
                              .replace("{y}", "{TileRow}")
                              .replace("{x}", "{TileCol}")))
          .attribution(getAttribution())
          .build();
    } else {
      LOGGER.error(
          "Configuration error: {} is not a supported map client for the HTML representation of features.",
          mapClientType());
      return null;
    }
  }

  public FilterEditor filterEditor() {
    if (Objects.nonNull(queryables())) {
      return new ImmutableFilterEditor.Builder()
          .backgroundUrl(
              Optional.ofNullable(htmlConfig().getLeafletUrl())
                  .or(() -> Optional.ofNullable(htmlConfig().getBasemapUrl())))
          .attribution(htmlConfig().getBasemapAttribution())
          .fields(queryables().entrySet())
          .build();
    } else {
      return null;
    }
  }

  @Value.Derived
  public CesiumData cesiumData() {
    return new CesiumData(features(), geometryProperties());
  }

  /**
   * public FeatureCollectionView( OgcApiDataV2 apiData, FeatureTypeConfigurationOgcApi
   * collectionData, Optional<BoundingBox> spatialExtent, String template, URI uri, String name,
   * String title, String description, String attribution, String urlPrefix, HtmlConfiguration
   * htmlConfig, String persistentUri, boolean noIndex, I18n i18n, Locale language, POSITION
   * mapPosition, Type mapClientType, String styleUrl, boolean removeZoomLevelConstraints,
   * Map<String, String> queryables, List<String> geometryProperties) { super(template, uri, name,
   * title, description, attribution, urlPrefix, htmlConfig, noIndex); this.features = new
   * ArrayList<>(); this.isCollection = !"featureDetails".equals(template); this.uri = uri; // TODO
   * need to overload getPath() as it currently forces trailing slashes while OGC // API uses no
   * trailing slashes this.persistentUri = persistentUri; this.schemaOrgFeatures =
   * Objects.nonNull(htmlConfig) && Objects.equals(htmlConfig.getSchemaOrgEnabled(), true);
   * this.mapPosition = mapPosition; this.uriBuilder = new URICustomizer(uri); this.cesiumData = new
   * CesiumData(features, geometryProperties);
   *
   * <p>this.bbox = spatialExtent .map( boundingBox -> ImmutableMap.of( "minLng",
   * Double.toString(boundingBox.getXmin()), "minLat", Double.toString(boundingBox.getYmin()),
   * "maxLng", Double.toString(boundingBox.getXmax()), "maxLat",
   * Double.toString(boundingBox.getYmax()))) .orElse(null);
   *
   * <p>if (mapClientType.equals(MapClient.Type.MAP_LIBRE)) { this.mapClient = new
   * ImmutableMapClient.Builder() .backgroundUrl( Optional.ofNullable(htmlConfig.getLeafletUrl())
   * .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl()))) .attribution(getAttribution())
   * .bounds(Optional.ofNullable(bbox)) .data( new ImmutableSource.Builder() .type(TYPE.geojson)
   * .url(uriBuilder.removeParameters("f").ensureParameter("f", "json").toString()) .build())
   * .popup(Popup.HOVER_ID) .styleUrl(Optional.ofNullable(styleUrl))
   * .removeZoomLevelConstraints(removeZoomLevelConstraints) .build(); } else if
   * (mapClientType.equals(MapClient.Type.CESIUM)) { this.mapClient = new
   * ImmutableMapClient.Builder() .type(mapClientType) .backgroundUrl(
   * Optional.ofNullable(htmlConfig.getLeafletUrl()) .or(() ->
   * Optional.ofNullable(htmlConfig.getBasemapUrl())) .map( url -> url.replace("{z}",
   * "{TileMatrix}") .replace("{y}", "{TileRow}") .replace("{x}", "{TileCol}")))
   * .attribution(getAttribution()) .build(); } else { LOGGER.error( "Configuration error: {} is not
   * a supported map client for the HTML representation of features.", mapClientType);
   * this.mapClient = null; }
   *
   * <p>if (Objects.nonNull(queryables)) { this.filterEditor = new ImmutableFilterEditor.Builder()
   * .backgroundUrl( Optional.ofNullable(htmlConfig.getLeafletUrl()) .or(() ->
   * Optional.ofNullable(htmlConfig.getBasemapUrl())))
   * .attribution(htmlConfig.getBasemapAttribution()) .fields(queryables.entrySet()) .build(); }
   * else { this.filterEditor = null; } }
   */
  public String getPath() {
    String path = uri().getPath();
    return path;
  }

  public boolean isMapTop() {
    return mapPosition() == POSITION.TOP
        || (mapPosition() == POSITION.AUTO
            && (features().isEmpty() || features().stream().anyMatch(FeatureHtml::hasObjects)));
  }

  public boolean isMapRight() {
    return mapPosition() == POSITION.RIGHT
        || (mapPosition() == POSITION.AUTO
            && !features().isEmpty()
            && features().stream().noneMatch(FeatureHtml::hasObjects));
  }

  @Override
  public List<NavigationDTO> getFormats() {
    return formats();
  }

  @Override
  public String getAttribution() {
    String basemapAttribution = super.getAttribution();
    if (Objects.nonNull(attribution())) {
      if (Objects.nonNull(basemapAttribution))
        return String.join(" | ", attribution(), basemapAttribution);
      else return attribution();
    }
    return basemapAttribution;
  }

  @Override
  public Optional<String> getCanonicalUrl() throws URISyntaxException {
    if (!isCollection() && PersistentUri() != null) return PersistentUri();

    URICustomizer canonicalUri = uriBuilder().copy().ensureNoTrailingSlash().clearParameters();

    boolean hasOtherParams = !canonicalUri.isQueryEmpty();
    boolean hasPrevLink =
        Objects.nonNull(metaPagination())
            && metaPagination().stream()
                .anyMatch(navigationDTO -> "prev".equals(navigationDTO.label));

    return !hasOtherParams && (!isCollection() || !hasPrevLink)
        ? Optional.of(canonicalUri.toString())
        : Optional.empty();
  }

  public String getQueryWithoutPage() {
    List<NameValuePair> query =
        URLEncodedUtils.parse(Query().substring(1), Charset.forName("utf-8")).stream()
            .filter(kvp -> !kvp.getName().equals("offset") && !kvp.getName().equals("limit"))
            .collect(Collectors.toList());

    return '?' + URLEncodedUtils.format(query, '&', Charset.forName("utf-8")) + '&';
  }

  public Function<String, String> getCurrentUrlWithSegment() {
    return segment ->
        uriBuilderWithFOnly()
            .copy()
            .ensureLastPathSegment(segment)
            .ensureNoTrailingSlash()
            .toString();
  }

  public DecoratedCollection<String> getKeywordsDecorated() {
    return new DecoratedCollection<>(keywords());
  }

  public Function<String, String> getQueryWithout() {
    return without -> {
      List<String> ignore = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(without);

      List<NameValuePair> query =
          URLEncodedUtils.parse(RawQuery().substring(1), Consts.ISO_8859_1).stream()
              .filter(kvp -> !ignore.contains(kvp.getName().toLowerCase()))
              .collect(Collectors.toList());

      return '?' + URLEncodedUtils.format(query, '&', Consts.UTF_8) + (!query.isEmpty() ? '&' : "");
    };
  }

  public Function<String, String> getCurrentUrlWithSegmentClearParams() {
    return segment ->
        uriBuilder()
            .copy()
            .ensureLastPathSegment(segment)
            .ensureNoTrailingSlash()
            .clearParameters()
            .toString();
  }

  public String Query() {

    return "?" + (uri().getQuery() != null ? uri().getQuery() + "&" : "");
  }

  public String RawQuery() {

    return "?" + (uri().getRawQuery() != null ? uri().getRawQuery() + "&" : "");
  }

  public String getQuery() {

    return "?" + (uri().getQuery() != null ? uri().getQuery() + "&" : "");
  }

  public String getRawQuery() {

    return "?" + (uri().getRawQuery() != null ? uri().getRawQuery() + "&" : "");
  }

  public Object getData() {
    return data();
  }
}
