/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.html.domain;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import io.dropwizard.views.View;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.immutables.value.Value;

public abstract class OgcApiView extends View {

  public abstract HtmlConfiguration htmlConfig();

  public abstract String urlPrefix();

  public abstract boolean noIndex();

  @Nullable
  public abstract List<NavigationDTO> breadCrumbs();

  @Value.Default
  public List<Link> links() {
    return ImmutableList.of();
  }

  // Constructor Variables as new Member

  @Nullable
  public abstract String title();

  @Nullable
  public abstract String description();

  @Nullable
  public abstract OgcApiDataV2 apiData();

  public OgcApiView(String templateName) {
    super(String.format("/templates/%s", templateName), Charsets.UTF_8);
  }

  public String getUrlPrefix() {
    return urlPrefix();
  }

  public String getTitle() {
    return title();
  }

  public String getDescription() {
    return description();
  }

  public List<NavigationDTO> getProcessedFormats() {
    return links().stream()
        .filter(link -> Objects.equals(link.getRel(), "alternate") && !link.getTypeLabel().isBlank())
        .sorted(Comparator.comparing(link -> link.getTypeLabel().toUpperCase()))
        .map(link -> new NavigationDTO(link.getTypeLabel(), link.getHref()))
        .collect(Collectors.toList());
  }

  public List<NavigationDTO> getProcessedBreadCrumbs() {
    return breadCrumbs();
  }

  public boolean hasBreadCrumbs() {
    if (breadCrumbs() != null) {
      return breadCrumbs().size() > 1;
    }
    return false;
  }

  public String getProcessedBreadCrumbsList() {
    String result = "";
    for (int i = 0; i < breadCrumbs().size(); i++) {
      NavigationDTO item = breadCrumbs().get(i);
      result +=
          "{ \"@type\": \"ListItem\", \"position\": "
              + (i + 1)
              + ", \"name\": \""
              + item.label
              + "\"";
      if (Objects.nonNull(item.url)) {
        result += ", \"item\": \"" + item.url + "\"";
      }
      result += " }";
      if (i < breadCrumbs().size() - 1) {
        result += ",\n    ";
      }
    }
    return result;
  }

  public String getProcessedAttribution() {
    if (Objects.nonNull(htmlConfig().getLeafletAttribution())) {
      return htmlConfig().getLeafletAttribution();
    }
    if (Objects.nonNull(htmlConfig().getOpenLayersAttribution())) {
      return htmlConfig().getOpenLayersAttribution();
    }
    return htmlConfig().getBasemapAttribution();
  }
}
