/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.domain;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import de.ii.ogcapi.foundation.domain.CollectionExtent;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcResourceMetadata;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.ImmutableSource;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.MapClient.Popup;
import de.ii.ogcapi.html.domain.MapClient.Source.TYPE;
import de.ii.ogcapi.html.domain.MapClient.Type;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrix;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tiles.domain.TileLayer.GeometryType;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO class needs to be in 'domain', since it is also accessed from MAP_TILES; find better
// solution
public class TileSetsView extends OgcApiView {
  private static final Logger LOGGER = LoggerFactory.getLogger(TileSetsView.class);

  public List<Set<Map.Entry<String, String>>> tileCollections;
  public List<String> tileMatrixSetIds;
  public String tilesUrl;
  public String tileJsonLink;
  public String tileJsonTitle;
  public String mapTitle;
  public String metadataTitle;
  public String tileMatrixSetTitle;
  public String tilesetDescription;
  public String templateTitle;
  public String howToUse;
  public String none;
  public boolean withOlMap;
  public boolean spatialSearch;
  public boolean isVector;
  public Map<String, String> bbox;
  public final MapClient mapClient;
  public final String xyzTemplate;

  public TileSetsView(
      OgcApiDataV2 apiData,
      TileSets tiles,
      Map<String, TileMatrixSet> tileMatrixSets,
      List<NavigationDTO> breadCrumbs,
      String urlPrefix,
      MapClient.Type mapClientType,
      String styleUrl,
      boolean removeZoomLevelConstraints,
      HtmlConfiguration htmlConfig,
      boolean noIndex,
      I18n i18n,
      Optional<Locale> language) {
    super(
        "tiles.mustache",
        Charsets.UTF_8,
        apiData,
        breadCrumbs,
        htmlConfig,
        noIndex,
        urlPrefix,
        tiles.getLinks(),
        tiles.getTitle().orElse(apiData.getId()),
        tiles.getDescription().orElse(""));

    // the center of all tilesets is the same, the only difference is the tiling scheme
    final Optional<TilePoint> center =
        tiles.getTilesets().stream().findAny().flatMap(TileSet::getCenterPoint);

    Optional<BoundingBox> spatialExtent =
        tiles.getTilesets().stream()
            .map(TileSet::getBoundingBox)
            // the bbox of all tilesets is the same, the only difference is the tiling scheme
            .findAny()
            .map(
                tBbox ->
                    BoundingBox.of(
                        tBbox.getLowerLeft()[0].doubleValue(),
                        tBbox.getLowerLeft()[1].doubleValue(),
                        tBbox.getUpperRight()[0].doubleValue(),
                        tBbox.getUpperRight()[1].doubleValue(),
                        OgcCrs.CRS84))
            .or(() -> apiData.getDefaultExtent().flatMap(CollectionExtent::getSpatial));
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
    this.tileMatrixSetIds =
        tiles.getTilesets().stream()
            .map(TileSet::getTileMatrixSetId)
            .filter(
                tileMatrixSetId ->
                    mapClientType.equals(MapClient.Type.OPEN_LAYERS)
                        || tileMatrixSetId.equals("WebMercatorQuad"))
            .collect(Collectors.toList());
    this.tileCollections =
        tiles.getTilesets().stream()
            .filter(
                ts ->
                    mapClientType.equals(MapClient.Type.OPEN_LAYERS)
                        || ts.getTileMatrixSetId().equals("WebMercatorQuad"))
            .map(
                ts -> {
                  String tmsId = ts.getTileMatrixSetId();
                  TileMatrixSet tileMatrixSet = tileMatrixSets.get(tmsId);
                  if (tileMatrixSet == null) return null;
                  BoundingBox bbox = tileMatrixSet.getBoundingBox();
                  String extent =
                      "["
                          + bbox.getXmin()
                          + ","
                          + bbox.getYmin()
                          + ","
                          + bbox.getXmax()
                          + ","
                          + bbox.getYmax()
                          + "]";
                  int maxLevel = tileMatrixSet.getMaxLevel();
                  List<TileMatrix> tileMatrixList = tileMatrixSet.getTileMatrices(0, maxLevel);
                  String sizes =
                      String.format(
                          "[%s]",
                          tileMatrixList.stream()
                              .map(
                                  tileMatrix ->
                                      String.format(
                                          "[%d, %d]",
                                          tileMatrix.getMatrixWidth(),
                                          tileMatrix.getMatrixHeight()))
                              .collect(Collectors.joining(", ")));
                  double diff = bbox.getXmax() - bbox.getXmin();
                  String resolutions =
                      String.format(
                          "[%s]",
                          tileMatrixList.stream()
                              .map(
                                  tileMatrix ->
                                      String.valueOf(
                                          diff
                                              / (tileMatrix.getMatrixWidth()
                                                  * tileMatrixSet.getTileSize())))
                              .collect(Collectors.joining(", ")));

                  String level =
                      ts.getCenterPoint()
                          .flatMap(TilePoint::getTileMatrix)
                          .orElse(getDefaultLevel(diff, tileMatrixSet.getMaxLevel()));
                  String lon =
                      ts.getCenterPoint().isPresent()
                              && ts.getCenterPoint().get().getCoordinates().size() >= 2
                          ? Double.toString(ts.getCenterPoint().get().getCoordinates().get(0))
                          : spatialExtent.isPresent()
                              ? Double.toString(
                                  spatialExtent.get().getXmax() * 0.5
                                      + spatialExtent.get().getXmin() * 0.5)
                              : "0.0";
                  String lat =
                      ts.getCenterPoint().isPresent()
                              && ts.getCenterPoint().get().getCoordinates().size() >= 2
                          ? Double.toString(ts.getCenterPoint().get().getCoordinates().get(1))
                          : spatialExtent.isPresent()
                              ? Double.toString(
                                  spatialExtent.get().getYmax() * 0.5
                                      + spatialExtent.get().getYmin() * 0.5)
                              : "0.0";

                  return new ImmutableMap.Builder<String, String>()
                      .put("tileMatrixSet", tmsId)
                      .put("maxLevel", String.valueOf(maxLevel))
                      .put("extent", extent)
                      .put("defaultZoomLevel", level)
                      .put("defaultCenterLon", lon)
                      .put("defaultCenterLat", lat)
                      .put("resolutions", resolutions)
                      .put("sizes", sizes)
                      .put("projection", "EPSG:" + tileMatrixSet.getCrs().getCode())
                      .build()
                      .entrySet();
                })
            .collect(Collectors.toList());

    List<Link> tileTemplates =
        tiles.getTilesets().stream()
            .map(OgcResourceMetadata::getLinks)
            .flatMap(Collection::stream)
            .filter(link -> Objects.equals(link.getRel(), "item"))
            .collect(Collectors.toUnmodifiableList());

    Optional<String> tileTemplate =
        tileTemplates.stream()
            .filter(link -> Objects.equals(link.getType(), "application/vnd.mapbox-vector-tile"))
            .map(Link::getHref)
            .map(
                href ->
                    href.replaceAll(
                        "/\\w+/\\{tileMatrix}/\\{tileRow}/\\{tileCol}",
                        "/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
            .findFirst();
    if (tileTemplate.isPresent()) {
      this.tilesUrl = tileTemplate.get();
      this.isVector = true;
    } else {
      this.tilesUrl =
          tileTemplates.stream()
              .filter(link -> link.getType().startsWith("image/"))
              .map(Link::getHref)
              .map(
                  href ->
                      href.replaceAll(
                          "/\\w+/\\{tileMatrix}/\\{tileRow}/\\{tileCol}",
                          "/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
              .findFirst()
              .orElse(null);
      this.isVector = false;
    }

    this.tileJsonLink =
        tiles.getTilesets().stream()
            .map(OgcResourceMetadata::getLinks)
            .flatMap(Collection::stream)
            .filter(link -> Objects.equals(link.getRel(), "self"))
            .map(Link::getHref)
            .findFirst()
            .map(href -> href.replaceAll("/\\w+$", "/WebMercatorQuad") + "?f=tilejson")
            .orElse(null);

    this.mapTitle = i18n.get("mapTitle", language);
    this.templateTitle = i18n.get("templateTitle", language);
    this.howToUse = i18n.get("howToUseTitle", language);
    this.metadataTitle = i18n.get("metadataTitle", language);
    this.tileMatrixSetTitle = i18n.get("tileMatrixSetTitle", language);
    this.tilesetDescription = i18n.get("tilesetDescriptionTitle", language);
    this.tileJsonTitle = i18n.get("tileJsonTitle", language);
    this.none = i18n.get("none", language);

    this.withOlMap = true;
    this.spatialSearch = false;

    this.xyzTemplate =
        tilesUrl
            .replace("{tileMatrixSetId}", "WebMercatorQuad")
            .replace("{tileMatrix}", "{z}")
            .replace("{tileRow}", "{y}")
            .replace("{tileCol}", "{x}");

    if (tiles.getTilesets().size() >= 1) {
      if (mapClientType.equals(MapClient.Type.MAP_LIBRE)) {
        Optional<TileSet> tileSet =
            tiles.getTilesets().stream()
                .filter(ts -> ts.getTileMatrixSetId().equals("WebMercatorQuad"))
                .findAny();
        if (tileSet.isPresent()) {
          Multimap<String, List<String>> layers = getLayers(tileSet.get());

          this.mapClient =
              getMapClient(
                  Type.MAP_LIBRE, styleUrl, removeZoomLevelConstraints, center, htmlConfig, layers);
        } else {
          LOGGER.error(
              "Configuration error: {} as the client for the HTML representation of tile sets requires that a tile set with the tiling scheme {} exists.",
              mapClientType,
              "WebMercatorQuad");
          this.mapClient = null;
        }
      } else if (mapClientType.equals(MapClient.Type.OPEN_LAYERS)) {
        Multimap<String, List<String>> layers = getLayers(tiles.getTilesets().get(0));

        this.mapClient =
            getMapClient(
                Type.OPEN_LAYERS, styleUrl, removeZoomLevelConstraints, center, htmlConfig, layers);
      } else {
        LOGGER.error(
            "Configuration error: {} is not a supported map client for the HTML representation of tile sets.",
            mapClientType);
        this.mapClient = null;
      }
    } else {
      this.mapClient = null;
    }
  }

  private MapClient getMapClient(
      Type type,
      String styleUrl,
      boolean removeZoomLevelConstraints,
      Optional<TilePoint> center,
      HtmlConfiguration htmlConfig,
      Multimap<String, List<String>> layers) {
    return new ImmutableMapClient.Builder()
        .type(type)
        .backgroundUrl(
            Optional.ofNullable(htmlConfig.getLeafletUrl())
                .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
        .attribution(
            Optional.ofNullable(htmlConfig.getLeafletAttribution())
                .or(() -> Optional.ofNullable(htmlConfig.getBasemapAttribution())))
        .data(
            new ImmutableSource.Builder()
                .type(isVector ? TYPE.vector : TYPE.raster)
                .url(xyzTemplate)
                .putAllLayers(layers)
                .build())
        .bounds(Optional.ofNullable(bbox))
        .popup(Popup.CLICK_PROPERTIES)
        .styleUrl(Optional.ofNullable(styleUrl))
        .removeZoomLevelConstraints(removeZoomLevelConstraints)
        .center(center.map(TilePoint::getCoordinates).orElse(ImmutableList.of()))
        .zoom(center.flatMap(TilePoint::getTileMatrix).map(Double::parseDouble))
        .build();
  }

  private Multimap<String, List<String>> getLayers(TileSet tileSet) {
    return tileSet.getLayers().stream()
        .filter(layer -> layer.getDataType() == DataType.vector)
        .map(
            layer ->
                layer.getGeometryType().isPresent()
                    ? new SimpleImmutableEntry<>(
                        layer.getId(), ImmutableList.of(layer.getGeometryType().get().name()))
                    : new SimpleImmutableEntry<>(
                        layer.getId(),
                        ImmutableList.of(
                            GeometryType.points.name(),
                            GeometryType.lines.name(),
                            GeometryType.polygons.name())))
        .collect(
            ImmutableSetMultimap.toImmutableSetMultimap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private String getDefaultLevel(double lonDiff, int maxLevel) {
    int defaultLevel = 0;
    double dx = 360;
    while (dx > lonDiff && defaultLevel <= maxLevel) {
      dx = dx / 2.0;
      defaultLevel++;
    }
    return String.valueOf(defaultLevel);
  }
}
