/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.collections.app.html;

import com.google.common.base.Charsets;
import de.ii.ldproxy.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ldproxy.ogcapi.common.domain.OgcApiDatasetView;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableStyle;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class OgcApiCollectionView extends OgcApiDatasetView {

    private final OgcApiCollection collection;
    public String itemType;
    public boolean spatialSearch;
    public Map<String, String> temporalExtent;
    public List<String> crs;
    public boolean hasGeometry;
    public String storageCrs;
    public Metadata metadata;
    public Link items;
    public String defaultStyle;
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
    public final boolean isDataset;
    public final MapClient mapClient;

    public String none;

    public OgcApiCollectionView(OgcApiDataV2 apiData, OgcApiCollection collection,
                                final List<NavigationDTO> breadCrumbs, String urlPrefix, HtmlConfiguration htmlConfig,
                                boolean noIndex, URICustomizer uriCustomizer, I18n i18n, Optional<Locale> language) {
        super("collection.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
                collection.getLinks(),
                collection.getTitle()
                          .orElse(collection.getId()),
                collection.getDescription()
                          .orElse(null),
                uriCustomizer,
                collection.getExtent(), language);
        this.collection = collection;
        this.isDataset = Objects.nonNull(htmlConfig) ? htmlConfig.getSchemaOrgEnabled() : false;

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
        Optional<String> defaultStyleOrNull = (Optional<String>) collection.getExtensions()
                                                                           .get("defaultStyle");
        this.defaultStyle = defaultStyleOrNull==null ? null : defaultStyleOrNull.get();
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
        this.mapClient = new ImmutableMapClient.Builder()
            .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
            .attribution(Optional.ofNullable(htmlConfig.getLeafletAttribution())
                .or(() -> Optional.ofNullable(htmlConfig.getBasemapAttribution())))
            .bounds(Optional.ofNullable(this.getBbox()))
            .drawBounds(true)
            .isInteractive(false)
            .defaultStyle(new ImmutableStyle.Builder().color("red").build())
            .build();

        this.none = i18n.get ("none", language);
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
                .collect(Collectors.toUnmodifiableList());
    }

    public boolean hasDownload() {
        return !getDownloadLinks().isEmpty();
    }

    public List<Link> getDownloadLinks() {
        return links
                .stream()
                .filter(link -> link.getRel().matches("^(?:enclosure)$"))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Link> getDistributionLinks() {
        return links.stream()
                    .filter(link -> Objects.equals(link.getRel(), "items") || Objects.equals(link.getRel(), "enclosure"))
                    .filter(link -> !"text/html".equals(link.getType()))
                    .collect(Collectors.toUnmodifiableList());
    }

    public List<Link> getTiles() {
        return links
                .stream()
                .filter(link -> link.getRel().startsWith("http://www.opengis.net/def/rel/ogc/1.0/tilesets-"))
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<Link> getStyles() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "http://www.opengis.net/def/rel/ogc/1.0/styles"))
                .findFirst();
    }

    public Optional<Link> getMap() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "ldp-map"))
                .findFirst();
    }

    public OgcApiCollection getCollection() { return collection; }

    public Optional<String> getSchemaOrgDataset() {
        // for cases with a single collection, that collection is not reported as a sub-dataset
        return apiData.getCollections().size() > 1
                ? Optional.of(getSchemaOrgDataset(apiData, Optional.of(apiData.getCollections()
                                                                              .get(collection.getId())), uriCustomizer.clearParameters()
                                                                                                                      .removeLastPathSegments(2)
                                                                                                                      .ensureNoTrailingSlash()
                                                                                                                      .copy(), false))
                : Optional.empty();
    }
}
