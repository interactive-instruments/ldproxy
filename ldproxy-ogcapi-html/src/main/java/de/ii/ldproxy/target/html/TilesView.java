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
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.wfs3.vt.TileCollection;
import de.ii.ldproxy.wfs3.vt.TileCollections;

import java.util.*;

public class TilesView extends LdproxyView {
    public List<TileCollection> tileCollections;
    public String tilesUrl;
    public String mapTitle;
    public String none;
    public boolean withOlMap;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    public Map<String, String> temporalExtent;

    public TilesView(OgcApiDatasetData apiData,
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

        // TODO this is quick and dirty - the view needs to be improved

        this.tileCollections = tiles.getTileMatrixSetLinks();
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
        double[] spatialExtent = apiData.getFeatureTypes()
                .values()
                .stream()
                .filter(featureTypeConfiguration -> !collectionId.isPresent() || Objects.equals(featureTypeConfiguration.getId(),collectionId.get()))
                .map(featureTypeConfiguration -> featureTypeConfiguration.getExtent()
                        .getSpatial()
                        .getCoords())
                .reduce((doubles, doubles2) -> new double[]{
                        Math.min(doubles[0], doubles2[0]),
                        Math.min(doubles[1], doubles2[1]),
                        Math.max(doubles[2], doubles2[2]),
                        Math.max(doubles[3], doubles2[3])})
                .orElse(null);
        this.bbox2 = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent[1]),
                "minLat", Double.toString(spatialExtent[0]),
                "maxLng", Double.toString(spatialExtent[3]),
                "maxLat", Double.toString(spatialExtent[2])); // TODO is axis order mixed up in script.mustache?
        Long[] interval = apiData.getFeatureTypes()
                .values()
                .stream()
                .filter(featureTypeConfiguration -> !collectionId.isPresent() || Objects.equals(featureTypeConfiguration.getId(),collectionId.get()))
                .map(featureTypeConfiguration -> featureTypeConfiguration.getExtent()
                        .getTemporal())
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
