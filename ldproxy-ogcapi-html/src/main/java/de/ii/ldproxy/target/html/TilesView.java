/**
 * Copyright 2019 interactive instruments GmbH
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
import de.ii.ldproxy.ogcapi.domain.OgcApiLink;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.wfs3.vt.TileCollection;
import de.ii.ldproxy.wfs3.vt.TileCollections;
import io.dropwizard.views.View;

import java.util.*;
import java.util.stream.Collectors;

public class TilesView extends View {
    private final OgcApiDatasetData apiData;
    private final List<NavigationDTO> breadCrumbs;
    public List<NavigationDTO> formats;
    public List<TileCollection> tileCollections;
    public final HtmlConfig htmlConfig;
    public String tilesUrl;
    public List<OgcApiLink> links;
    public String urlPrefix;
    public String title;
    public String description;
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
                     String staticUrlPrefix,
                     HtmlConfig htmlConfig,
                     URICustomizer uriCustomizer,
                     I18n i18n,
                     Optional<Locale> language) {
        super("tiles.mustache", Charsets.UTF_8);

        // TODO this is quick and dirty - the view needs to be improved

        this.tileCollections = tiles.getTileMatrixSetLinks();
        this.links = tiles.getLinks();
        this.tilesUrl = links.stream()
                .filter(link -> Objects.equals(link.getRel(),"tiles"))
                .filter(link -> Objects.equals(link.getType(), "application/vnd.mapbox-vector-tile"))
                .map(link -> link.getHref())
                .findFirst()
                .orElse(null);
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = staticUrlPrefix;
        this.htmlConfig = htmlConfig;

        this.title = tiles
                .getTitle()
                .orElse(apiData.getId());
        this.description = tiles
                .getDescription()
                .orElse("");
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
                .map(temporalExtent -> new Long[]{temporalExtent.getStart(), temporalExtent.getComputedEnd()})
                .reduce((longs, longs2) -> new Long[]{
                        Math.min(longs[0], longs2[0]),
                        Math.max(longs[1], longs2[1])})
                .orElse(null);
        this.temporalExtent = interval==null ? null : ImmutableMap.of(
                "start", interval[0].toString(),
                "computedEnd", interval[1].toString());
        this.formats = links.stream()
                    .filter(link -> Objects.equals(link.getRel(), "alternate"))
                    .sorted(Comparator.comparing(link -> link.getTypeLabel()
                            .toUpperCase()))
                    .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                    .collect(Collectors.toList());

        this.apiData = apiData;
    }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        return links.stream()
                .filter(link -> Objects.equals(link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                .collect(Collectors.toList());
    }
}
