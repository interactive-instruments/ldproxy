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
import de.ii.ldproxy.ogcapi.routes.domain.RouteDefinitionInfo;

import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public final String computeRouteButton;
    public Map<String, String> bbox;
    public final MapClient mapClient;

    private final RouteDefinitionInfo templateInfo;

    public RoutesView(OgcApiDataV2 apiData, RouteDefinitionInfo templateInfo, final List<NavigationDTO> breadCrumbs,
                      String urlPrefix, HtmlConfiguration htmlConfig, boolean noIndex, I18n i18n, Optional<Locale> language) {
        super("routes.mustache", Charsets.UTF_8, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix,
              ImmutableList.of(),
              i18n.get("routesTitle", language),
              i18n.get("routesDescription", language));
        this.templateInfo = templateInfo;
        routeNameTitle = i18n.get("routeNameTitle", language);
        preferenceTitle = i18n.get("preferenceTitle", language);
        additionalFlagsTitle = i18n.get("additionalFlagsTitle", language);
        startLocationTitle = i18n.get("startLocationTitle", language);
        endLocationTitle = i18n.get("endLocationTitle", language);
        crsTitle = i18n.get("crsTitle", language);
        computeRouteButton = i18n.get("computeRouteButton", language);
        xTitle = i18n.get("xTitle", language);
        yTitle = i18n.get("yTitle", language);

        this.bbox = apiData.getSpatialExtent()
            .map(boundingBox -> ImmutableMap.of(
                "minLng", Double.toString(boundingBox.getXmin()),
                "minLat", Double.toString(boundingBox.getYmin()),
                "maxLng", Double.toString(boundingBox.getXmax()),
                "maxLat", Double.toString(boundingBox.getYmax())))
            .orElse(null);

        this.mapClient = new ImmutableMapClient.Builder()
            .backgroundUrl(Optional.ofNullable(htmlConfig.getLeafletUrl())
                               .or(() -> Optional.ofNullable(htmlConfig.getMapBackgroundUrl())))
            .attribution(Optional.ofNullable(htmlConfig.getLeafletAttribution())
                             .or(() -> Optional.ofNullable(htmlConfig.getMapAttribution())))
            .bounds(Optional.ofNullable(bbox))
            .data(new ImmutableSource.Builder()
                      .type(MapClient.Source.TYPE.geojson)
                      .url("data:application/geo+json,{type:\"FeatureCollection\",features:[]}")
                      .build())
            .popup(MapClient.Popup.CLICK_PROPERTIES)
            .styleUrl(Optional.ofNullable(null)) // TODO
            .removeZoomLevelConstraints(false) // TODO
            .build();
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

    public Set<Map.Entry<String,String>> getCrs() {
        return templateInfo
            .getCrs()
            .entrySet();
    }

    public Optional<String> getAttribution() {
        return apiData.getMetadata().flatMap(Metadata::getAttribution);
    }
}
