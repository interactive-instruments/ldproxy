/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import org.immutables.value.Value;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinitionInfo.Builder.class)
public interface RouteDefinitionInfo {
  Map<String, String> getPreferences();
  String getDefaultPreference();
  Map<String, String> getModes();
  String getDefaultMode();
  Map<String, String> getAdditionalFlags();
  Map<String, String> getCrs();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<RouteDefinitionInfo> FUNNEL = (from, into) -> {
    from.getPreferences()
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(entry -> {
          into.putString(entry.getKey(), StandardCharsets.UTF_8);
          into.putString(entry.getValue(), StandardCharsets.UTF_8);
        });
    into.putString(from.getDefaultPreference(), StandardCharsets.UTF_8);
    from.getModes()
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(entry -> {
          into.putString(entry.getKey(), StandardCharsets.UTF_8);
          into.putString(entry.getValue(), StandardCharsets.UTF_8);
        });
    into.putString(from.getDefaultMode(), StandardCharsets.UTF_8);
    from.getAdditionalFlags()
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(entry -> {
          into.putString(entry.getKey(), StandardCharsets.UTF_8);
          into.putString(entry.getValue(), StandardCharsets.UTF_8);
        });
    from.getCrs()
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(entry -> {
          into.putString(entry.getKey(), StandardCharsets.UTF_8);
          into.putString(entry.getValue(), StandardCharsets.UTF_8);
        });
  };

}
