/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.app;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiCatalog;
import de.ii.ogcapi.foundation.domain.ApiCatalogEntry;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class ServiceOverviewView extends OgcApiView {
  public URI uri;
  public boolean isApiCatalog = true;
  public String tagsTitle;
  public String canonicalUrl;
  public List<ApiCatalogEntry> data;
  public Optional<String> googleSiteVerification;

  public ServiceOverviewView(
      URI uri,
      ApiCatalog apiCatalog,
      HtmlConfiguration htmlConfig,
      I18n i18n,
      Optional<Locale> language) {
    super(
        "services.mustache",
        Charsets.UTF_8,
        null,
        new ImmutableList.Builder<NavigationDTO>()
            .add(new NavigationDTO(i18n.get("root", language), true))
            .build(),
        htmlConfig,
        htmlConfig.getNoIndexEnabled(),
        apiCatalog.getUrlPrefix(),
        apiCatalog.getLinks(),
        apiCatalog.getTitle().orElse(i18n.get("rootTitle", language)),
        apiCatalog.getDescription().orElse(i18n.get("rootDescription", language)));
    this.data = apiCatalog.getApis();
    this.uri = uri;
    this.canonicalUrl = apiCatalog.getCatalogUri().toString();
    this.googleSiteVerification = apiCatalog.getGoogleSiteVerification();
    this.tagsTitle = i18n.get("tagsTitle", language);
  }

  public String getDatasetsAsString() {
    return data.stream()
        .filter(ApiCatalogEntry::isDataset)
        .map(
            api ->
                "{ \"@type\": \"Dataset\", "
                    + "\"name\": \""
                    + api.getTitle().orElse(api.getId()).replace("\"", "\\\"")
                    + "\", "
                    + "\"description\": \""
                    + api.getDescription().orElse("").replace("\"", "\\\"")
                    + "\", "
                    + "\"sameAs\": \""
                    + api.getLandingPageUri()
                    + "\""
                    + " }")
        .collect(Collectors.joining(", "));
  }

  public boolean hasTags() {
    return !getAllTags().isEmpty();
  }

  public List<String> getAllTags() {
    return data.stream()
        .map(api -> api.getTags())
        .flatMap(Collection::stream)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }
}
