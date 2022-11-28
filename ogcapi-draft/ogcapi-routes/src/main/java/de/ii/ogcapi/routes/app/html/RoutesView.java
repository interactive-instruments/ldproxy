/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.app.html;

import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.ImmutableSource;
import de.ii.ogcapi.html.domain.ImmutableStyle;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.OgcApiView;
import de.ii.ogcapi.routes.domain.HtmlFormDefaults;
import de.ii.ogcapi.routes.domain.RouteDefinitionInfo;
import de.ii.ogcapi.routes.domain.Routes;
import de.ii.ogcapi.routes.domain.RoutingConfiguration;
import de.ii.ogcapi.routes.domain.RoutingFlag;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new")
public abstract class RoutesView extends OgcApiView {

  public abstract Routes routes();

  public abstract HtmlFormDefaults htmlDefaults();

  public abstract Optional<BoundingBox> bbox();

  public abstract I18n i18n();

  public abstract Optional<Locale> language();

  @Value.Derived
  public String routeNameTitle() {
    return i18n().get("routeNameTitle", language());
  }

  @Value.Derived
  public String preferenceTitle() {
    return i18n().get("preferenceTitle", language());
  }

  @Value.Derived
  public String modeTitle() {
    return i18n().get("modeTitle", language());
  }

  @Value.Derived
  public String additionalFlagsTitle() {
    return i18n().get("additionalFlagsTitle", language());
  }

  @Value.Derived
  public String startLocationTitle() {
    return i18n().get("startLocationTitle", language());
  }

  @Value.Derived
  public String endLocationTitle() {
    return i18n().get("endLocationTitle", language());
  }

  @Value.Derived
  public String crsTitle() {
    return i18n().get("crsTitle", language());
  }

  @Value.Derived
  public String xTitle() {
    return i18n().get("xTitle", language());
  }

  @Value.Derived
  public String yTitle() {
    return i18n().get("yTitle", language());
  }

  @Value.Derived
  public String maxWeightTitle() {
    return i18n().get("maxWeightTitle", language());
  }

  @Value.Derived
  public String maxHeightTitle() {
    return i18n().get("maxHeightTitle", language());
  }

  @Value.Derived
  public String weightUnitTitle() {
    return i18n().get("weightUnitTitle", language());
  }

  @Value.Derived
  public String heightUnitTitle() {
    return i18n().get("heightUnitTitle", language());
  }

  @Value.Derived
  public String computeRouteButton() {
    return i18n().get("computeRouteButton", language());
  }

  @Value.Derived
  public MapClient mapClient() {
    return new ImmutableMapClient.Builder()
        .backgroundUrl(
            Optional.ofNullable(htmlConfig().getLeafletUrl())
                .or(() -> Optional.ofNullable(htmlConfig().getBasemapUrl())))
        .attribution(
            Optional.ofNullable(htmlConfig().getLeafletAttribution())
                .or(() -> Optional.ofNullable(htmlConfig().getBasemapAttribution())))
        .data(
            new ImmutableSource.Builder()
                .type(MapClient.Source.TYPE.geojson)
                .url("{\"type\":\"FeatureCollection\",\"features\":[]}")
                .isData(true)
                .build())
        .popup(MapClient.Popup.CLICK_PROPERTIES)
        .defaultStyle(
            new ImmutableStyle.Builder()
                .color("#00f")
                .lineWidth(5)
                .circleRadius(6)
                .circleMinZoom(12)
                .build())
        .build();
  }

  @Value.Derived
  public boolean supportsMaxWeight() {
    return apiData()
        .getExtension(RoutingConfiguration.class)
        .map(RoutingConfiguration::supportsWeightRestrictions)
        .orElse(false);
  }

  @Value.Derived
  public boolean supportsMaxHeight() {
    return apiData()
        .getExtension(RoutingConfiguration.class)
        .map(RoutingConfiguration::supportsHeightRestrictions)
        .orElse(false);
  }

  @Value.Derived
  public boolean supportsObstacles() {
    return apiData()
        .getExtension(RoutingConfiguration.class)
        .map(RoutingConfiguration::supportsObstacles)
        .orElse(false);
  }

  @Value.Derived
  public String routesListTitle() {
    return i18n().get("routesListTitle", language());
  }

  @Value.Derived
  public String routesListDescription() {
    return i18n().get("routesListDescription", language());
  }

  @Value.Derived
  public RouteDefinitionInfo templateInfo() {
    return routes().getTemplateInfo().orElse(null);
  }

  public RoutesView() {
    super("routes.mustache");
  }

  public Optional<Map.Entry<String, String>> getDefaultPreference() {
    return templateInfo().getPreferences().entrySet().stream()
        .filter(entry -> entry.getKey().equals(templateInfo().getDefaultPreference()))
        .findFirst();
  }

  public Optional<Map.Entry<String, String>> getDefaultMode() {
    return templateInfo().getModes().entrySet().stream()
        .filter(entry -> entry.getKey().equals(templateInfo().getDefaultMode()))
        .findFirst();
  }

  public Set<Map.Entry<String, String>> getOtherPreferences() {
    return templateInfo().getPreferences().entrySet().stream()
        .filter(entry -> !entry.getKey().equals(templateInfo().getDefaultPreference()))
        .collect(ImmutableSet.toImmutableSet());
  }

  public Set<Map.Entry<String, String>> getOtherModes() {
    return templateInfo().getModes().entrySet().stream()
        .filter(entry -> !entry.getKey().equals(templateInfo().getDefaultMode()))
        .collect(ImmutableSet.toImmutableSet());
  }

  public Set<Map.Entry<String, RoutingFlag>> getAdditionalFlags() {
    return templateInfo().getAdditionalFlags().entrySet();
  }

  public boolean hasAdditionalFlags() {
    return !templateInfo().getAdditionalFlags().isEmpty();
  }

  public boolean hasLinks() {
    return links().stream().anyMatch(link -> Objects.equals(link.getRel(), "item"));
  }

  public List<Link> getItemLinks() {
    return links().stream()
        .filter(link -> Objects.equals(link.getRel(), "item"))
        .collect(Collectors.toUnmodifiableList());
  }

  public String getRouteName() {
    return htmlDefaults().getName().orElse("Route");
  }

  public Float getStartX() {
    List<Float> pos = htmlDefaults().getStart();
    if (Objects.nonNull(pos) && pos.size() >= 2) return pos.get(0);
    return null;
  }

  public Float getStartY() {
    List<Float> pos = htmlDefaults().getStart();
    if (Objects.nonNull(pos) && pos.size() >= 2) return pos.get(1);
    return null;
  }

  public Float getEndX() {
    List<Float> pos = htmlDefaults().getEnd();
    if (Objects.nonNull(pos) && pos.size() >= 2) return pos.get(0);
    return null;
  }

  public Float getEndY() {
    List<Float> pos = htmlDefaults().getEnd();
    if (Objects.nonNull(pos) && pos.size() >= 2) return pos.get(1);
    return null;
  }

  public double getCenterX() {
    List<Float> pos = htmlDefaults().getCenter();
    if (Objects.nonNull(pos) && pos.size() >= 2) return pos.get(0);
    return 0.0;
  }

  public double getCenterY() {
    List<Float> pos = htmlDefaults().getCenter();
    if (Objects.nonNull(pos) && pos.size() >= 2) return pos.get(1);
    return 0.0;
  }

  public int getCenterLevel() {
    return Objects.requireNonNullElse(htmlDefaults().getCenterLevel(), 0);
  }

  @Override
  public String getProcessedAttribution() {
    Optional<String> datasetAttribution =
        apiData().getMetadata().flatMap(ApiMetadata::getAttribution);
    if (datasetAttribution.isEmpty()) return super.getProcessedAttribution();

    return apiData().getMetadata().flatMap(ApiMetadata::getAttribution).get()
        + " | "
        + super.getProcessedAttribution();
  }
}
