/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.data;

import de.ii.ldproxy.ogcapi.observation_processing.api.TemporalInterval;

import java.util.Vector;

public class DataArrayXy {
    public float[][][] array;
    private final double minLon;
    private final double minLat;
    private final double maxLon;
    private final double maxLat;
    private final int width;
    private final int height;
    private final double diffLon;
    private final double diffLat;
    private final Vector<String> vars;
    private final TemporalInterval interval;

    public DataArrayXy(int width, int height, Vector<String> vars,
                       double minLon, double minLat,
                       double maxLon, double maxLat,
                       TemporalInterval interval) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        this.width = width;
        this.height = height;
        this.diffLon = width>0 ? (maxLon-minLon)/width : 0.0f;
        this.diffLat = height>0 ? (maxLat-minLat)/height : 0.0f;
        this.vars = vars;
        this.interval = interval;
        this.array = new float[height][width][vars.size()];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Vector<String> getVars() {
        return vars;
    }

    public TemporalInterval getInterval() { return interval; }

    public double lon(int i) { return minLon + diffLon*i; }

    public double lat(int i) { return maxLat - diffLat*i; }
}
