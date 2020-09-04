/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.tiles;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.ldproxy.ogcapi.tiles.tileMatrixSet.TileMatrixSet;
import de.ii.xtraplatform.crs.domain.BoundingBox;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TileSetsView extends OgcApiView {
    public List<Map<String,String>> tileCollections;
    public String tilesUrl;
    public Link tileJsonLink;
    public String mapTitle;
    public String metadataTitle;
    public String tileMatrixSetTitle;
    public String none;
    public boolean withOlMap;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    private Map<String, String> center;
    public Map<String, String> temporalExtent;

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
        this.bbox2 = spatialExtent.map(boundingBox -> ImmutableMap.of(
                "minLng", Double.toString(boundingBox.getXmin()),
                "minLat", Double.toString(boundingBox.getYmin()),
                "maxLng", Double.toString(boundingBox.getXmax()),
                "maxLat", Double.toString(boundingBox.getYmax())))
                                  .orElse(null);
        this.center = tiles.getDefaultCenter().isPresent() && tiles.getDefaultCenter().get().length>=2 ? ImmutableMap.of(
                "lon", Double.toString(tiles.getDefaultCenter().get()[0]),
                "lat", Double.toString(tiles.getDefaultCenter().get()[1])) : spatialExtent.isPresent() ? ImmutableMap.of(
                "lon", Double.toString(spatialExtent.get().getXmax()*0.5+spatialExtent.get().getXmin()*0.5),
                "lat", Double.toString(spatialExtent.get().getYmax()*0.5+spatialExtent.get().getYmin()*0.5)) : ImmutableMap.of();
        this.tileCollections = spatialExtent.isPresent() ? tiles.getTileMatrixSetLinks()
                .stream()
                .filter(tms -> tms.getTileMatrixSet().isPresent())
                .map(tms -> {
                    String tmsId = tms.getTileMatrixSet().get();
                    TileMatrixSet tileMatrixSet = tileMatrixSets.get(tmsId);
                    if (tileMatrixSet==null)
                        return null;
                    BoundingBox bbox = tileMatrixSet.getBoundingBox();
                    String extent = "[" + bbox.getXmin() + "," + bbox.getYmin() + "," + bbox.getXmax() + "," + bbox.getYmax() + "]";
                    long widthAtL0 = tileMatrixSet.getTileMatrix(0).getMatrixWidth();
                    return new ImmutableMap.Builder<String,String>()
                            .put("tileMatrixSet",tmsId)
                            .put("maxLevel",tms.getTileMatrixSetLimits()
                                    .stream()
                                    .map(tmsl -> Integer.parseInt(tmsl.getTileMatrix()))
                                    .max(Comparator.naturalOrder())
                                    .orElse(-1)
                                    .toString())
                            .put("extent",extent)
                            // TODO: The +1 for CRS84 is necessary as OpenLayers seems to change the zoom levels by 1 for this tile grid
                            // TODO: we should have a better fallback than simply "10"
                            .put("defaultZoomLevel",Integer.toString(tms.getDefaultZoomLevel().orElse(10) + (tmsId.equals("WorldCRS84Quad")?1:0)))
                            .put("defaultCenterLon",this.center.get("lon"))
                            .put("defaultCenterLat",this.center.get("lat"))
                            .put("resolutionAt0",Double.toString((bbox.getXmax()- bbox.getXmin())/(widthAtL0*tileMatrixSet.getTileSize())))
                            .put("widthAtL0",Long.toString(widthAtL0))
                            .put("projection","EPSG:"+tileMatrixSet.getCrs().getCode())
                            .build();
                })
                .collect(Collectors.toList()) : ImmutableList.of();

        this.tilesUrl = links.stream()
                .filter(link -> Objects.equals(link.getRel(),"item"))
                .filter(link -> Objects.equals(link.getType(), "application/vnd.mapbox-vector-tile"))
                .map(link -> link.getHref())
                .findFirst()
                .orElse(null);

        this.tileJsonLink = links.stream()
                .filter(link -> Objects.equals(link.getRel(),"describedby"))
                .filter(link -> Objects.equals(link.getType(), "application/json"))
                .findFirst()
                .orElse(null);

        this.mapTitle = i18n.get("mapTitle", language);
        this.metadataTitle = i18n.get("metadataTitle", language);
        this.tileMatrixSetTitle = i18n.get("tileMatrixSetTitle", language);
        this.none = i18n.get ("none", language);

        this.withOlMap = true;
        this.spatialSearch = false;

        Long[] interval = apiData.getCollections()
                .values()
                .stream()
                .filter(featureTypeConfiguration -> !collectionId.isPresent() || Objects.equals(featureTypeConfiguration.getId(),collectionId.get()))
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
