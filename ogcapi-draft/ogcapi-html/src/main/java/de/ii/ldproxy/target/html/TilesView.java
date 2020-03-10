/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.application.I18n;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.tiles.TileCollections;
import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TilesView extends LdproxyView {
    public List<Map<String,String>> tileCollections;
    public String tilesUrl;
    public String mapTitle;
    public String none;
    public boolean withOlMap;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    public Map<String, String> temporalExtent;

    public TilesView(OgcApiApiDataV2 apiData,
                     TileCollections tiles,
                     Optional<String> collectionId,
                     List<NavigationDTO> breadCrumbs,
                     String urlPrefix,
                     HtmlConfig htmlConfig,
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

        this.tileCollections = tiles.getTileMatrixSetLinks()
                .stream()
                .filter(tms -> tms.getTileMatrixSet().isPresent())
                .map(tms -> new ImmutableMap.Builder<String,String>()
                        .put("tileMatrixSet",tms.getTileMatrixSet().get())
                        .put("maxLevel",tms.getTileMatrixSetLimits()
                                .stream()
                                .map(tmsl -> Integer.parseInt(tmsl.getTileMatrix()))
                                .max(Comparator.naturalOrder())
                                .orElse(-1)
                                .toString())
                        .put("extent",tms.getTileMatrixSet().get().equals("WorldCRS84Quad") ? "[-180,-90,180,90]" : "[-20037508.3427892,-20037508.3427892,20037508.3427892,20037508.3427892]")
                        .put("resolutionAt0",Double.toString(tms.getTileMatrixSet().get().equals("WorldCRS84Quad") ? 360.0/512 : 2*20037508.3427892/256))
                        .put("widthAtL0",tms.getTileMatrixSet().get().equals("WorldCRS84Quad") ? "2" : "1")
                        .put("projection",tms.getTileMatrixSet().get().equals("WorldCRS84Quad") ? "EPSG:4326" : tms.getTileMatrixSet().get().equals("WorldMercatorWGS84Quad") ? "EPSG:3395" : "EPSG:3857")
                        .build())
                .collect(Collectors.toList());

        this.tilesUrl = links.stream()
                .filter(link -> Objects.equals(link.getRel(),"item"))
                .filter(link -> Objects.equals(link.getType(), "application/vnd.mapbox-vector-tile"))
                .map(link -> link.getHref())
                .findFirst()
                .orElse(null);

        this.mapTitle = i18n.get("mapTitle", language);
        this.none = i18n.get ("none", language);

        this.withOlMap = true;
        this.spatialSearch = false;
        BoundingBox spatialExtent = apiData.getSpatialExtent();
        this.bbox2 = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent.getXmin()),
                "minLat", Double.toString(spatialExtent.getYmin()),
                "maxLng", Double.toString(spatialExtent.getXmax()),
                "maxLat", Double.toString(spatialExtent.getYmax()));
        Long[] interval = apiData.getCollections()
                .values()
                .stream()
                .filter(featureTypeConfiguration -> !collectionId.isPresent() || Objects.equals(featureTypeConfiguration.getId(),collectionId.get()))
                .map(FeatureTypeConfigurationOgcApi::getExtent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(FeatureTypeConfigurationOgcApi.CollectionExtent::getTemporal)
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
            this.temporalExtent = interval==null ? null : ImmutableMap.of(
                    "end", interval[1]==null ? null : interval[1].toString());
        else if (interval[1]==null)
            this.temporalExtent = interval==null ? null : ImmutableMap.of(
                    "start", interval[0]==null ? null : interval[0].toString());
        else
            this.temporalExtent = interval==null ? null : ImmutableMap.of(
                    "start", interval[0]==null ? null : interval[0].toString(),
                    "end", interval[1]==null ? null : interval[1].toString());
    }
}
