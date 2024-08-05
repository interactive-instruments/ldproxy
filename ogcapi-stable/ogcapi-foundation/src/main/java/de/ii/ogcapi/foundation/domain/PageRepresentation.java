/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.immutables.value.Value;

public abstract class PageRepresentation {

  @SuppressWarnings("UnstableApiUsage")
  public static final Funnel<PageRepresentation> FUNNEL =
      (from, into) -> {
        from.getTitle().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getDescription().ifPresent(s -> into.putString(s, StandardCharsets.UTF_8));
        from.getLinks().stream()
            .sorted(Comparator.comparing(Link::getHref))
            .forEachOrdered(
                link ->
                    into.putString(link.getHref(), StandardCharsets.UTF_8)
                        .putString(
                            Objects.requireNonNullElse(link.getRel(), ""), StandardCharsets.UTF_8));
      };

  public static final String SORT_PRIORITY = "sortPriority";

  public abstract Optional<String> getTitle();

  public abstract Optional<String> getDescription();

  @JsonIgnore
  public abstract List<Link> getLinks();

  @JsonProperty(value = "links", required = true)
  @Value.Derived
  public List<Link> getOrderedLinks() {
    return getLinks().stream()
        .sorted(Link.COMPARATOR_LINKS)
        .collect(Collectors.toUnmodifiableList());
  }

  @JsonIgnore
  public abstract Optional<Date> getLastModified();

  @JsonIgnore
  public abstract List<Map<String, Object>> getSections();

  @JsonIgnore
  @Value.Derived
  public List<Map<String, Object>> getOrderedSections() {
    return getSections().stream()
        .sorted(
            (stringObjectMap, t1) -> {
              if (!stringObjectMap.containsKey(SORT_PRIORITY)) {
                return 1;
              }
              if (!t1.containsKey(SORT_PRIORITY)) {
                return -1;
              }
              return ((Integer) stringObjectMap.get(SORT_PRIORITY))
                  - (((Integer) t1.get(SORT_PRIORITY)));
            })
        .collect(Collectors.toList());
  }

  @JsonIgnore
  @Value.Default
  public boolean getSectionsFirst() {
    return false;
  }
}
