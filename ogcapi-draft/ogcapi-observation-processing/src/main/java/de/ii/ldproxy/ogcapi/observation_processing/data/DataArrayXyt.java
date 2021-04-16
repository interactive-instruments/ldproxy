/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.observation_processing.data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Vector;

public class DataArrayXyt {
    public float[][][][] array;
    private final double minLon;
    private final double minLat;
    private final double minTime;
    private final double maxLon;
    private final double maxLat;
    private final double maxTime;
    private final int width;
    private final int height;
    private final int steps;
    private final double diffLon;
    private final double diffLat;
    private final double diffTime;
    private final Vector<String> vars;

    DataArrayXyt(int width, int height, int steps, Vector<String> vars,
                 double minLon, double minLat, double minTime,
                 double maxLon, double maxLat, double maxTime) {
        this.minLon = minLon;
        this.minLat = minLat;
        this.minTime = minTime;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        this.maxTime = maxTime;
        this.width = width;
        this.height = height;
        this.steps = steps;
        this.diffLon = width>0 ? (maxLon-minLon)/width : 0.0f;
        this.diffLat = height>0 ? (maxLat-minLat)/height : 0.0f;
        this.diffTime = steps>0 ? (maxTime-minTime)/steps : 0.0f;
        this.vars = vars;
        this.array = new float[steps][height][width][vars.size()];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSteps() {
        return steps;
    }

    public Vector<String> getVars() {
        return vars;
    }

    public double lon(int i) { return minLon + diffLon*i; }

    public double lat(int i) { return maxLat - diffLat*i; }

    public double time(int i) {
        return minTime + diffTime*i;
    }

    public LocalDate date(int i) {
        return Observations.date(time(i));
    }

    public OffsetDateTime datetime(int i) {
        return Observations.datetime(time(i));
    }
}
