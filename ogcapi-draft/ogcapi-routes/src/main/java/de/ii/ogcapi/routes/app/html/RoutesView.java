/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.routes.app.html;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.Metadata;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.ImmutableSource;
import de.ii.ogcapi.html.domain.ImmutableStyle;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.NavigationDTO;
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

public class RoutesView extends OgcApiView {

    public final String routeNameTitle;
    public final String preferenceTitle;
    public final String modeTitle;
    public final String additionalFlagsTitle;
    public final String startLocationTitle;
    public final String endLocationTitle;
    public final String crsTitle;
    public final String xTitle;
    public final String yTitle;
    public final String maxWeightTitle;
    public final String maxHeightTitle;
    public final String weightUnitTitle;
    public final String heightUnitTitle;
    public final String computeRouteButton;
    public Map<String, String> bbox;
    public final MapClient mapClient;
    public final boolean supportsMaxWeight;
    public final boolean supportsMaxHeight;
    public final boolean supportsObstacles;
    public final String routesListTitle;
    public final String routesListDescription;

    private final RouteDefinitionInfo templateInfo;
    private final HtmlFormDefaults htmlDefaults;

    public RoutesView(OgcApiDataV2 apiData, Routes routes, HtmlFormDefaults htmlDefaults, Optional<BoundingBox> bbox, final List<NavigationDTO> breadCrumbs,
                      String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex, I18n i18n, Optional<Locale> language) {
        super("routes.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
              routes.getLinks(),
              i18n.get("routesTitle", language),
              apiData.getExtension(RoutingConfiguration.class)
                  .map(RoutingConfiguration::supportsObstacles)
                  .orElse(false)
                  ? String.format("%s %s", i18n.get("routesDescription", language), i18n.get("routesDescriptionObstacles", language))
                  : i18n.get("routesDescription", language));
        this.templateInfo = routes.getTemplateInfo().orElse(null);
        this.htmlDefaults = htmlDefaults;
        routeNameTitle = i18n.get("routeNameTitle", language);
        preferenceTitle = i18n.get("preferenceTitle", language);
        modeTitle = i18n.get("modeTitle", language);
        additionalFlagsTitle = i18n.get("additionalFlagsTitle", language);
        startLocationTitle = i18n.get("startLocationTitle", language);
        endLocationTitle = i18n.get("endLocationTitle", language);
        crsTitle = i18n.get("crsTitle", language);
        computeRouteButton = i18n.get("computeRouteButton", language);
        xTitle = i18n.get("xTitle", language);
        yTitle = i18n.get("yTitle", language);
        maxWeightTitle = i18n.get("maxWeightTitle", language);
        maxHeightTitle = i18n.get("maxHeightTitle", language);
        weightUnitTitle = i18n.get("weightUnitTitle", language);
        heightUnitTitle = i18n.get("heightUnitTitle", language);
        routesListTitle = i18n.get("routesListTitle", language);
        routesListDescription = i18n.get("routesListDescription", language);

        this.bbox = bbox
            .map(boundingBox -> ImmutableMap.of(
                "minLng", Double.toString(boundingBox.getXmin()),
                "minLat", Double.toString(boundingBox.getYmin()),
                "maxLng", Double.toString(boundingBox.getXmax()),
                "maxLat", Double.toString(boundingBox.getYmax())))
            .orElse(null);

        this.mapClient = new ImmutableMapClient.Builder()
            .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                               .or(() -> Optional.ofNullable(htmlConfig.getBasemapUrl())))
            .attribution(Optional.ofNullable(htmlConfig.getLeafletAttribution())
                             .or(() -> Optional.ofNullable(htmlConfig.getBasemapAttribution())))
            .data(new ImmutableSource.Builder()
                      .type(MapClient.Source.TYPE.geojson)
                      .url("{\"type\":\"FeatureCollection\",\"features\":[]}")
                      .isData(true)
                      .build())
            .popup(MapClient.Popup.CLICK_PROPERTIES)
            .defaultStyle(new ImmutableStyle.Builder()
                .color("#00f")
                .lineWidth(5)
                .circleRadius(6)
                .circleMinZoom(12)
                .build())
            .build();

        this.supportsMaxWeight = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::supportsWeightRestrictions)
            .orElse(false);
        this.supportsMaxHeight = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::supportsHeightRestrictions)
            .orElse(false);
        this.supportsObstacles = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::supportsObstacles)
            .orElse(false);
    }

    public Optional<Map.Entry<String,String>> getDefaultPreference() {
        return templateInfo
            .getPreferences()
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().equals(templateInfo.getDefaultPreference()))
            .findFirst();
    }

    public Optional<Map.Entry<String,String>> getDefaultMode() {
        return templateInfo
            .getModes()
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().equals(templateInfo.getDefaultMode()))
            .findFirst();
    }

    public Set<Map.Entry<String,String>> getOtherPreferences() {
        return templateInfo
            .getPreferences()
            .entrySet()
            .stream()
            .filter(entry -> !entry.getKey().equals(templateInfo.getDefaultPreference()))
            .collect(ImmutableSet.toImmutableSet());
    }

    public Set<Map.Entry<String,String>> getOtherModes() {
        return templateInfo
            .getModes()
            .entrySet()
            .stream()
            .filter(entry -> !entry.getKey().equals(templateInfo.getDefaultMode()))
            .collect(ImmutableSet.toImmutableSet());
    }

    public Set<Map.Entry<String, RoutingFlag>> getAdditionalFlags() {
        return templateInfo
            .getAdditionalFlags()
            .entrySet();
    }

    public boolean hasAdditionalFlags() {
        return !templateInfo
            .getAdditionalFlags()
            .isEmpty();
    }

    public boolean hasLinks() {
        return links.stream()
            .anyMatch(link -> Objects.equals(link.getRel(), "item"));
    }

    public List<Link> getItemLinks() {
        return links.stream()
            .filter(link -> Objects.equals(link.getRel(), "item"))
            .collect(Collectors.toUnmodifiableList());
    }

    public String getRouteName() {
        return htmlDefaults.getName().orElse("Route");
    }

    public Float getStartX() {
        List<Float> pos = htmlDefaults.getStart();
        if (Objects.nonNull(pos) && pos.size()>=2)
            return pos.get(0);
        return null;
    }

    public Float getStartY() {
        List<Float> pos = htmlDefaults.getStart();
        if (Objects.nonNull(pos) && pos.size()>=2)
            return pos.get(1);
        return null;
    }

    public Float getEndX() {
        List<Float> pos = htmlDefaults.getEnd();
        if (Objects.nonNull(pos) && pos.size()>=2)
            return pos.get(0);
        return null;
    }

    public Float getEndY() {
        List<Float> pos = htmlDefaults.getEnd();
        if (Objects.nonNull(pos) && pos.size()>=2)
            return pos.get(1);
        return null;
    }

    public double getCenterX() {
        List<Float> pos = htmlDefaults.getCenter();
        if (Objects.nonNull(pos) && pos.size()>=2)
            return pos.get(0);
        return 0.0;
    }

    public double getCenterY() {
        List<Float> pos = htmlDefaults.getCenter();
        if (Objects.nonNull(pos) && pos.size()>=2)
            return pos.get(1);
        return 0.0;
    }

    public int getCenterLevel() {
        return Objects.requireNonNullElse(htmlDefaults.getCenterLevel(), 0);
    }

    @Override
    public String getAttribution() {
        Optional<String> datasetAttribution = apiData.getMetadata().flatMap(Metadata::getAttribution);
        if (datasetAttribution.isEmpty())
            return super.getAttribution();

        return apiData.getMetadata().flatMap(Metadata::getAttribution).get() + " | " + super.getAttribution();
    }
}
