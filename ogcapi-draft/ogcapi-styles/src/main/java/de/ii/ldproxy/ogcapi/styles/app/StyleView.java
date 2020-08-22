/**
 * Copyright 2020 interactive instruments GmbH
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

import java.util.Map;

public class StyleView extends GenericView {
    public String styleUrl;
    public String apiId;
    public String styleId;
    public boolean popup = true;
    public Map<String, String> bbox;
    private OgcApiDataV2 apiData;

    public StyleView(String styleUrl, OgcApi api, String styleId) {
        super("/templates/style", null);
        this.styleUrl = styleUrl;
        this.apiId = apiId;
        this.styleId = styleId;
        this.apiData = api.getData();

        BoundingBox spatialExtent = apiData.getSpatialExtent();
        this.bbox = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent.getXmin()),
                "minLat", Double.toString(spatialExtent.getYmin()),
                "maxLng", Double.toString(spatialExtent.getXmax()),
                "maxLat", Double.toString(spatialExtent.getYmax()));
    }
}
