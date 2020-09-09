/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.common.domain.OgcApiExtent;
import de.ii.ldproxy.ogcapi.common.domain.OgcApiExtentSpatial;
import de.ii.ldproxy.ogcapi.common.domain.OgcApiExtentTemporal;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.StyleEntry;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import org.apache.felix.ipojo.annotations.Requires;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class OgcApiCollectionView extends OgcApiView {

    @Requires
    private I18n i18n;

    private final OgcApiCollection collection;
    public String itemType;
    public boolean spatialSearch;
    public Map<String, String> bbox2;
    public Map<String, String> temporalExtent;
    public List<String> crs;
    public boolean hasGeometry;
    public String storageCrs;
    public Metadata metadata;
    public Link items;
    private List<StyleEntry> styleEntries;
    public String defaultStyle;
    public boolean withStyleInfos;
    public final String itemTypeTitle;
    public final String dataTitle;
    public final String metadataTitle;
    public final String licenseTitle;
    public final String downloadTitle;
    public final String spatialExtentTitle;
    public final String temporalExtentTitle;
    public final String supportedCrsTitle;
    public final String storageCrsTitle;
    public final String additionalLinksTitle;
    public final String expertInformationTitle;
    public final String defaultStyleTitle;
    public final String styleInfosTitle;
    public final String collectionInformationTitle;
    public final String mainLinksTitle;

    public String none;

    public OgcApiCollectionView(OgcApiDataV2 apiData, OgcApiCollection collection,
                                final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfiguration htmlConfig,
                                boolean noIndex, I18n i18n, Optional<Locale> language) {
        super("collection.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                collection.getLinks(),
                collection
                        .getTitle()
                        .orElse(collection.getId()),
                collection
                        .getDescription()
                        .orElse(""));
        this.collection = collection;

        this.items = collection
                .getLinks()
                .stream()
                .filter(link -> link.getRel().equalsIgnoreCase("items"))
                .filter(link -> link.getType().equalsIgnoreCase("text/html"))
                .findFirst()
                .orElse(null);
        this.crs = collection
                .getCrs();
        this.storageCrs = collection
                .getStorageCrs()
                .orElse(null);
        this.hasGeometry = apiData.getSpatialExtent().isPresent();
        Optional<String> defaultStyleOrNull = (Optional<String>) collection
                .getExtensions()
                .get("defaultStyle");
        this.defaultStyle = defaultStyleOrNull==null ? null : defaultStyleOrNull.get();
        this.styleEntries = (List<StyleEntry>) collection
                .getExtensions()
                .get("styles");
        this.withStyleInfos = (this.styleEntries!=null);
        Optional<OgcApiExtent> extent = collection.getExtent();
        if (extent.isPresent()) {
            OgcApiExtentSpatial spatialExtent = extent.get()
                                                      .getSpatial()
                                                      .orElse(null);
            if (Objects.nonNull(spatialExtent)) {
                double[] boundingBox = spatialExtent.getBbox()[0]; // TODO just the first bbox and it is assumed to be in CRS84
                this.bbox2 = ImmutableMap.of(
                        "minLng", Double.toString(boundingBox[0]),
                        "minLat", Double.toString(boundingBox[1]),
                        "maxLng", Double.toString(boundingBox[2]),
                        "maxLat", Double.toString(boundingBox[3]));
            }
            OgcApiExtentTemporal temporalExtent = extent.get()
                                                        .getTemporal()
                                                        .orElse(null);
            if (Objects.nonNull(temporalExtent)) {
                String[] interval = temporalExtent.getInterval()[0]; // TODO just the first interval and it is assumed to be in Gregorian calendar
                if (interval==null)
                    this.temporalExtent = null;
                else if (interval[0]==null && interval[1]==null)
                    this.temporalExtent = ImmutableMap.of();
                else if (interval[0]==null)
                    this.temporalExtent = interval==null ? null : ImmutableMap.of(
                            "end", interval[1]==null ? null : String.valueOf(Instant.parse(interval[1]).toEpochMilli()));
                else if (interval[1]==null)
                    this.temporalExtent = interval==null ? null : ImmutableMap.of(
                            "start", interval[0]==null ? null : String.valueOf(Instant.parse(interval[0]).toEpochMilli()));
                else
                    this.temporalExtent = interval==null ? null : ImmutableMap.of(
                            "start", interval[0]==null ? null : String.valueOf(Instant.parse(interval[0]).toEpochMilli()),
                            "end", interval[1]==null ? null : String.valueOf(Instant.parse(interval[1]).toEpochMilli()));
            }
        } else {
            this.bbox2 = null;
            this.temporalExtent = null;
        }
        this.spatialSearch = false;
        this.itemType = i18n.get(collection.getItemType().orElse("feature"), language);
        this.itemTypeTitle = i18n.get("itemTypeTitle", language);
        this.dataTitle = i18n.get("dataTitle", language);
        this.licenseTitle = i18n.get("licenseTitle", language);
        this.metadataTitle = i18n.get("metadataTitle", language);
        this.downloadTitle = i18n.get("downloadTitle", language);
        this.spatialExtentTitle = i18n.get("spatialExtentTitle", language);
        this.temporalExtentTitle = i18n.get("temporalExtentTitle", language);
        this.supportedCrsTitle = i18n.get("supportedCrsTitle", language);
        this.storageCrsTitle = i18n.get("storageCrsTitle", language);
        this.additionalLinksTitle = i18n.get("additionalLinksTitle", language);
        this.expertInformationTitle = i18n.get ("expertInformationTitle", language);
        this.defaultStyleTitle = i18n.get ("defaultStyleTitle", language);
        this.styleInfosTitle = i18n.get ("styleInfosTitle", language);
        this.mainLinksTitle = i18n.get ("mainLinksTitle", language);
        this.collectionInformationTitle = i18n.get ("collectionInformationTitle", language);

        this.none = i18n.get ("none", language);
    }

    public List<Link> getLinks() {
        return links
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|items|tiles|describedby|license|enclosure|ogc-dapa)$"))
                .collect(Collectors.toList());
    }

    public boolean hasMetadata() {
        return !getMetadataLinks().isEmpty();
    }

    public List<Link> getMetadataLinks() {
        return links
                .stream()
                .filter(link -> link.getRel().matches("^(?:describedby)$"))
                .collect(Collectors.toList());
    }

    public boolean hasLicense() {
        return !getLicenseLinks().isEmpty();
    }

    public List<Link> getLicenseLinks() {
        return links
                .stream()
                .filter(link -> link.getRel().matches("^(?:license)$"))
                .collect(Collectors.toList());
    }

    public boolean hasDownload() {
        return !getDownloadLinks().isEmpty();
    }

    public List<Link> getDownloadLinks() {
        return links
                .stream()
                .filter(link -> link.getRel().matches("^(?:enclosure)$"))
                .collect(Collectors.toList());
    }

    public Optional<Link> getTiles() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "tiles"))
                .findFirst();
    }

    public Optional<Link> getDapa() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "ogc-dapa"))
                .findFirst();
    }

    public OgcApiCollection getCollection() { return collection; }

    public List<StyleEntry> getStyles() {
        return styleEntries;
    }
}
