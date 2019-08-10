/**
 * Copyright 2019 interactive instruments GmbH
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
 * @author portele
 */
public class Wfs3ExtentTemporal {
    private String[][] interval;
    private String trs;

    public Wfs3ExtentTemporal() {

    }

    public Wfs3ExtentTemporal(long begin, long end) {
        this.interval = new String[][]{{
                                Instant.ofEpochMilli(begin).truncatedTo(ChronoUnit.SECONDS).toString(),
                                Instant.ofEpochMilli(end).truncatedTo(ChronoUnit.SECONDS).toString()
                            }};
        this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    }

    public Wfs3ExtentTemporal(String begin, String end) {
        this.interval = new String[][]{{begin, end}};
        this.trs = "http://www.opengis.net/def/uom/ISO-8601/0/Gregorian";
    }

    public String[][] getInterval() {
        return interval;
    }

    public void setInterval(String[][] interval) {
        this.interval = interval;
    }

    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }
}
