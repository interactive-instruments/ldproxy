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
import de.ii.ldproxy.ogcapi.domain.CollectionExtent;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.PageRepresentation;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.ldproxy.ogcapi.tiles.domain.TilePoint;
import de.ii.ldproxy.ogcapi.tiles.domain.TileSets;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrix;
import de.ii.ldproxy.ogcapi.tiles.domain.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;

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
    public boolean isVector;
    public Map<String, String> bbox;
    public Map<String, String> interval;

    public TileSetsView(OgcApiDataV2 apiData,
                        Optional<String> collectionId,
                        BoundingBox spatialExtent,
                        TemporalExtent temporalExtent,
                        TileSets tiles,
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

        if (Objects.isNull(spatialExtent))
            spatialExtent = apiData.getDefaultExtent()
                                   .flatMap(CollectionExtent::getSpatial)
                                   .orElse(null);
        this.bbox = Objects.isNull(spatialExtent) ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent.getXmin()),
                "minLat", Double.toString(spatialExtent.getYmin()),
                "maxLng", Double.toString(spatialExtent.getXmax()),
                "maxLat", Double.toString(spatialExtent.getYmax()));
        BoundingBox finalSpatialExtent = spatialExtent;
        this.tileCollections = Objects.nonNull(spatialExtent) ? tiles.getTilesets()
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
                                      .orElse(getDefaultLevel(diff, tileMatrixSet.getMaxLevel()));
                    String lon = tms.getCenterPoint().isPresent() && tms.getCenterPoint().get().getCoordinates().size() >= 2
                            ? Double.toString(tms.getCenterPoint().get().getCoordinates().get(0))
                            : Double.toString(finalSpatialExtent.getXmax() * 0.5 + finalSpatialExtent.getXmin() * 0.5);
                    String lat = tms.getCenterPoint().isPresent() && tms.getCenterPoint().get().getCoordinates().size() >= 2
                            ? Double.toString(tms.getCenterPoint().get().getCoordinates().get(1))
                            : Double.toString(finalSpatialExtent.getYmax() * 0.5 + finalSpatialExtent.getYmin() * 0.5);

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

        List<Link> tileTemplates = tiles.getTilesets()
                                        .stream()
                                        .map(PageRepresentation::getLinks)
                                        .flatMap(Collection::stream)
                                        .filter(link -> Objects.equals(link.getRel(), "item"))
                                        .collect(Collectors.toUnmodifiableList());

        Optional<String> tileTemplate = tileTemplates.stream()
                                                     .filter(link -> Objects.equals(link.getType(), "application/vnd.mapbox-vector-tile"))
                                                     .map(Link::getHref)
                                                     .map(href -> href.replaceAll("/\\w+/\\{tileMatrix}/\\{tileRow}/\\{tileCol}", "/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
                                                     .findAny();
        if (tileTemplate.isPresent()) {
            this.tilesUrl = tileTemplate.get();
            this.isVector = true;
        } else {
            this.tilesUrl = tileTemplates.stream()
                                        .filter(link -> link.getType().startsWith("image/"))
                                        .map(Link::getHref)
                                        .map(href -> href.replaceAll("/\\w+/\\{tileMatrix}/\\{tileRow}/\\{tileCol}", "/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
                                        .findAny()
                                         .orElse(null);
            this.isVector = false;
        }

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

        Long[] interval = Objects.isNull(temporalExtent) ? null : new Long[]{temporalExtent.getStart(), temporalExtent.getEnd()};
        if (interval==null)
            this.interval = null;
        else if (interval[0]==Long.MIN_VALUE && interval[1]==Long.MAX_VALUE)
            this.interval = ImmutableMap.of();
        else if (interval[0]==Long.MIN_VALUE)
            this.interval = ImmutableMap.of(
                    "end", interval[1].toString());
        else if (interval[1]==Long.MAX_VALUE)
            this.interval = ImmutableMap.of(
                    "start", interval[0].toString());
        else
            this.interval = ImmutableMap.of(
                    "start", interval[0].toString(),
                    "end", interval[1].toString());
    }

    private String getDefaultLevel(double lonDiff, int maxLevel) {
        int defaultLevel = 0;
        double dx = 360;
        while (dx>lonDiff && defaultLevel<=maxLevel) {
            dx = dx/2.0;
            defaultLevel++;
        }
        return String.valueOf(defaultLevel);
    }
}
