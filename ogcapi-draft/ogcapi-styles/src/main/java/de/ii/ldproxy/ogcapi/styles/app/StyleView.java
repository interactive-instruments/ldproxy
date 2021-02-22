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
    private final OgcApiDataV2 apiData;

    public StyleView(String styleUrl, OgcApi api, String styleId, boolean popup, boolean layerControl, Map<String, Collection<String>> layerMap) {
        super("/templates/style", null);
        this.styleUrl = styleUrl;
        this.apiId = api.getId();
        this.styleId = styleId;
        this.apiData = api.getData();
        this.popup = popup;
        this.layerSwitcher = layerControl;
        this.layerIds = "{" +
                String.join(", ", layerMap.entrySet()
                                          .stream()
                                          .sorted(Comparator.comparing(Map.Entry::getKey))
                                          .map(entry -> "\""+entry.getKey()+"\": [ \"" + String.join("\", \"", entry.getValue()) + "\" ]")
                                          .collect(Collectors.toList())) +
                "}";

        Optional<BoundingBox> spatialExtent = apiData.getSpatialExtent();
        this.bbox = spatialExtent.isEmpty() ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent.get().getXmin()),
                "minLat", Double.toString(spatialExtent.get().getYmin()),
                "maxLng", Double.toString(spatialExtent.get().getXmax()),
                "maxLat", Double.toString(spatialExtent.get().getYmax()));
    }
}
