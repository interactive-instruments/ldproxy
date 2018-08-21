/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.wfs3.api;

import com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * @author zahnen
 */
public class Wfs3Extent {
    private double[] spatial;
    private String[] temporal;

    public Wfs3Extent() {

    }

    public Wfs3Extent(String xmin, String ymin, String xmax, String ymax) {
        try {
            this.spatial = ImmutableList.of(xmin, ymin, xmax, ymax)
                                        .stream()
                                        .mapToDouble(Double::parseDouble)
                                        .toArray();
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    public Wfs3Extent(long begin, long end) {
        this.temporal = new String[]{Instant.ofEpochMilli(begin)
                                            .truncatedTo(ChronoUnit.SECONDS).toString(), Instant.ofEpochMilli(end)
                                                                                                .truncatedTo(ChronoUnit.SECONDS).toString()};
    }

    public Wfs3Extent(long begin, long end, double xmin, double ymin, double xmax, double ymax) {
        this.temporal = new String[]{Instant.ofEpochMilli(begin)
                                            .truncatedTo(ChronoUnit.SECONDS).toString(), Instant.ofEpochMilli(end)
                                                                                                .truncatedTo(ChronoUnit.SECONDS).toString()};
        this.spatial = new double[]{xmin, ymin, xmax, ymax};
    }

    public Wfs3Extent(double xmin, double ymin, double xmax, double ymax) {
        this.spatial = new double[]{xmin, ymin, xmax, ymax};
    }

    public double[] getSpatial() {
        return spatial;
    }

    public void setSpatial(double[] spatial) {
        this.spatial = spatial;
    }

    public String[] getTemporal() {
        return temporal;
    }

    public void setTemporal(String[] temporal) {
        this.temporal = temporal;
    }
}
