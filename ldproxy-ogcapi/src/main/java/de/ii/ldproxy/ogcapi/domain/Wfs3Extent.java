/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * @author zahnen
 */
public class Wfs3Extent {
    private Wfs3ExtentSpatial spatial;
    private Wfs3ExtentTemporal temporal;

    public Wfs3Extent() {
        this.spatial = new Wfs3ExtentSpatial(-180, -90, 180, 90);
        this.temporal = new Wfs3ExtentTemporal(null, null);
    }

    public Wfs3Extent(String xmin, String ymin, String xmax, String ymax) {
        this.spatial = new Wfs3ExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = new Wfs3ExtentTemporal(null, null);
    }

    public Wfs3Extent(double xmin, double ymin, double xmax, double ymax) {
        this.spatial = new Wfs3ExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = new Wfs3ExtentTemporal(null, null);
    }

    public Wfs3Extent(String begin, String end) {
        this.spatial = new Wfs3ExtentSpatial(-180, -90, 180, 90);
        this.temporal = new Wfs3ExtentTemporal(begin, end);
    }

    public Wfs3Extent(long begin, long end) {
        this.spatial = new Wfs3ExtentSpatial(-180, -90, 180, 90);
        this.temporal = new Wfs3ExtentTemporal(begin, end);
    }

    public Wfs3Extent(long begin, long end, double xmin, double ymin, double xmax, double ymax) {
        this.spatial = new Wfs3ExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = new Wfs3ExtentTemporal(begin, end);
    }

    public Wfs3Extent(String begin, String end, String xmin, String ymin, String xmax, String ymax) {
        this.spatial = new Wfs3ExtentSpatial(xmin, ymin, xmax, ymax);
        this.temporal = new Wfs3ExtentTemporal(begin, end);
    }

    public Wfs3ExtentSpatial getSpatial() {
        return spatial;
    }

    public void setSpatial(Wfs3ExtentSpatial spatial) {
        this.spatial = spatial;
    }

    public Wfs3ExtentTemporal getTemporal() {
        return temporal;
    }

    public void setTemporal(Wfs3ExtentTemporal temporal) {
        this.temporal = temporal;
    }
}
