/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles.app.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableSource;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.ldproxy.ogcapi.html.domain.MapClient.Popup;
import de.ii.ldproxy.ogcapi.html.domain.MapClient.Source.TYPE;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.ldproxy.ogcapi.tiles.app.tileMatrixSet.WebMercatorQuad;
import de.ii.ldproxy.ogcapi.tiles.domain.TilePoint;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSet.DataType;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrix;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TileSetsView extends OgcApiView {
    public List<Map<String,String>> tileCollections;
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
    public Map<String, String> bbox;
    public Map<String, String> temporalExtent;
    public final MapClient mapClient;
    public final String xyzTemplate;

    public TileSetsView(OgcApiDataV2 apiData,
                        TileSets tiles,
                        Optional<String> collectionId,
                        Map<String, TileMatrixSet> tileMatrixSets,
                        List<NavigationDTO> breadCrumbs,
                        String urlPrefix,
                        HtmlConfiguration htmlConfig,
                        boolean noIndex,
                        URICustomizer uriCustomizer,
                        I18n i18n,
                        Optional<Locale> language) {
        super("tiles.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                tiles.getLinks(),
                tiles.getTitle()
                     .orElse(apiData.getId()),
                tiles.getDescription()
                     .orElse(""));

        Optional<BoundingBox> spatialExtent = apiData.getSpatialExtent();
        this.bbox = spatialExtent.map(boundingBox -> ImmutableMap.of(
                "minLng", Double.toString(boundingBox.getXmin()),
                "minLat", Double.toString(boundingBox.getYmin()),
                "maxLng", Double.toString(boundingBox.getXmax()),
                "maxLat", Double.toString(boundingBox.getYmax())))
                                  .orElse(null);
        this.tileCollections = spatialExtent.isPresent() ? tiles.getTilesets()
                .stream()
                .map(tms -> {
                    String tmsId = tms.getTileMatrixSetId();
                    TileMatrixSet tileMatrixSet = tileMatrixSets.get(tmsId);
                    if (tileMatrixSet==null)
                        return null;
                    BoundingBox bbox = tileMatrixSet.getBoundingBox();
                    String extent = "[" + bbox.getXmin() + "," + bbox.getYmin() + "," + bbox.getXmax() + "," + bbox.getYmax() + "]";
                    int maxLevel = tileMatrixSet.getMaxLevel();
                    List<TileMatrix> tileMatrixList = tileMatrixSet.getTileMatrices(0, maxLevel);
                    String sizes = String.format("[%s]", tileMatrixList.stream()
                                                                       .map(tileMatrix -> String.format("[%d, %d]", tileMatrix.getMatrixWidth(), tileMatrix.getMatrixHeight()))
                                                                       .collect(Collectors.joining(", ")));
                    double diff = bbox.getXmax() - bbox.getXmin();
                    String resolutions = String.format("[%s]", tileMatrixList.stream()
                                                                       .map(tileMatrix -> String.valueOf(diff/(tileMatrix.getMatrixWidth()*tileMatrixSet.getTileSize())))
                                                                       .collect(Collectors.joining(", ")));

                    String level = tms.getCenterPoint()
                                      .flatMap(TilePoint::getTileMatrix)
                                      .orElse("10");
                    String lon = tms.getCenterPoint().isPresent() && tms.getCenterPoint().get().getCoordinates().size() >= 2
                            ? Double.toString(tms.getCenterPoint().get().getCoordinates().get(0))
                            : Double.toString(spatialExtent.get().getXmax() * 0.5 + spatialExtent.get().getXmin() * 0.5);
                    String lat = tms.getCenterPoint().isPresent() && tms.getCenterPoint().get().getCoordinates().size() >= 2
                            ? Double.toString(tms.getCenterPoint().get().getCoordinates().get(1))
                            : Double.toString(spatialExtent.get().getYmax() * 0.5 + spatialExtent.get().getYmin() * 0.5);

                    return new ImmutableMap.Builder<String,String>()
                            .put("tileMatrixSet",tmsId)
                            .put("maxLevel",String.valueOf(maxLevel))
                            .put("extent",extent)
                            .put("defaultZoomLevel",level)
                            .put("defaultCenterLon",lon)
                            .put("defaultCenterLat",lat)
                            .put("resolutions",resolutions)
                            .put("sizes",sizes)
                            .put("projection","EPSG:"+tileMatrixSet.getCrs().getCode())
                            .build();
                })
                .collect(Collectors.toList()) : ImmutableList.of();

        this.tilesUrl = tiles.getTilesets()
                             .stream()
                             .map(PageRepresentation::getLinks)
                             .flatMap(Collection::stream)
                             .filter(link -> Objects.equals(link.getRel(),"item"))
                             .filter(link -> Objects.equals(link.getType(), "application/vnd.mapbox-vector-tile"))
                             .map(Link::getHref)
                             .findFirst()
                             .map(href -> href.replaceAll("/\\w+/\\{tileMatrix}/\\{tileRow}/\\{tileCol}", "/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
                             .orElse(null);

        this.tileJsonLink = tiles.getTilesets()
                                 .stream()
                                 .map(PageRepresentation::getLinks)
                                 .flatMap(Collection::stream)
                                 .filter(link -> Objects.equals(link.getRel(),"self"))
                                 .map(Link::getHref)
                                 .findFirst()
                                 .map(href -> href.replaceAll("/\\w+$", "/{tileMatrixSetId}"))
                                 .orElse(null);

        this.mapTitle = i18n.get("mapTitle", language);
        this.templateTitle = i18n.get("templateTitle", language);
        this.howToUse = i18n.get("howToUseTitle", language);
        this.metadataTitle = i18n.get("metadataTitle", language);
        this.tileMatrixSetTitle = i18n.get("tileMatrixSetTitle", language);
        this.tilesetDescription = i18n.get("tilesetDescriptionTitle", language);
        this.tileJsonTitle = i18n.get("tileJsonTitle", language);
        this.none = i18n.get ("none", language);

        this.withOlMap = true;
        this.spatialSearch = false;

        Long[] interval = apiData.getCollections()
                .values()
                .stream()
                .filter(featureTypeConfiguration -> collectionId.isEmpty() || Objects.equals(featureTypeConfiguration.getId(), collectionId.get()))
                .map(featureType -> apiData.getTemporalExtent(featureType.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getEnd()})
                .reduce((longs, longs2) -> new Long[]{
                        longs[0]==null || longs2[0]==null ? null : Math.min(longs[0], longs2[0]),
                        longs[1]==null || longs2[1]==null ? null : Math.max(longs[1], longs2[1])})
                .orElse(null);
        if (interval==null)
            this.temporalExtent = null;
        else if (interval[0]==null && interval[1]==null)
            this.temporalExtent = ImmutableMap.of();
        else if (interval[0]==null)
            this.temporalExtent = ImmutableMap.of(
                    "end", interval[1].toString());
        else if (interval[1]==null)
            this.temporalExtent = ImmutableMap.of(
                    "start", interval[0].toString());
        else
            this.temporalExtent = ImmutableMap.of(
                    "start", interval[0].toString(),
                    "end", interval[1].toString());

        this.xyzTemplate = tilesUrl.replace("{tileMatrixSetId}","WebMercatorQuad").replace("{tileMatrix}","{z}").replace("{tileRow}","{y}").replace("{tileCol}","{x}");

        if (tiles.getTilesets().size() == 1 && Objects.equals(tiles.getTilesets().get(0).getTileMatrixSetId(), WebMercatorQuad.ID)) {
          Multimap<String, String> layers = tiles.getTilesets().get(0).getLayers().stream()
              .filter(layer -> layer.getDataType() == DataType.vector && layer.getGeometryType().isPresent())
              .map(layer -> new SimpleImmutableEntry<>(layer.getId(), layer.getGeometryType().get().name()))
              .collect(ImmutableSetMultimap.toImmutableSetMultimap(Map.Entry::getKey, Map.Entry::getValue));

          this.mapClient = new ImmutableMapClient.Builder()
              .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                  .or(() -> Optional.ofNullable(htmlConfig.getMapBackgroundUrl())))
              .attribution(Optional.ofNullable(htmlConfig.getLeafletAttribution())
                  .or(() -> Optional.ofNullable(htmlConfig.getMapAttribution())))
              .data(new ImmutableSource.Builder()
                .type(TYPE.vector)
                .url(xyzTemplate)
                .putAllLayers(layers)
                .build())
              .bounds(bbox)
              .popup(Popup.CLICK_PROPERTIES)
              .build();
        } else if (tiles.getTilesets().size() > 1) {
          //TODO: OpenLayers
          this.mapClient = null;
        } else {
          this.mapClient = null;
        }
    }
}
