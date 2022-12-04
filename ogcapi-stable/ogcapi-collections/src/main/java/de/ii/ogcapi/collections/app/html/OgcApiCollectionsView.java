/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.Collections;
import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.TooManyFields")
public class OgcApiCollectionsView extends OgcApiView {
  private final List<OgcApiCollection> collections;
  private final boolean showCollectionDescriptions;
  public final boolean hasGeometry;
  private final I18n i18n;
  private final Optional<Locale> language;
  public String keywords;
  public List<String> crs;
  public ApiMetadata metadata;
  public String collectionsTitle;
  public String supportedCrsTitle;
  public String metadataTitle;
  public String licenseTitle;
  public String downloadTitle;
  public String additionalLinksTitle;
  public String expertInformationTitle;
  public String none;
  public String moreInformation;

  public OgcApiCollectionsView(
      OgcApiDataV2 apiData,
      Collections collections,
      Optional<BoundingBox> spatialExtent,
      final List<NavigationDTO> breadCrumbs,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex,
      boolean showCollectionDescriptions,
      I18n i18n,
      Optional<Locale> language) {
    super(
        "collections.mustache",
        Charsets.UTF_8,
        apiData,
        breadCrumbs,
        htmlConfig,
        noIndex,
        urlPrefix,
        collections.getLinks(),
        collections.getTitle().orElse(apiData.getId()),
        collections.getDescription().orElse(""));
    this.i18n = i18n;
    this.language = language;
    this.collections = collections.getCollections();
    this.showCollectionDescriptions = showCollectionDescriptions;
    this.crs = collections.getCrs();
    this.hasGeometry = spatialExtent.isPresent();

    this.collectionsTitle = i18n.get("collectionsTitle", language);
    this.supportedCrsTitle = i18n.get("supportedCrsTitle", language);
    this.metadataTitle = i18n.get("metadataTitle", language);
    this.licenseTitle = i18n.get("licenseTitle", language);
    this.downloadTitle = i18n.get("downloadTitle", language);
    this.additionalLinksTitle = i18n.get("additionalLinksTitle", language);
    this.expertInformationTitle = i18n.get("expertInformationTitle", language);
    this.none = i18n.get("none", language);
    this.moreInformation = i18n.get("moreInformation", language);
  }

  public List<Link> getLinks() {
    return links.stream()
        .filter(
            link ->
                link.getRel() == null
                    || !link.getRel().matches("^self|alternate|describedby|license|enclosure$"))
        .collect(Collectors.toList());
  }

  public boolean hasMetadata() {
    return !getMetadataLinks().isEmpty();
  }

  public List<Link> getMetadataLinks() {
    return links.stream()
        .filter(link -> link.getRel() == null || link.getRel().matches("^describedby$"))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unused")
  public boolean hasLicense() {
    return !getLicenseLinks().isEmpty();
  }

  public List<Link> getLicenseLinks() {
    return links.stream()
        .filter(link -> link.getRel() == null || link.getRel().matches("^license$"))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unused")
  public boolean hasDownload() {
    return !getDownloadLinks().isEmpty();
  }

  public List<Link> getDownloadLinks() {
    return links.stream()
        .filter(link -> link.getRel() == null || link.getRel().matches("^enclosure$"))
        .collect(Collectors.toList());
  }

  public List<Map<String, String>> getCollections() {

    Comparator<OgcApiCollection> byTitle =
        Comparator.comparing(collection -> collection.getTitle().orElse(collection.getId()));

    return collections.stream()
        .sorted(byTitle)
        .map(
            collection ->
                ImmutableMap.of(
                    "title",
                    collection.getTitle().orElse(collection.getId()),
                    "description",
                    showCollectionDescriptions ? collection.getDescription().orElse("") : "",
                    "id",
                    collection.getId(),
                    "hrefcollection",
                    collection.getLinks().stream()
                        .filter(
                            link -> link.getRel() == null || link.getRel().equalsIgnoreCase("self"))
                        .findFirst()
                        .map(Link::getHref)
                        .orElse(""),
                    "hrefitems",
                    collection.getLinks().stream()
                        .filter(
                            link -> link.getRel() == null || link.getRel().equalsIgnoreCase("self"))
                        .findFirst()
                        .map(link -> link.getHref() + "/items")
                        .orElse(""),
                    "itemType",
                    i18n.get(collection.getItemType().orElse("feature"), language),
                    "itemCount",
                    collection.getItemCount().map(Object::toString).orElse("")))
        .collect(Collectors.toList());
  }
}
