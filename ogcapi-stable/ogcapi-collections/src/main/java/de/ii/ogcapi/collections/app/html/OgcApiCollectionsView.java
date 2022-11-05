/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.collections.app.html;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.collections.domain.Collections;
import de.ii.ogcapi.collections.domain.OgcApiCollection;
import de.ii.ogcapi.common.domain.OgcApiDatasetView;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
public abstract class OgcApiCollectionsView extends OgcApiDatasetView {

  abstract Collections collectionsC();

  abstract List<OgcApiCollection> collections();

  abstract Optional<BoundingBox> spatialExtent();

  abstract boolean showCollectionDescriptions();

  abstract I18n i18n();

  abstract Optional<String> dataSourceUrl();

  @Value.Derived
  boolean hasGeometry() {
    return spatialExtent().isPresent();
  }

  abstract String keywords();

  abstract List<String> crs();

  abstract ApiMetadata metadata();

  @Value.Derived
  String collectionsTitle() {
    return i18n().get("collectionsTitle", language());
  }

  @Value.Derived
  String supportedCrsTitle() {
    return i18n().get("supportedCrsTitle", language());
  }

  @Value.Derived
  String metadataTitle() {
    return i18n().get("metadataTitle", language());
  }

  @Value.Derived
  String licenseTitle() {
    return i18n().get("icenseTitle", language());
  }

  @Value.Derived
  String downloadTitle() {
    return i18n().get("downloadTitle", language());
  }

  @Value.Derived
  String additionalLinksTitle() {
    return i18n().get("additionalLinksTitle", language());
  }

  @Value.Derived
  String expertInformationTitle() {
    return i18n().get("additionalLinksTitle", language());
  }

  @Value.Derived
  String none() {
    return i18n().get("none", language());
  }

  @Value.Derived
  String moreInformation() {
    return i18n().get("moreInformation", language());
  }

  public OgcApiCollectionsView() {
    super("collections.mustache");
  }

  public List<Link> getLinks() {
    return links().stream()
        .filter(
            link -> !link.getRel().matches("^(?:self|alternate|describedby|license|enclosure)$"))
        .collect(Collectors.toList());
  }

  public boolean hasMetadata() {
    return !getMetadataLinks().isEmpty();
  }

  public List<Link> getMetadataLinks() {
    return links().stream()
        .filter(link -> link.getRel().matches("^(?:describedby)$"))
        .collect(Collectors.toList());
  }

  public boolean hasLicense() {
    return !getLicenseLinks().isEmpty();
  }

  public List<Link> getLicenseLinks() {
    return links().stream()
        .filter(link -> link.getRel().matches("^(?:license)$"))
        .collect(Collectors.toList());
  }

  public boolean hasDownload() {
    return !getDownloadLinks().isEmpty();
  }

  public List<Link> getDownloadLinks() {
    return links().stream()
        .filter(link -> link.getRel().matches("^(?:enclosure)$"))
        .collect(Collectors.toList());
  }

  public List<Map<String, String>> getCollections() {

    Comparator<OgcApiCollection> byTitle =
        Comparator.comparing(collection -> collection.getTitle().orElse(collection.getId()));

    return collections().stream()
        .sorted(byTitle)
        .map(
            collection ->
                ImmutableMap.of(
                    "title",
                    collection.getTitle().orElse(collection.getId()),
                    "description",
                    showCollectionDescriptions() ? collection.getDescription().orElse("") : "",
                    "id",
                    collection.getId(),
                    "hrefcollection",
                    collection.getLinks().stream()
                        .filter(link -> link.getRel().equalsIgnoreCase("self"))
                        .findFirst()
                        .map(link -> link.getHref())
                        .orElse(""),
                    "hrefitems",
                    collection.getLinks().stream()
                        .filter(link -> link.getRel().equalsIgnoreCase("self"))
                        .findFirst()
                        .map(link -> link.getHref() + "/items")
                        .orElse(""),
                    "itemType",
                    i18n().get(collection.getItemType().orElse("feature"), language()),
                    "itemCount",
                    collection.getItemCount().map(Object::toString).orElse("")))
        .collect(Collectors.toList());
  }
}
