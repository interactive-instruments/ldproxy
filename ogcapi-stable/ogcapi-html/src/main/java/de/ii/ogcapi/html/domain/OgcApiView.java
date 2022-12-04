/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.domain;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.dropwizard.views.View;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public abstract class OgcApiView extends View {

  protected final OgcApiDataV2 apiData;
  protected final List<NavigationDTO> breadCrumbs;
  protected final List<Link> links;
  protected final HtmlConfiguration htmlConfig;
  protected final String urlPrefix;
  protected final String title;
  protected final String description;
  protected final boolean noIndex;

  protected OgcApiView(
      String templateName,
      @Nullable Charset charset,
      @Nullable OgcApiDataV2 apiData,
      List<NavigationDTO> breadCrumbs,
      HtmlConfiguration htmlConfig,
      boolean noIndex,
      String urlPrefix,
      @Nullable List<Link> links,
      @Nullable String title,
      @Nullable String description) {
    super(String.format("/templates/%s", templateName), charset);
    this.apiData = apiData;
    this.breadCrumbs = Objects.requireNonNullElse(breadCrumbs, ImmutableList.of());
    this.links = Objects.requireNonNullElse(links, ImmutableList.of());
    this.htmlConfig = htmlConfig;
    this.noIndex = noIndex;
    this.urlPrefix = urlPrefix;
    this.title = title;
    this.description = description;
  }

  public List<NavigationDTO> getFormats() {
    return links.stream()
        .filter(
            link -> Objects.equals(link.getRel(), "alternate") && !link.getTypeLabel().isBlank())
        .sorted(Comparator.comparing(link -> link.getTypeLabel().toUpperCase(Locale.ROOT)))
        .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
        .collect(Collectors.toList());
  }

  public List<NavigationDTO> getBreadCrumbs() {
    return breadCrumbs;
  }

  @SuppressWarnings("unused")
  public boolean hasBreadCrumbs() {
    return breadCrumbs.size() > 1;
  }

  @SuppressWarnings("unused")
  public String getBreadCrumbsList() {
    StringBuilder result = new StringBuilder(128);
    for (int i = 0; i < breadCrumbs.size(); i++) {
      NavigationDTO item = breadCrumbs.get(i);
      result
          .append("{ \"@type\": \"ListItem\", \"position\": ")
          .append(i + 1)
          .append(", \"name\": \"")
          .append(item.label)
          .append('"');
      if (Objects.nonNull(item.url)) {
        result.append(", \"item\": \"").append(item.url).append('"');
      }
      result.append(" }");
      if (i < breadCrumbs.size() - 1) {
        result.append(",\n    ");
      }
    }
    return result.toString();
  }

  public String getUrlPrefix() {
    return urlPrefix;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  @SuppressWarnings("deprecation")
  public String getAttribution() {
    if (Objects.nonNull(htmlConfig.getLeafletAttribution())) {
      return htmlConfig.getLeafletAttribution();
    }
    if (Objects.nonNull(htmlConfig.getOpenLayersAttribution())) {
      return htmlConfig.getOpenLayersAttribution();
    }
    return htmlConfig.getBasemapAttribution();
  }
}
