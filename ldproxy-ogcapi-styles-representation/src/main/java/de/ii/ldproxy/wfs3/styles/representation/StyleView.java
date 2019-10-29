/**
 * Copyright 2019 interactive instruments GmbH
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

        double[] spatialExtent = apiData.getFeatureTypes()
                .values()
                .stream()
                .map(featureTypeConfiguration -> featureTypeConfiguration.getExtent()
                        .getSpatial()
                        .getCoords())
                .reduce((doubles, doubles2) -> new double[]{
                        Math.min(doubles[0], doubles2[0]),
                        Math.min(doubles[1], doubles2[1]),
                        Math.max(doubles[2], doubles2[2]),
                        Math.max(doubles[3], doubles2[3])})
                .orElse(null);
        this.bbox = spatialExtent==null ? null : ImmutableMap.of(
                "minLng", Double.toString(spatialExtent[1]),
                "minLat", Double.toString(spatialExtent[0]),
                "maxLng", Double.toString(spatialExtent[3]),
                "maxLat", Double.toString(spatialExtent[2])); // TODO is axis order mixed up in script.mustache?

    }
}
