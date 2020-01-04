/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.styles.representation;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataset;
import de.ii.ldproxy.ogcapi.domain.OgcApiDatasetData;
import de.ii.xtraplatform.rest.views.GenericView;

import java.util.Map;

public class StyleView extends GenericView {
    public String styleUrl;
    public String apiId;
    public String styleId;
    public Map<String, String> bbox;
    private OgcApiDatasetData apiData;

    public StyleView(String styleUrl, OgcApiDataset api, String styleId) {
        super("style", null);
        this.styleUrl = styleUrl;
        this.apiId = apiId;
        this.styleId = styleId;
        this.apiData = api.getData();

        double[] spatialExtent = apiData.getSpatialExtent();
        this.bbox = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent[0]),
                "minLat", Double.toString(spatialExtent[1]),
                "maxLng", Double.toString(spatialExtent[2]),
                "maxLat", Double.toString(spatialExtent[3]));

    }
}
