/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

public class OgcApiExtent {
    private OgcApiExtentSpatial spatial;
    private OgcApiExtentTemporal temporal;

    public OgcApiExtent() {
        this.spatial = null;
        this.temporal = null;
    }

    public OgcApiExtent(String xmin, String ymin, String xmax, String ymax) {
        this.spatial = new OgcApiExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = null;
    }

    public OgcApiExtent(double xmin, double ymin, double xmax, double ymax) {
        this.spatial = new OgcApiExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = null;
    }

    public OgcApiExtent(String begin, String end) {
        this.spatial = null;
        this.temporal = new OgcApiExtentTemporal(begin, end);
    }

    public OgcApiExtent(long begin, long end) {
        this.spatial = null;
        this.temporal = new OgcApiExtentTemporal(begin, end);
    }

    public OgcApiExtent(long begin, long end, double xmin, double ymin, double xmax, double ymax) {
        this.spatial = new OgcApiExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = new OgcApiExtentTemporal(begin, end);
    }

    public OgcApiExtent(String begin, String end, String xmin, String ymin, String xmax, String ymax) {
        this.spatial = new OgcApiExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = new OgcApiExtentTemporal(begin, end);
    }
    public OgcApiExtentSpatial getSpatial() {
        return spatial;
    }

    public void setSpatial(OgcApiExtentSpatial spatial) {
        this.spatial = spatial;
    }

    public OgcApiExtentTemporal getTemporal() {
        return temporal;
    }

    public void setTemporal(OgcApiExtentTemporal temporal) {
        this.temporal = temporal;
    }
}
