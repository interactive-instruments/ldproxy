/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;

public class OgcApiExtentSpatial {
    private double[][] bbox;
    private String crs;

    public OgcApiExtentSpatial() {

    }

    public OgcApiExtentSpatial(String xmin, String ymin, String xmax, String ymax) {
        try {
            double[] bbox1 = ImmutableList.of(xmin, ymin, xmax, ymax)
                    .stream()
                    .mapToDouble(Double::parseDouble)
                    .toArray();
            this.bbox = new double[][]{ bbox1 };
            this.crs = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    public OgcApiExtentSpatial(double xmin, double ymin, double xmax, double ymax) {
        this.bbox = new double[][]{{xmin, ymin, xmax, ymax}};
        this.crs = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
    }

    public double[][] getBbox() {
        return bbox;
    }

    public void setBbox(double[][] bbox) {
        this.bbox = bbox;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }
}
