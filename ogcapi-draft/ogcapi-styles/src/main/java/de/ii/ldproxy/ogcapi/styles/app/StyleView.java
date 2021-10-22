/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.styles.app;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ldproxy.ogcapi.html.domain.MapClient;
import de.ii.ldproxy.ogcapi.html.domain.MapClient.Popup;
import de.ii.xtraplatform.services.domain.GenericView;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class StyleView extends GenericView {
    public final String title;
    public final String styleUrl;
    public final boolean popup;
    public final boolean layerSwitcher;
    public final String layerIds;
    public final MapClient mapClient;

    public StyleView(String styleUrl, OgcApiDataV2 apiData, String styleId, boolean popup, boolean layerControl, Map<String, Collection<String>> layerMap) {
        super("/templates/style", null);
        this.title = "Style " + styleId;
        this.styleUrl = styleUrl;
        this.layerIds = "{" +
                layerMap.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> "\""+entry.getKey()+"\": [ \"" + String.join("\", \"", entry.getValue()) + "\" ]")
                        .collect(Collectors.joining(", ")) +
                "}";

        this.mapClient = new ImmutableMapClient.Builder()
            .styleUrl(styleUrl)
            .popup(Popup.CLICK_PROPERTIES)
            .savePosition(true)
            .build();

        this.popup = false;
        this.layerSwitcher = false;
    }
}
