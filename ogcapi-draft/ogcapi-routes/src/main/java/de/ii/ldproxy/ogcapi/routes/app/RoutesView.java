/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.routes.app;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.ldproxy.ogcapi.domain.I18n;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableSource;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.ldproxy.ogcapi.routes.domain.HtmlFormDefaults;
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionInfo;
import de.ii.ldproxy.ogcapi.routes.domain.RoutingConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class RoutesView extends OgcApiView {

    public final String routeNameTitle;
    public final String preferenceTitle;
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

    private final RouteDefinitionInfo templateInfo;
    private final HtmlFormDefaults htmlDefaults;

    public RoutesView(OgcApiDataV2 apiData, RouteDefinitionInfo templateInfo, HtmlFormDefaults htmlDefaults, final List<NavigationDTO> breadCrumbs,
                      String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex, I18n i18n, Optional<Locale> language) {
        super("routes.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
              ImmutableList.of(),
              i18n.get("routesTitle", language),
              i18n.get("routesDescription", language));
        this.templateInfo = templateInfo;
        this.htmlDefaults = htmlDefaults;
        routeNameTitle = i18n.get("routeNameTitle", language);
        preferenceTitle = i18n.get("preferenceTitle", language);
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

        this.bbox = apiData.getSpatialExtent()
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
            .bounds(Optional.ofNullable(bbox))
            .data(new ImmutableSource.Builder()
                      .type(MapClient.Source.TYPE.geojson)
                      .url("data:application/geo+json,{type:\"FeatureCollection\",features:[]}")
                      .build())
            .popup(MapClient.Popup.CLICK_PROPERTIES)
            .styleUrl(Optional.ofNullable(null)) // TODO
            .removeZoomLevelConstraints(false) // TODO
            .build();

        this.supportsMaxWeight = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getLoadRestrictions)
            .orElse(false);
        this.supportsMaxHeight = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getHeightRestrictions)
            .orElse(false);
        this.supportsObstacles = apiData.getExtension(RoutingConfiguration.class)
            .map(RoutingConfiguration::getObstacles)
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

    public Set<Map.Entry<String,String>> getOtherPreferences() {
        return templateInfo
            .getPreferences()
            .entrySet()
            .stream()
            .filter(entry -> !entry.getKey().equals(templateInfo.getDefaultPreference()))
            .collect(ImmutableSet.toImmutableSet());
    }

    public Set<Map.Entry<String,String>> getAdditionalFlags() {
        return templateInfo
            .getAdditionalFlags()
            .entrySet();
    }

    public boolean hasAdditionalFlags() {
        return !templateInfo
            .getAdditionalFlags()
            .isEmpty();
    }

    public Set<Map.Entry<String,String>> getCrs() {
        return templateInfo
            .getCrs()
            .entrySet();
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
