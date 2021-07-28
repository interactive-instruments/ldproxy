/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.OgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.services.domain.GenericView;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StyleView extends GenericView {
    public final String styleUrl;
    public final String apiId;
    public final String styleId;
    public final boolean popup;
    public final boolean layerSwitcher;
    public final String layerIds;
    public final Map<String, String> bbox;

    public StyleView(String styleUrl, OgcApiDataV2 apiData, BoundingBox spatialExtent, String styleId, boolean popup, boolean layerControl, Map<String, Collection<String>> layerMap) {
        super("/templates/style", null);
        this.apiId = apiData.getId();
        this.styleId = styleId;
        this.styleUrl = styleUrl;
        this.popup = popup;
        this.layerSwitcher = layerControl;
        this.layerIds = "{" +
                layerMap.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> "\""+entry.getKey()+"\": [ \"" + String.join("\", \"", entry.getValue()) + "\" ]")
                        .collect(Collectors.joining(", ")) +
                "}";
        this.bbox = Objects.isNull(spatialExtent) ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent.getXmin()),
                "minLat", Double.toString(spatialExtent.getYmin()),
                "maxLng", Double.toString(spatialExtent.getXmax()),
                "maxLat", Double.toString(spatialExtent.getYmax()));
    }
}
