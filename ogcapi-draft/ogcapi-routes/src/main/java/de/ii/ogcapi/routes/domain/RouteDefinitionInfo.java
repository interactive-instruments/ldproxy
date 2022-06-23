/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.hash.Funnel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true, deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableRouteDefinitionInfo.Builder.class)
public interface RouteDefinitionInfo {
  Map<String, String> getPreferences();

  String getDefaultPreference();

  Map<String, String> getModes();

  String getDefaultMode();

  Map<String, RoutingFlag> getAdditionalFlags();

  @SuppressWarnings("UnstableApiUsage")
  Funnel<RouteDefinitionInfo> FUNNEL =
      (from, into) -> {
        from.getPreferences().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(
                entry -> {
                  into.putString(entry.getKey(), StandardCharsets.UTF_8);
                  into.putString(entry.getValue(), StandardCharsets.UTF_8);
                });
        into.putString(from.getDefaultPreference(), StandardCharsets.UTF_8);
        from.getModes().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(
                entry -> {
                  into.putString(entry.getKey(), StandardCharsets.UTF_8);
                  into.putString(entry.getValue(), StandardCharsets.UTF_8);
                });
        into.putString(from.getDefaultMode(), StandardCharsets.UTF_8);
        from.getAdditionalFlags().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEachOrdered(
                entry -> {
                  into.putString(entry.getKey(), StandardCharsets.UTF_8);
                  RoutingFlag.FUNNEL.funnel(entry.getValue(), into);
                });
      };
}
