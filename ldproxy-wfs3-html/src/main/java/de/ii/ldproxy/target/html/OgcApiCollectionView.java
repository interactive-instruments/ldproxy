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
import de.ii.ldproxy.ogcapi.domain.*;
import io.dropwizard.views.View;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class OgcApiCollectionView extends View {
    private final OgcApiCollection collection;
    private final List<NavigationDTO> breadCrumbs;
    private final String urlPrefix;
    private final OgcApiDatasetData datasetData;
    public final HtmlConfig htmlConfig;
    public String itemType;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    public Map<String, String> temporalExtent;
    public String title;
    public String description;
    public List<String> crs;
    public String storageCrs;
    public Metadata metadata;
    public List<OgcApiLink> links;
    public OgcApiLink items;

    public OgcApiCollectionView(OgcApiDatasetData datasetData, OgcApiCollection collection,
                                final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfig htmlConfig) {
        super("collection.mustache", Charsets.UTF_8);
        this.collection = collection;
        this.breadCrumbs = breadCrumbs;
        this.urlPrefix = urlPrefix;
        this.htmlConfig = htmlConfig;

        this.title = collection
                .getTitle()
                .orElse(collection.getId());
        this.description = collection
                .getDescription()
                .orElse("");
        this.links = collection.getLinks();
        this.items = collection
                .getLinks()
                .stream()
                .filter(link -> link.getRel().equalsIgnoreCase("items"))
                .findFirst()
                .orElse(null);
        this.crs = collection
                .getCrs();
        this.storageCrs = collection
                .getStorageCrs()
                .orElse("");
        OgcApiExtentSpatial spatialExtent = collection
                .getExtent()
                .getSpatial();
        if (Objects.nonNull(spatialExtent)) {
            double[] boundingBox = spatialExtent.getBbox()[0]; // TODO just the first bbox and it is assumed to be in CRS84
            this.bbox2 = ImmutableMap.of(
                    "minLng", Double.toString(boundingBox[1]),
                    "minLat", Double.toString(boundingBox[0]),
                    "maxLng", Double.toString(boundingBox[3]),
                    "maxLat", Double.toString(boundingBox[2])); // TODO is axis order mixed up in script.mustache?
        }
        OgcApiExtentTemporal temporalExtent = collection
                .getExtent()
                .getTemporal();
        if (Objects.nonNull(temporalExtent)) {
            String[] interval = temporalExtent.getInterval()[0]; // TODO just the first interval and it is assumed to be in Gregorian calendar
            this.temporalExtent = interval == null ? null : ImmutableMap.of(
                    "start", interval[0] == null ? String.valueOf(Instant.EPOCH.toEpochMilli()) : String.valueOf(Instant.parse(interval[0]).toEpochMilli()),
                    "computedEnd", interval[1] == null ? String.valueOf(Instant.now().toEpochMilli()) : String.valueOf(Instant.parse(interval[1]).toEpochMilli()));
        }
        this.spatialSearch = false;
        this.itemType = collection.getItemType()
                .orElse("feature");

        this.datasetData = datasetData;
    }

    public List<OgcApiLink> getLinks() {
        return links
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|items|home)$"))
                .collect(Collectors.toList());
    }

    public OgcApiCollection getCollection() { return collection; }

    public List<NavigationDTO> getBreadCrumbs() {
        return breadCrumbs;
    }

    public List<NavigationDTO> getFormats() {
        return links
                .stream()
                .filter(wfs3Link -> Objects.equals(wfs3Link.getRel(), "alternate"))
                .sorted(Comparator.comparing(link -> link.getTypeLabel()
                        .toUpperCase()))
                .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
                .collect(Collectors.toList());
    }
}
